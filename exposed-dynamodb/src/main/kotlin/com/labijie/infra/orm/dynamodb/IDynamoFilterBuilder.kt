/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb

import com.labijie.infra.orm.dynamodb.exception.DynamodbExpressionFormatException


interface IDynamoFilterBuilder : IDynamoRangeKeyQueryBuilder {

    companion object {
        internal val NULL = object : IDynamoFilterBuilder {}
    }

    infix fun DynamoColumn<String>.contains(substr: String) =
        FunctionExpr<Boolean>("contains", listOf(this.colExpr(), substr.valueExpr(this)))

    infix fun <TElement> DynamoSetColumn<TElement>.contains(value: TElement) =
        FunctionExpr<Boolean>("contains", listOf(this.colExpr(), value.valueExpr(this)))

    infix fun ListColumn.contains(substr: Any?) =
        FunctionExpr<Boolean>("contains", listOf(this.colExpr(), substr.valueExpr(this)))

    // ----------------- Attribute Exists / Type -----------------
    fun DynamoColumn<*>.exists() = FunctionExpr<Boolean>("attribute_exists", listOf(this.colExpr()))
    fun DynamoColumn<*>.notExists() = FunctionExpr<Boolean>("attribute_not_exists", listOf(this.colExpr()))

    private fun DynamoColumn<*>.attributeType(type: String) =
        FunctionExpr<Boolean>("attribute_type", listOf(this.colExpr(), type.constantExpr()))

    fun DynamoColumn<*>.isString() = attributeType(DynamoDataType.STRING)
    fun DynamoColumn<*>.isNumber() = attributeType(DynamoDataType.NUMBER)
    fun DynamoColumn<*>.isBoolean() = attributeType(DynamoDataType.BOOLEAN)
    fun DynamoColumn<*>.isBinary() = attributeType(DynamoDataType.BINARY)
    fun DynamoColumn<*>.isStringSet() = attributeType(DynamoDataType.STRING_SET)
    fun DynamoColumn<*>.isNumberSet() = attributeType(DynamoDataType.NUMBER_SET)
    fun DynamoColumn<*>.isBinarySet() = attributeType(DynamoDataType.BINARY_SET)
    fun DynamoColumn<*>.isList() = attributeType(DynamoDataType.LIST)
    fun DynamoColumn<*>.isMap() = attributeType(DynamoDataType.MAP)

    // ----------------- IN List Expression -----------------
    infix fun <T> DynamoColumn<T>.inList(values: List<T>): DynamoExpression<Boolean> {

        if (values.isEmpty()) {
            throw DynamodbExpressionFormatException(
                "DynamoDB 'inList' operation cannot be applied on column '${this.name}' with an empty list. "
            )
        }
        return InListExpr(this, values)
    }

    // ----------------- Size Function -----------------
    fun StringColumn.size(): FunctionExpr<Int> = FunctionExpr("size", listOf(this.colExpr()))

    fun BinaryColumn.size(): FunctionExpr<Int> = FunctionExpr("size", listOf(this.colExpr()))

    fun <T> ListColumn.size(): FunctionExpr<Int> = FunctionExpr("size", listOf(this.colExpr()))
    fun <T> DynamoSetColumn<*>.size(): FunctionExpr<Int> = FunctionExpr("size", listOf(this.colExpr()))
    fun <K, V> MapColumn.size(): FunctionExpr<Int> = FunctionExpr("size", listOf(this.colExpr()))


    // ----------------- ValueExpr Extensions -----------------

    infix fun DynamoExpression<Boolean>.or(other: DynamoExpression<Boolean>) = BinaryExpr(this, other, BinaryOp.Or)

    fun not(condition: DynamoExpression<Boolean>) = NotExpr(condition)


    infix fun <T> DynamoColumn<T>.neq(value: T?) = BinaryExpr(this.colExpr(), value.valueExpr(this), BinaryOp.Neq)

    //返回值：比如 size() > 3
    infix fun <T: Number> FunctionExpr<T>.greater(value: T) = BinaryExpr(this, value.valueExpr(), BinaryOp.Gt)
    infix fun <T: Number> FunctionExpr<T>.greaterEq(value: T) = BinaryExpr(this, value.valueExpr(), BinaryOp.Ge)
    infix fun <T: Number> FunctionExpr<T>.less(value: T) = BinaryExpr(this, value.valueExpr(), BinaryOp.Lt)
    infix fun <T: Number> FunctionExpr<T>.lessEq(value: T) = BinaryExpr(this, value.valueExpr(), BinaryOp.Le)

    infix fun <T: Number> FunctionExpr<T>.eq(value: T) = BinaryExpr(this, value.valueExpr(), BinaryOp.Eq)
    infix fun <T: Number> FunctionExpr<T>.neq(value: T) = BinaryExpr(this, value.valueExpr(), BinaryOp.Neq)

}