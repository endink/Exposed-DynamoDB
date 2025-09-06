package com.labijie.infra.orm.dynamodb.builder

import com.labijie.infra.orm.dynamodb.DynamoTable
import com.labijie.infra.orm.dynamodb.DynamoUpdateExpression
import com.labijie.infra.orm.dynamodb.IDynamodbUpdateBuilder
import com.labijie.infra.orm.dynamodb.RenderContext
import com.labijie.infra.orm.dynamodb.UpdateRequestCustomizer
import com.labijie.infra.orm.dynamodb.ifNotNull
import com.labijie.infra.orm.dynamodb.ifNotNullOrEmpty
import com.labijie.infra.orm.dynamodb.ifNullOrBlankInput
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.dynamodb.model.ReturnValue
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest

/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */

class DynamoUpdateBuilder(internal val table: DynamoTable) {

    companion object {
        private val logger by lazy {
            LoggerFactory.getLogger(DynamoUpdateBuilder::class.java)
        }
    }

    val tableName: String = table.tableName

    internal val segments by lazy {
        DynamoSegmentsBuilder()
    }

    internal val condition by lazy {
        DynamoConditionBuilder()
    }

    fun request(returnValue: ReturnValue = ReturnValue.NONE, customizer: UpdateRequestCustomizer? = null): UpdateItemRequest {

        val result = condition.buildCondition()

        val ctx = RenderContext(true)
        val updateExpression = this.segments.render(ctx)
        val conditionExpression = result.conditionExpression?.render(ctx)

        // Build the UpdateItemRequest
        val update = UpdateItemRequest.builder()
            .tableName(tableName)
            .key(result.keys)
            .updateExpression(updateExpression)
            .ifNullOrBlankInput(conditionExpression) { conditionExpression(it) }
            .ifNotNullOrEmpty(ctx.values) { expressionAttributeValues(it) }
            .ifNotNullOrEmpty(ctx.attributeNames) { expressionAttributeNames(it) }
            .returnValues(returnValue)
            .ifNotNull(customizer) {
                it.invoke(this)
                this
            }
            .build()

        return update
    }

    inner class DynamoSegmentsBuilder() : IDynamodbUpdateBuilder {

        internal val expressions = mutableListOf<DynamoUpdateExpression<*>>()

        internal fun addExpression(value: DynamoUpdateExpression<*>) {
            this.expressions.add(value)
        }

        internal fun render(context: RenderContext): String {

            val distinct = expressions.groupBy { it.targetName }.map { (target, values) ->
                val last = values.last()
                if (values.size > 1) {
                    for (item in values) {
                        if (item != last) {
                            logger.warn(
                                "Duplex update attribute '${target} (${item.render(RenderContext.Companion.DUMMY)})', merged as '${
                                    last.render(
                                        RenderContext.Companion.DUMMY
                                    )
                                }'"
                            )
                        }
                    }
                }
                last
            }

            val grouped = distinct.groupBy { it.Op }

            val merged = grouped.map { (op, expr) ->

                val list = expr.joinToString(", ") {
                    it.renderWithoutOp(context)
                }
                "$op $list"
            }

            val result = merged.joinToString(" ")
            return result
        }

    }
}