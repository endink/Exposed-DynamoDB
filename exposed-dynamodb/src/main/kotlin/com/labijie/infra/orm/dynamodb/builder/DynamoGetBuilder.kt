/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb.builder

import com.labijie.infra.orm.dynamodb.*
import com.labijie.infra.orm.dynamodb.exception.DynamoException
import com.labijie.infra.orm.dynamodb.exception.DynamodbExpressionFormatException
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest


class DynamoGetBuilder<PK, SK>(table: DynamoTable<PK, SK>) : ProjectionBaseBuilder(table) {

    private val keys: MutableMap<String, AttributeValue> = mutableMapOf()


    init {
        table.columns.forEach {
            it.addProjection()
        }
    }

    fun keys(partitionKey: PK, sortKey: SK?): DynamoGetBuilder<PK, SK> {
        keys.clear()
        val pk = table.primaryKey.partitionKey.getColumn()

        keys.put(pk.name, pk.toDbValue(partitionKey))

        table.primaryKey.sortKey?.let {
            sk->
            if(sortKey != null) {
                val col = sk.getColumn()
                keys.put(col.name, col.toDbValue(sortKey))
            }else {
                throw DynamodbExpressionFormatException.sortKeyMissed(table.tableName)
            }
        }
        return this
    }

    fun keys(block: IDynamoExactKeyQueryBuilder<PK, SK>.() -> DynamoExpression<Boolean>): DynamoGetBuilder<PK, SK> {
        val expr = block.invoke(IDynamoExactKeyQueryBuilder.default())
        keys.clear()
        IDynamoExactKeyQueryBuilder.extractKeys(expr, keys)

        if(keys.isEmpty()) throw DynamoException("No key clause defined.")

        return this
    }


    fun request(customizer: (GetItemRequest.Builder.()-> Unit)? = null): GetItemRequest {

        if(keys.isEmpty()) throw DynamoException("No key clause defined.")

        val ctx = RenderContext(true)
        val projectionExpression = renderProjection(ctx)

        return GetItemRequest.builder()
            .key(keys)
            .tableName(this.tableName)
            .ifNullOrBlankInput(projectionExpression) { projectionExpression(projectionExpression) }
            .ifNotNullOrEmpty(ctx.attributeNames) { expressionAttributeNames(ctx.attributeNames) }
            .ifNotNull(customizer) {
                it.invoke(this)
                this
            }
            .build()
    }

}