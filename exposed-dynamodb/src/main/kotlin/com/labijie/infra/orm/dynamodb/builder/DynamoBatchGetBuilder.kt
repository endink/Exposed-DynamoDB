/**
 * This file is part of Exposed-DynamoDB project.
 * Copyright (c) 2025
 * @author Anders Xiao
 *
 * File Create: 2025/9/5
 */

package com.labijie.infra.orm.dynamodb.builder

import com.labijie.infra.orm.dynamodb.*
import com.labijie.infra.orm.dynamodb.exception.ExposedDynamoDbException
import com.labijie.infra.orm.dynamodb.exception.DynamoDbExpressionFormatException
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes

class DynamoBatchGetBuilder<TPartitionKey, TSortKey> internal constructor(private val table: DynamoTable<TPartitionKey, TSortKey>) {

    private val tableGet: MutableMap<String, ItemBuilder<*, *>> = mutableMapOf()
    private var customizer: BatchGetRequestCustomizer? = null

    val tableName: String = table.tableName

    internal val context by lazy {
        Context()
    }

    internal fun next(keys: Map<String, KeysAndAttributes>): BatchGetItemRequest {
        val request = BatchGetItemRequest.builder()
            .requestItems(keys)
            .ifNotNull(customizer) {
                it.invoke(this)
                this
            }
            .build()

        return request
    }

    fun request(customizer: ((BatchGetItemRequest.Builder) -> Unit)? = null): BatchGetItemRequest {
        this.customizer = customizer
        val tableGetItems = tableGet.mapValues { item ->
            val context = RenderContext(false)
            item.value.render(context)
        }
        val request = BatchGetItemRequest.builder()
            .requestItems(tableGetItems)
            .ifNotNull(customizer) {
                it.invoke(this)
                this
            }
            .build()

        return request
    }


    inner class Context internal constructor() {

        fun get(itemBuild: ItemBuilder<TPartitionKey, TSortKey>.() -> Unit) {
            val innerBuilder = ItemBuilder(table)
            itemBuild.invoke(innerBuilder)

            tableGet[tableName] = innerBuilder
        }

        fun <PK, SK> get(t: DynamoTable<PK, SK>, itemBuild: ItemBuilder<PK, SK>.() -> Unit) {
            val innerBuilder = ItemBuilder(t)
            itemBuild.invoke(innerBuilder)

            tableGet[tableName] = innerBuilder
        }
    }


    inner class ItemBuilder<PK, SK> internal constructor(table: DynamoTable<PK, SK>) : ProjectionBaseBuilder(table) {
        private val keys: MutableList<MutableMap<String, AttributeValue>> = mutableListOf()
        private var consistentRead: Boolean = false

        private val projection by lazy {
            projection()
        }

        fun keys(partitionKey: PK, vararg sortKey: SK): ItemBuilder<PK, SK> {
            val pk = table.primaryKey.partitionKey.getColumn()
            val sk = table.primaryKey.sortKey
            if (sk != null) {
                if(sortKey.isEmpty()) {
                    throw DynamoDbExpressionFormatException.sortKeyMissed(table.tableName)
                }
                sortKey.forEach {
                    val pkAndSk = mutableMapOf(
                        pk.name to pk.toDbValue(partitionKey),
                        sk.getColumn().name to sk.getColumn().toDbValue(it)
                    )
                    keys.add(pkAndSk)
                }
            } else {
                keys.add(
                    mutableMapOf(
                        pk.name to pk.toDbValue(partitionKey),
                    )
                )
            }
            return this
        }

        fun keys(block: IDynamoExactKeyQueryBuilder<PK, SK>.() -> DynamoExpression<Boolean>): ItemBuilder<PK, SK> {
            val expr = block.invoke(IDynamoExactKeyQueryBuilder.default())
            val attributes = mutableMapOf<String, AttributeValue>()
            IDynamoExactKeyQueryBuilder.extractKeys(expr, attributes)

            if (attributes.isEmpty()) throw ExposedDynamoDbException("No key clause defined.")
            keys.add(attributes)
            return this
        }

        fun projectAll(): ItemBuilder<PK, SK> {
            projection.projectAll()
            return this
        }

        fun projectKeyOnly(): ItemBuilder<PK, SK> {
            projection.projectKeyOnly()
            return this
        }

        fun project(vararg projections: IDynamoProjection): ItemBuilder<PK, SK> {
            projection.project(*projections)
            return this
        }

        fun consistentRead(consistentRead: Boolean): ItemBuilder<PK, SK> {
            this.consistentRead = consistentRead
            return this
        }

        internal fun render(context: RenderContext): KeysAndAttributes {
            if (keys.isEmpty()) throw ExposedDynamoDbException("No key clause defined.")

            val projectionExpression = renderProjection(context)

            return KeysAndAttributes.builder()
                .keys(keys)
                .consistentRead(consistentRead)
                .ifNullOrBlankInput(projectionExpression) { projectionExpression(projectionExpression) }
                .ifNotNullOrEmpty(context.attributeNames) { expressionAttributeNames(context.attributeNames) }
                .build()
        }

        init {
            table.columns.forEach {
                it.addProjection()
            }
        }
    }


}