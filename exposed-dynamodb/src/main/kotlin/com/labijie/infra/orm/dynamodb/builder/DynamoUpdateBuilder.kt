package com.labijie.infra.orm.dynamodb.builder

import com.labijie.infra.orm.dynamodb.DynamoTable
import com.labijie.infra.orm.dynamodb.DynamoUpdateExpression
import com.labijie.infra.orm.dynamodb.IDynamodbUpdateBuilder
import com.labijie.infra.orm.dynamodb.RenderContext
import org.slf4j.LoggerFactory

/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


class DynamoUpdateBuilder(internal val table: DynamoTable) : IDynamodbUpdateBuilder {

    companion object {
        private val logger by lazy {
            LoggerFactory.getLogger(DynamoUpdateBuilder::class.java)
        }
    }

    internal val expressions = mutableListOf<DynamoUpdateExpression<*>>()

    internal fun addExpression(value: DynamoUpdateExpression<*>) {
        this.expressions.add(value)
    }

    internal fun render(context: RenderContext): String {

        val distinct = expressions.groupBy { it.targetName }.map { (target, values) ->
            val last = values.last()
            if (values.size > 1) {
                for (item in values) {
                    if(item != last) {
                        logger.warn("Duplex update attribute '${target} (${item.render(RenderContext.Companion.DUMMY)})', merged as '${last.render(
                            RenderContext.Companion.DUMMY)}'")
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