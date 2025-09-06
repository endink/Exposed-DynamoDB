package com.labijie.infra.orm.dynamodb.builder

import com.labijie.infra.orm.dynamodb.*
import com.labijie.infra.orm.dynamodb.DynamodbUtils.prettyString
import com.labijie.infra.orm.dynamodb.exception.DynamoException
import com.labijie.infra.orm.dynamodb.execution.LastEvaluatedKeyCodec
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.QueryRequest

/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


open class DynamoQueryBuilder<PK, SK>(table: DynamoTable<PK, SK>) : ProjectionBaseBuilder(table),
    IDynamoFilterBuilder<PK, SK> {


    private var filterExpr: DynamoExpression<Boolean>? = null
    protected var keyExpr: DynamoExpression<Boolean>? = null
    private var limit: Int? = null
    private var indexName: String? = null
    private var indexForward: Boolean = true
    private var lastKey: Map<String, AttributeValue>? = null

    fun lastKeys(keys: Map<String, AttributeValue>): DynamoQueryBuilder<PK, SK> {
        lastKey = keys
        return this
    }

    fun lastKeys(encodedKeys: String): DynamoQueryBuilder<PK, SK> {
        lastKey = LastEvaluatedKeyCodec.decode(encodedKeys)
        return this
    }

    fun limit(limit: Int): DynamoQueryBuilder<PK, SK> {
        this.limit = limit
        return this
    }

    fun orderByDesc() {
        indexForward = false
        return
    }

    fun orderByAsc() {
        indexForward = true
        return
    }

    fun keys(index: String? = null, block: IDynamoRangeKeyQueryBuilder<PK, SK>.() -> DynamoExpression<Boolean>): DynamoQueryBuilder<PK, SK> {
        indexName = index
        keyExpr = block.invoke(this)
        return this
    }


    fun filter(block: DynamoQueryBuilder<PK, SK>.() -> DynamoExpression<Boolean>): DynamoQueryBuilder<PK, SK> {
        filterExpr = block.invoke(this)
        return this
    }


    fun request(customizer: (QueryRequest.Builder.()-> Unit)? = null) : QueryRequest {
        val context = RenderContext(true)

        val keyExpression = keyExpr?.render(context) ?: throw DynamoException("No key clause defined.")
        val filterExpression = filterExpr?.render(context)

        val projectExpression = renderProjection(context)

        val request = QueryRequest.builder()
            .ifNullOrBlankInput(projectExpression) { projectionExpression(projectExpression) }
            .ifNotNull(limit) { limit(it) }
            .ifNotNull(lastKey) { exclusiveStartKey(it) }
            .ifNullOrBlankInput(indexName) { indexName(it) }
            .keyConditionExpression(keyExpression)
            .ifNullOrBlankInput(filterExpression, { filterExpression(it) })
            .ifNotNullOrEmpty(context.values) { expressionAttributeValues(it) }
            .ifNotNullOrEmpty(context.attributeNames) { expressionAttributeNames(it) }
            .scanIndexForward(indexForward)
             .tableName(tableName)
            .ifNotNull(customizer) {
                it.invoke(this)
                this
            }
            .build()


        println(request.prettyString())

        return request
    }

    fun toList(client: DynamoDbClient): List<Map<String, Any?>> {
        val req = request()

        val response = client.query(req)

        // 把每条 item 转成 Map<String, Any?>
        return response.items().map { item ->
            item.map {
               it.key to AttributeValueConverter.fromDb(it.value)
            }.toMap()
        }
    }
}