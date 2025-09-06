/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb


interface IDynamoRangeKeyQueryBuilder<PK, SK> : IDynamoExactKeyQueryBuilder<PK, SK> {

    //number
    infix fun <R : IColumnBounded<R, TValue>, TValue> R.greater(value: TValue) = BinaryExpr(this.getColumn().colExpr(), value.valueExpr(this.getColumn()), BinaryOp.Gt)
    infix fun <R : IColumnBounded<R, TValue>, TValue> R.greaterEq(value: TValue) = BinaryExpr(this.getColumn().colExpr(), value.valueExpr(this.getColumn()), BinaryOp.Ge)
    infix fun <R : IColumnBounded<R, TValue>, TValue> R.less(value: TValue) = BinaryExpr(this.getColumn().colExpr(), value.valueExpr(this.getColumn()), BinaryOp.Lt)
    infix fun <R : IColumnBounded<R, TValue>, TValue> R.lessEq(value: TValue) = BinaryExpr(this.getColumn().colExpr(), value.valueExpr(this.getColumn()), BinaryOp.Le)

    fun <R : IColumnBounded<R, T>, T> R.between(min: T, max: T) = BetweenExpr(this.getColumn(), min, max)

    infix fun DynamoColumn<String>.beginsWith(prefix: String) = FunctionExpr<Boolean>("begins_with", listOf(this.colExpr(), prefix.valueExpr(this)))

    infix fun DynamoColumn<ByteArray>.beginsWith(prefix: ByteArray) = FunctionExpr<Boolean>("begins_with", listOf(this.colExpr(), prefix.valueExpr(this)))
}