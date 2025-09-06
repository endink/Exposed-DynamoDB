/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb.schema

import com.labijie.infra.orm.dynamodb.DynamoColumn
import com.labijie.infra.orm.dynamodb.DynamoDataType
import com.labijie.infra.orm.dynamodb.DynamoTable
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest
import software.amazon.awssdk.services.dynamodb.model.BillingMode
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest
import software.amazon.awssdk.services.dynamodb.model.DeleteRequest
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement
import software.amazon.awssdk.services.dynamodb.model.KeyType
import software.amazon.awssdk.services.dynamodb.model.LocalSecondaryIndex
import software.amazon.awssdk.services.dynamodb.model.Projection
import software.amazon.awssdk.services.dynamodb.model.ProjectionType
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType
import software.amazon.awssdk.services.dynamodb.model.ScanRequest
import software.amazon.awssdk.services.dynamodb.model.WriteRequest
import kotlin.system.measureTimeMillis


object DynamodbSchemaUtils {
    /**
     * Create a DynamoDB table including primary key and Local Secondary Indexes (LSI)
     */
    fun <PK, SK> createTableIfNotExist(client: DynamoDbClient, table: DynamoTable<PK, SK>): Boolean {

        // 检查表是否已经存在
        val resp = client.listTables()
        if(resp.tableNames().contains(table.tableName))
        {
            return false
        }

        val attributeDefinitions = mutableSetOf<AttributeDefinition>()

        // Add primary key attributes
        table.primaryKey.partitionKey.getColumn().toAttributeDefinition()?.let { attributeDefinitions.add(it) }
        table.primaryKey.sortKey?.getColumn()?.toAttributeDefinition()?.let { attributeDefinitions.add(it) }

        // Add LSI sort key attributes
        table.indexes.values.forEach { index ->
            index.column.toAttributeDefinition()?.let { attributeDefinitions.add(it) }
        }

        // Build primary table KeySchema (HASH + optional RANGE)
        val keySchema = mutableListOf<KeySchemaElement>()
        keySchema.add(
            KeySchemaElement.builder()
                .attributeName(table.primaryKey.partitionKey.getColumn().name)
                .keyType(KeyType.HASH)
                .build()
        )
        table.primaryKey.sortKey?.let {
            keySchema.add(
                KeySchemaElement.builder()
                    .attributeName(it.getColumn().name)
                    .keyType(KeyType.RANGE)
                    .build()
            )
        }

        val keyAttributes = table.indexes.values.map { index -> index.column.name }.union(attributeDefinitions.map { it.attributeName() }).toSet()

        // Build Local Secondary Indexes (each shares HASH with table, own RANGE)
        val localSecondaryIndexes = table.indexes.map { (indexName, index) ->
            LocalSecondaryIndex.builder()
                .indexName(indexName)
                .keySchema(
                    KeySchemaElement.builder()
                        .attributeName(table.primaryKey.partitionKey.getColumn().name)
                        .keyType(KeyType.HASH)
                        .build(),
                    KeySchemaElement.builder()
                        .attributeName(index.column.name)
                        .keyType(KeyType.RANGE)
                        .build()
                )
                .projection(
                    Projection.builder()
                        .projectionType(index.projection) // Can be KEYS_ONLY or INCLUDE
                        .apply {
                            if (index.projection == ProjectionType.INCLUDE) {
                                nonKeyAttributes(index.projectedColumns.filter { !keyAttributes.contains(it) })
                            }
                        }
                        .build()
                )
                .build()
        }

        // Create table request
        val request = CreateTableRequest.builder()
            .tableName(table.tableName)
            .attributeDefinitions(attributeDefinitions)
            .keySchema(keySchema)
            .billingMode(BillingMode.PAY_PER_REQUEST)
            .localSecondaryIndexes(localSecondaryIndexes)
            .build()

        val response = client.createTable(request)
        println("Table created: ${response.tableDescription().tableName()}")

        return true
    }


    /**
     * Convert DynamoColumn to AttributeDefinition (only S/N/B types)
     */
    private fun DynamoColumn<*>.toAttributeDefinition(): AttributeDefinition? {
        return when (this.dynamoDbType()) {
            DynamoDataType.STRING, DynamoDataType.STRING_SET -> AttributeDefinition.builder()
                .attributeName(name)
                .attributeType(ScalarAttributeType.S)
                .build()

            DynamoDataType.NUMBER, DynamoDataType.NUMBER_SET -> AttributeDefinition.builder()
                .attributeName(name)
                .attributeType(ScalarAttributeType.N)
                .build()

            DynamoDataType.BINARY, DynamoDataType.BINARY_SET -> AttributeDefinition.builder()
                .attributeName(name)
                .attributeType(ScalarAttributeType.B)
                .build()

            else -> null // BOOLEAN, NULL, LIST, MAP cannot be used in AttributeDefinitions
        }
    }

    private val logger by lazy {
        LoggerFactory.getLogger(DynamodbSchemaUtils::class.java)
    }

    /**
     * Clear all items in the given DynamoDB table.
     * This uses Scan to retrieve items and BatchWriteItem to delete them in batches.
     */
    fun <PK, SK> clearTable(client: DynamoDbClient, table: DynamoTable<PK, SK>) {
        val tableName = table.tableName
        logger.info("Clearing table: $tableName")

        val pkName = table.primaryKey.partitionKey.getColumn().name
        val skName = table.primaryKey.sortKey?.getColumn()?.name

        var totalDeleted = 0
        var batchCount = 0

        val elapsed = measureTimeMillis {
            var lastEvaluatedKey: Map<String, AttributeValue>? = null

            do {
                val scanRequest = ScanRequest.builder()
                    .tableName(tableName)
                    .exclusiveStartKey(lastEvaluatedKey)
                    .projectionExpression(
                        listOfNotNull(pkName, skName).joinToString(",")
                    )
                    .build()

                val scanResponse = client.scan(scanRequest)
                val items = scanResponse.items()

                if (items.isNotEmpty()) {
                    val writeRequests = items.map { item ->
                        val keyMap = mutableMapOf<String, AttributeValue>()
                        keyMap[pkName] = item[pkName]!!
                        skName?.let { sk ->
                            item[sk]?.let { keyMap[sk] = it }
                        }
                        WriteRequest.builder()
                            .deleteRequest(DeleteRequest.builder().key(keyMap).build())
                            .build()
                    }

                    // split into 25 item chunks
                    writeRequests.chunked(25).forEach { batch ->
                        batchCount++
                        val batchRequest = BatchWriteItemRequest.builder()
                            .requestItems(mapOf(tableName to batch))
                            .build()
                        client.batchWriteItem(batchRequest)

                        totalDeleted += batch.size
                        logger.info(
                            "Batch #$batchCount deleted ${batch.size} items: {}",
                            batch.joinToString { wr ->
                                wr.deleteRequest().key().entries.joinToString { (k, v) -> "$k=${v.s() ?: v.n() ?: v.b()}" }
                            }
                        )
                    }
                }

                lastEvaluatedKey = scanResponse.lastEvaluatedKey()
            } while (!lastEvaluatedKey.isNullOrEmpty())
        }

        logger.info("Table cleared: $tableName, totalDeleted=$totalDeleted, batches=$batchCount, elapsed=${elapsed}ms")
    }
}