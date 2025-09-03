/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 *
 * File Create: 2025-09-03
 */

package com.labijie.infra.orm.dynamodb.builder

import com.labijie.infra.orm.dynamodb.*
import com.labijie.infra.orm.dynamodb.DynamodbUtils.prettyString
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.Put
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.ReturnValue

class DynamoPutBuilder(val table: DynamoTable) {
    internal val values = mutableMapOf<DynamoColumn<*>, Any?>()
    private var conditionExpression: DynamoExpression<Boolean>? = null


    private fun buildSetter(): Map<String, AttributeValue> {
        val result = LinkedHashMap<String, AttributeValue>(values.size)

        values.forEach {
            columnValue->
            val attrValue: AttributeValue = if (columnValue.value == null) {
                AttributeValue.builder().nul(true).build()
            } else {
                columnValue.key.toDbValue(columnValue.value)
            }

            result[columnValue.key.name] = attrValue
        }

        return result
    }

    internal fun build(): Put {
        val item = buildSetter()

        val ctx = RenderContext(true)
        val expr = conditionExpression?.render(ctx)


        val put = Put.builder().tableName(this.table.tableName)
            .item(item)
            .ifNotNullOrEmpty(ctx.attributeNames) { expressionAttributeNames(it) }
            .ifNotNullOrEmpty(ctx.values) { expressionAttributeValues(it) }
            .ifNullOrBlankInput(expr) { conditionExpression(it) }
            .build()

        println(put.prettyString())

        return put
    }

    fun condition(where: IDynamoFilterBuilder.() -> DynamoExpression<Boolean>): DynamoPutBuilder {
        conditionExpression = where.invoke(IDynamoFilterBuilder.NULL)
        return this
    }

    fun request(returnValue: ReturnValue = ReturnValue.NONE, customizer: (PutItemRequest.Builder.()-> PutItemRequest.Builder)? = null): PutItemRequest {
        val put = build()

        return put.request(returnValue, customizer)
    }

}