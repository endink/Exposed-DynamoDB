/**
 * This file is part of Exposed-DynamoDB project.
 * Copyright (c) 2025
 * @author Anders Xiao
 *
 * File Create: 2025/9/5
 */

package com.labijie.infra.orm.dynamodb.builder

import com.labijie.infra.orm.dynamodb.*
import software.amazon.awssdk.services.dynamodb.model.Delete
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest
import software.amazon.awssdk.services.dynamodb.model.ReturnValue

class DynamoDeleteBuilder<PK, SK>(internal val table: DynamoTable<PK, SK>) {
    val tableName: String = table.tableName

    internal val condition by lazy {
        DynamoConditionBuilder(table)
    }

    fun build(): Delete {
        val result = condition.buildCondition()

        val ctx = RenderContext(true)
        val expr = result.conditionExpression?.render(ctx)

        return Delete.builder()
            .tableName(this.tableName)
            .key(result.keys)
            .ifNullOrBlankInput(expr) { conditionExpression(it) }
            .ifNotNullOrEmpty(ctx.values) { expressionAttributeValues(it) }
            .ifNotNullOrEmpty(ctx.attributeNames) { expressionAttributeNames(it) }
            .build()
    }

    fun request(returnValue: ReturnValue = ReturnValue.NONE, customizer: DeleteRequestCustomizer? = null): DeleteItemRequest {
        return build().request(returnValue, customizer)
    }
}