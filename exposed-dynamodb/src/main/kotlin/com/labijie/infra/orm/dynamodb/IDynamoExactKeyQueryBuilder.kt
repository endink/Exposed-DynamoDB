/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb

import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import kotlin.text.isNullOrBlank


interface IDynamoExactKeyQueryBuilder<PK, SK> {

    companion object {

        private val DEFAULT: Any = object : IDynamoExactKeyQueryBuilder<Any, Any> {}

        internal fun <PK, SK> default(): IDynamoExactKeyQueryBuilder<PK, SK> {
            @Suppress("UNCHECKED_CAST")
            return DEFAULT as IDynamoExactKeyQueryBuilder<PK, SK>
        }

        internal fun extractKeys(expr: DynamoExpression<*>, attributes: MutableMap<String, AttributeValue>) {
            when (expr) {
                is BinaryExpr -> {
                    if (expr.op == BinaryOp.Eq) {
                        val col = (expr.left as? ColumnExpr<*>)?.column
                        val valueExpr = expr.right as? ValueExpr<*>
                        if (col != null && valueExpr != null) {
                            attributes.putIfAbsent(col.name, col.toDbValue(valueExpr.value))
                        }
                    }
                    if(expr.op == BinaryOp.And) {
                        extractKeys(expr.left, attributes)
                        extractKeys(expr.right, attributes)
                    }
                }
            }
        }
    }

    // ----------------- Binary Expressions -----------------
    infix fun <T> DynamoColumn<T>.eq(value: T) = BinaryExpr(this.colExpr(), value.valueExpr(this), BinaryOp.Eq)

    // ----------------- Boolean Logic -----------------
    infix fun DynamoExpression<Boolean>.and(other: DynamoExpression<Boolean>) = BinaryExpr(this, other, BinaryOp.And)

    infix fun DynamoExpression<Boolean>.andNotNull(other: DynamoExpression<Boolean>?) : DynamoExpression<Boolean> {
        return other?.let { this.and(other) }  ?: this
    }


    fun DynamoExpression<Boolean>.andIf(predicate: Boolean, condition: ()-> DynamoExpression<Boolean>): DynamoExpression<Boolean> {
        return if(predicate) {
            val right =condition.invoke()
            BinaryExpr(this, right, BinaryOp.And)
        } else this
    }

    fun <TValue> DynamoExpression<Boolean>.andIfNotNull(value: TValue?, condition: (value: TValue)-> DynamoExpression<Boolean>): DynamoExpression<Boolean> {
        return value?.let {
            val right =condition.invoke(it)
            BinaryExpr(this, right, BinaryOp.And)
        } ?: this
    }


    fun DynamoExpression<Boolean>.andIfNotNullOrBlank(value: String?, condition: (value: String)-> DynamoExpression<Boolean>): DynamoExpression<Boolean> {
        if(value.isNullOrBlank()) {
            return this
        }
        val right =condition.invoke(value)
        return BinaryExpr(this, right, BinaryOp.And)
    }

}