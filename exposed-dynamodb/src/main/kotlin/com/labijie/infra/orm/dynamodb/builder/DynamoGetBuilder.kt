/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb.builder

import com.labijie.infra.orm.dynamodb.*
import com.labijie.infra.orm.dynamodb.exception.DynamodbException
import com.labijie.infra.orm.dynamodb.exception.DynamodbTypeMismatchException
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest


class DynamoGetBuilder(table: DynamoTable) : ProjectionBaseBuilder(table) {

    private val keys: MutableMap<String, AttributeValue> = mutableMapOf()


    init {
        table.columns.forEach {
            it.addProjection()
        }
    }


    fun keys(block: IDynamoExactKeyQueryBuilder.() -> DynamoExpression<Boolean>): DynamoGetBuilder {
        val expr = block.invoke(IDynamoExactKeyQueryBuilder.NULL)
        keys.clear()
        IDynamoExactKeyQueryBuilder.extractKeys(expr, keys)

        if(keys.isEmpty()) throw DynamodbException("No key clause defined.")

        return this
    }

    fun request(customizer: (GetItemRequest.Builder.()-> GetItemRequest.Builder)? = null): GetItemRequest {

        if(keys.isEmpty()) throw DynamodbException("No key clause defined.")

        val ctx = RenderContext(true)
        val projectionExpression = renderProjection(ctx)

        return GetItemRequest.builder()
            .key(keys)
            .tableName(this.tableName)
            .ifNullOrBlankInput(projectionExpression) { projectionExpression(projectionExpression) }
            .ifNotNullOrEmpty(ctx.attributeNames) { expressionAttributeNames(ctx.attributeNames) }
            .ifNotNull(customizer) { it.invoke(this) }
            .build()
    }

}