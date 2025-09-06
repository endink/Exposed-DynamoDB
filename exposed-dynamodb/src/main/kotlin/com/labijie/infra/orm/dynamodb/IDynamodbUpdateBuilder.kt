/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb

import com.labijie.infra.orm.dynamodb.builder.DynamoUpdateBuilder


internal fun IDynamodbUpdateBuilder.addExpression(value: DynamoUpdateExpression<*>) {
    if(this is DynamoUpdateBuilder<*, *>.DynamoSegmentsBuilder) {
        this.expressions.add(value)
    }
}

interface IDynamodbUpdateBuilder {

    infix operator fun <T: Number> ColumnGetExpr<T>.plusAssign(value: T) {
         addExpression(AddExpr(this, value))
    }

    infix operator fun <T: Number> ColumnGetExpr<T>.minusAssign(value: T) {
        addExpression(DeleteExpr(this, value))
    }

    infix operator fun <T: Number> NumericColumn<T>.minus(value: T): MinusExpr<T> {
        return MinusExpr(this.colExpr(), value)
    }

    infix operator fun <T: Number> NumericColumn<T>.plus(value: T): PlusExpr<T> {
        return PlusExpr(this.colExpr(), value)
    }

    infix operator fun <TElement> SetColumnGetExpr<TElement>.minusAssign(values: DynamoSet<TElement>) {
        return addExpression(DeleteExpr(this, values))
    }

    infix operator fun <TElement> SetColumnGetExpr<TElement>.plusAssign(values: DynamoSet<TElement>) {
        return addExpression(AddExpr(this, values))
    }

//    fun DynamoColumn<List<Any>>.slice(startInclusive : Int, endExclusive : Int? = null): SliceExpr {
//        return SliceExpr(this, startInclusive, endExclusive)
//    }

    fun <T> DynamoColumn<T>.ifNotExists(initValue: T): IfNotExistExpr<T> {
        return IfNotExistExpr(this, initValue)
    }

    infix operator fun <T: Number> IfNotExistExpr<T>.minus(value: T): MinusExpr<T> {
        return MinusExpr(this, value)
    }

    infix operator fun <T: Number> IfNotExistExpr<T>.plus(value: T): PlusExpr<T> {
        return PlusExpr(this, value)
    }

    infix operator fun <T: Number> DynamoColumn<T>.minus(value: T): MinusExpr<T> {
        return MinusExpr(this.colExpr(), value)
    }

    infix operator fun <T: Number> DynamoColumn<T>.plus(value: T): PlusExpr<T> {
        return PlusExpr(this.colExpr(), value)
    }

    infix operator fun ILeftValueExpression<List<Any>>.plus(values: List<Any>): ComputedValueExpr<List<Any>> {
        return ComputedValueExpr("list_append", listOf(this, ValueExpr(values, null)))
    }

    infix operator fun ILeftValueExpression<List<Any>>.plus(right: IComputedValueExpr<List<Any>>): ComputedValueExpr<List<Any>> {
        return ComputedValueExpr("list_append", listOf(this, right))
    }

    infix operator fun IComputedValueExpr<List<Any>>.plus(right: List<Any>): ComputedValueExpr<List<Any>> {
        return ComputedValueExpr("list_append", listOf(this, ValueExpr(right, null)))
    }

    infix operator fun IComputedValueExpr<List<Any>>.plus(right: IComputedValueExpr<List<Any>>): ComputedValueExpr<List<Any>> {
        return ComputedValueExpr("list_append", listOf(this, right))
    }

    fun ILeftGetExpr<*>.remove() {
        addExpression(RemoveExpr(this))
    }


    infix operator fun ColumnGetExpr<List<Any>>.get(int: Int) : ListItemGetExpr {
        return ListItemGetExpr(this, int)
    }

    infix operator fun ColumnGetExpr<Map<String, *>>.get(key: String) : MapItemGetExpr {
        return MapItemGetExpr(this, key)
    }

    //无限递归
    infix operator fun ILeftGetExpr<Any>.get(int: Int): ListItemGetExpr {
        return ListItemGetExpr(this, int)
    }

    infix operator fun ILeftGetExpr<Any>.get(key: String): MapItemGetExpr {
        return MapItemGetExpr(this, key)
    }

    infix operator fun <T> DynamoColumn<List<Any>>.get(index: Int): ListItemValueExpr<T> {
        return ListItemValueExpr(this, index)
    }

    infix operator fun <T> DynamoColumn<Map<String, *>>.get(key: String): MapItemValueExpr<T> {
        return MapItemValueExpr(this, key)
    }

    operator fun ColumnGetExpr<List<Any>>.set(int: Int, value: Any?) {
        val item = ListItemGetExpr(this, int)
        addExpression(SetExpr(item, value.valueExpr()))
    }


    operator fun ColumnGetExpr<Map<String, *>>.set(key: String, value: Any?) {
        val item = MapItemGetExpr(this, key)
        addExpression(SetExpr(item, value.valueExpr()))
    }


    operator fun ILeftGetExpr<Any>.set(int: Int, value: Any?) {
        val item = ListItemGetExpr(this, int)
        addExpression(SetExpr(item, value.valueExpr()))
    }


    operator fun ILeftGetExpr<Any>.set(key: String, value: Any?) {
        val item = MapItemGetExpr(this, key)
        addExpression(SetExpr(item, value.valueExpr()))
    }

//    operator fun ILeftGetExpr<Any>.set(key: String, value: IComputedValueExpr<Any>) {
//        val item = MapItemGetExpr(this, key)
//        addExpression(SetExpr(item, value))
//    }


    infix operator fun ILeftGetExpr<Any>.minusAssign(values: DynamoSet<*>) {
        return addExpression(DeleteExpr(this, values))
    }

    infix operator fun ILeftGetExpr<Any>.plusAssign(values: DynamoSet<*>) {
        return addExpression(AddExpr(this, values))
    }
}
