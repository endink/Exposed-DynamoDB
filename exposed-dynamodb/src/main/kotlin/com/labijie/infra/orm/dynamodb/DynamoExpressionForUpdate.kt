/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb

import com.labijie.infra.orm.dynamodb.exception.DynamoDbExpressionFormatException

abstract class DynamoUpdateExpression<TValue>(
    val left: ILeftValueExpression<TValue>
) : DynamoExpression<Void> {
    abstract val Op: String

    abstract val targetName: String

    val leftColumnOrNull: DynamoColumn<TValue>?
        get() {
            return (left as? ColumnExpr<TValue>)?.column
        }

    fun renderWithoutOp(ctx: RenderContext): String {
        return render(ctx).removePrefix("$Op ")
    }

}

interface ILeftGetExpr<T>: ILeftValueExpression<T>  {
    val column: DynamoColumn<*>
}

interface IComputedValueExpr<TReturn> : IRightValueExpression<TReturn>

open class ComputedValueExpr<TReturn>(
    functionName: String,
    args: List<DynamoExpression<*>>
) : FunctionExpr<TReturn>(functionName, args), IComputedValueExpr<TReturn>


class IfNotExistExpr<TReturn>(
    val column: DynamoColumn<TReturn>,
    initValue: TReturn
) : ComputedValueExpr<TReturn>("if_not_exists", listOf(column.colExpr(), initValue.valueExpr(column)))


class SliceExpr(
    val column: DynamoColumn<List<Any>>,
    private val startInclusive: Int = 0,
    private val endExclusive: Int? = null,
) : IComputedValueExpr<List<Any>> {

    override fun render(ctx: RenderContext): String {

        if (startInclusive < 0) {
            throw DynamoDbExpressionFormatException(
                "Invalid slice expression: 'startInclusive' cannot be negative. Got startInclusive=$startInclusive"
            )
        }

        if (endExclusive != null && endExclusive < 0) {
            throw DynamoDbExpressionFormatException(
                "Invalid slice expression: 'endExclusive' cannot be negative. Got endExclusive=$endExclusive"
            )
        }

        return "${ColumnExpr(column).render(ctx)}[${startInclusive}:${endExclusive?.toString() ?: ""}]"
    }
}


/** SET expression: col = value */
class SetExpr<TValue>(leftExpr: ILeftValueExpression<TValue>, val value: IRightValueExpression<*>?) :
    DynamoUpdateExpression<TValue>(leftExpr) {
    override fun render(ctx: RenderContext): String {
        return "$Op ${left.render(ctx)} = ${(value ?: ValueExpr(null, leftColumnOrNull)).render(ctx)}"
    }

    override val targetName: String = leftExpr.targetName
    override val Op: String = "SET"
}

class MinusExpr<TValue: Number>(val left: DynamoExpression<TValue>, val value: TValue): DynamoExpression<TValue>, IComputedValueExpr<TValue> {
    override fun render(ctx: RenderContext): String {
        return "${left.render(ctx)} + ${ValueExpr(value, null).render(ctx)}"
    }
}

class PlusExpr<TValue: Number>(val left: DynamoExpression<TValue>, val value: TValue): DynamoExpression<TValue>, IComputedValueExpr<TValue> {
    override fun render(ctx: RenderContext): String {
        return "${left.render(ctx)} + ${ValueExpr(value, null).render(ctx)}"
    }
}


class AddExpr<TValue>(left: ILeftValueExpression<TValue>, val value: TValue) :
    DynamoUpdateExpression<TValue>(left) {
    override fun render(ctx: RenderContext): String {
        return "$Op ${left.render(ctx)} ${ValueExpr(value, leftColumnOrNull).render(ctx)}"
    }

    override val targetName: String = left.targetName

    override val Op: String = "ADD"
}


class DeleteExpr<TValue>(left: ILeftValueExpression<TValue>, val value: TValue) :
    DynamoUpdateExpression<TValue>(left) {

    override fun render(ctx: RenderContext): String {
        return "$Op ${left.render(ctx)} ${ValueExpr(value, leftColumnOrNull).render(ctx)}"
    }
    override val targetName: String = left.targetName
    override val Op: String = "DELETE "
}

class RemoveExpr<T>(left: ILeftValueExpression<T>) :
    DynamoUpdateExpression<T>(left) {

    override fun render(ctx: RenderContext): String {
        return "$Op ${left.render(ctx)}"
    }
    override val targetName: String = left.targetName
    override val Op: String = "REMOVE"
}

class ColumnGetExpr<T>(override val column: DynamoColumn<T>): ILeftGetExpr<T> {
    override fun render(ctx: RenderContext): String {
        return ctx.placeName(column)
    }
    override val targetName: String = column.name
}

open class SetColumnGetExpr<TElement>(override val column: DynamoColumn<DynamoSet<TElement>>): ILeftGetExpr<DynamoSet<TElement>> {
    override fun render(ctx: RenderContext): String {
        return ctx.placeName(column)
    }
    override val targetName: String = column.name
}

/** Represents a column in a table. */
class ListItemGetExpr(
    val list: ILeftGetExpr<*>,
    val index: Int) : ILeftGetExpr<Any>, IDynamoProjection {

    override val column: DynamoColumn<*> = list.column

    override val targetName: String = "${list.targetName}.[${index}]"

    init {
        if (index < 0) {
            throw DynamoDbExpressionFormatException("List column 'index' cannot be negative. Got index=$index, column=${column.name}")
        }
    }

    override fun render(ctx: RenderContext): String {
        return "${list.render(ctx)}[${index}]"
    }
}

class MapItemGetExpr(
    val map: ILeftGetExpr<*>,
    val key: String
) : ILeftGetExpr<Any>, IDynamoProjection {


    override val column: DynamoColumn<*> = map.column

    override val targetName: String = "${map.targetName}.[${key}]"

    init {
        if (key.isBlank()) {
            throw DynamoDbExpressionFormatException("Map column 'key' cannot be negative. Got key=$key, column=${column.name}")
        }
    }

    override fun render(ctx: RenderContext): String {
        return "${map.render(ctx)}.${key}"
    }
}
