/**
 * This file is part of Exposed-DynamoDB project.
 * Copyright (c) 2025
 * @author Anders Xiao
 *
 * File Create: 2025/9/5
 */

package com.labijie.infra.orm.dynamodb.builder

import com.labijie.infra.orm.dynamodb.*
import com.labijie.infra.orm.dynamodb.exception.DynamoException
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes

class DynamoBatchGetBuilder internal constructor(private val table: DynamoTable) {

    private val getItems: MutableMap<String, ItemGet> = mutableMapOf()
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

    fun request(customizer: ((BatchGetItemRequest.Builder)-> Unit)? = null): BatchGetItemRequest {
        this.customizer = customizer
        val items = getItems.mapValues {
            val context = RenderContext(false)
            it.value.keysItem(context)
        }
        val request = BatchGetItemRequest.builder()
            .requestItems(items)
            .ifNotNull(customizer) {
                it.invoke(this)
                this
            }
            .build()

        return request
    }


    inner class Context internal constructor() {
        fun get(groupName: String, build: ItemGet.() -> Unit): Context {
            val innerBuilder = ItemGet(table)

            build.invoke(innerBuilder)
            getItems[groupName] = innerBuilder
            return this
        }
    }


    inner class ItemGet internal constructor(table: DynamoTable) : ProjectionBaseBuilder(table) {
        val keys: MutableMap<String, AttributeValue> = mutableMapOf()
        private var consistentRead: Boolean = false

        private val projection by lazy {
            projection()
        }

        fun keys(block: IDynamoExactKeyQueryBuilder.() -> DynamoExpression<Boolean>): ItemGet {
            val expr = block.invoke(IDynamoExactKeyQueryBuilder.NULL)
            keys.clear()
            IDynamoExactKeyQueryBuilder.extractKeys(expr, keys)

            if (keys.isEmpty()) throw DynamoException("No key clause defined.")

            return this
        }

        fun projectAll(): ItemGet {
            projection.projectAll()
            return this
        }

        fun projectKeyOnly(): ItemGet {
            projection.projectKeyOnly()
            return this
        }

        fun project(vararg projections: IDynamoProjection): ItemGet {
            projection.project(*projections)
            return this
        }

        fun consistentRead(consistentRead: Boolean): ItemGet {
            this.consistentRead = consistentRead
            return this
        }

        internal fun keysItem(context: RenderContext): KeysAndAttributes {
            if (keys.isEmpty()) throw DynamoException("No key clause defined.")

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