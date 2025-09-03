package com.labijie.infra.orm.dynamodb.builder

import com.labijie.infra.orm.dynamodb.*
import com.labijie.infra.orm.dynamodb.DynamodbUtils.prettyString
import com.labijie.infra.orm.dynamodb.exception.DynamodbException
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.QueryRequest

/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


open class DynamoQueryBuilder(table: DynamoTable) : ProjectionBaseBuilder(table),
    IDynamoFilterBuilder {


    private var filterExpr: DynamoExpression<Boolean>? = null
    protected var keyExpr: DynamoExpression<Boolean>? = null
    private var limit: Int? = null
    private var indexName: String? = null
    private var indexForward: Boolean = true



    fun orderByDesc() {
        indexForward = false
        return
    }

    fun orderByAsc() {
        indexForward = true
        return
    }

    fun keys(index: String? = null, block: IDynamoRangeKeyQueryBuilder.() -> DynamoExpression<Boolean>): DynamoQueryBuilder {
        indexName = index
        keyExpr = block.invoke(this)
        return this
    }


    fun filter(block: DynamoQueryBuilder.() -> DynamoExpression<Boolean>): DynamoQueryBuilder {
        filterExpr = block.invoke(this)
        return this
    }

    private fun render(expression: DynamoExpression<Boolean>?, renderContext: RenderContext): String {
        val expr = expression
            ?: throw DynamodbException("No where clause defined")

        return expr.render(renderContext)
    }

    fun request(customizer: (QueryRequest.Builder.()-> QueryRequest.Builder)? = null) : QueryRequest {
        val context = RenderContext(true)

        val keyExpression = keyExpr?.render(context) ?: throw DynamodbException("No key clause defined.")
        val filterExpression = filterExpr?.render(context)

        val projectExpression = renderProjection(context)

        val request = QueryRequest.builder()
            .ifNullOrBlankInput(projectExpression) { projectionExpression(projectExpression) }
            .tableName(tableName)
            .ifNotNull(limit) { limit(it) }
            .ifNullOrBlankInput(indexName) { indexName(it) }
            .keyConditionExpression(keyExpression)
            .ifNullOrBlankInput(filterExpression, { filterExpression(it) })
            .ifNotNullOrEmpty(context.values) { expressionAttributeValues(it) }
            .ifNotNullOrEmpty(context.attributeNames) { expressionAttributeNames(it) }
            .scanIndexForward(indexForward)
            .ifNotNull(customizer) { it.invoke(this) }
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