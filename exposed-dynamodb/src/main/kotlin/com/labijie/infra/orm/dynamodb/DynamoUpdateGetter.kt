/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb

import com.labijie.infra.orm.dynamodb.builder.DynamoUpdateBuilder


class DynamoUpdateGetter(private val builder: DynamoUpdateBuilder) {

    operator fun <T> set(column: DynamoColumn<T>, value: T?) {
        builder.addExpression(SetExpr(column.colExpr(), ValueExpr(value, column)))
    }

    operator fun <T> set(column: DynamoColumn<T>, value: IComputedValueExpr<T>) {
        builder.addExpression(SetExpr(column.colExpr(), value))
    }

    infix operator fun <T> get(column: DynamoSetColumn<T>) : SetColumnGetExpr<T> {
        return SetColumnGetExpr(column)
    }

    infix operator fun <T> get(column: DynamoColumn<T>) : ColumnGetExpr<T> {
        return ColumnGetExpr(column)
    }
}