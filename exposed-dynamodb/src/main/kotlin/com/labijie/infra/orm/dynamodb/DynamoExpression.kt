/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb

interface DynamoExpression<T> {
    /** Render this expression into a DynamoDB expression string using the given context. */
    fun render(ctx: RenderContext): String
}


interface IRightValueExpression<T> : DynamoExpression<T>

interface ILeftValueExpression<T> : DynamoExpression<T> {
    val targetName: String
}

// ----------------- Column Expression -----------------
/** Represents a column in a table. */
class ColumnExpr<T>(val column: DynamoColumn<T>) : DynamoExpression<T>, ILeftValueExpression<T> {
    override fun render(ctx: RenderContext) = ctx.placeName(column)

    override val targetName: String = column.name
}



// ----------------- Value Expression -----------------
/** Represents a literal value in an expression. */
class ValueExpr<T>(val value: T, val hint: DynamoColumn<*>?) : IRightValueExpression<T> {
    override fun render(ctx: RenderContext) = ctx.placeValue(value, hint)
}

// ----------------- Value Expression -----------------
/** Represents a literal value in an expression. */
class ConstantExpr<T>(val value: T) : IRightValueExpression<T> {
    override fun render(ctx: RenderContext) = value.toString()
}

private val DynamoExpression<*>.isSimpleExpression
    get() = when (this) {
        is ColumnExpr<*>, is ValueExpr<*>, is ConstantExpr -> true
        else -> false
    }

// ----------------- Binary Expression -----------------
/** Represents a binary operation between two expressions, e.g., =, <>, >, AND, OR. */
enum class BinaryOp { Eq, Neq, Gt, Ge, Lt, Le, And, Or }

/** Extension to render operator to string */
internal fun BinaryOp.render(): String = when (this) {
    BinaryOp.Eq -> "="
    BinaryOp.Neq -> "<>"
    BinaryOp.Gt -> ">"
    BinaryOp.Ge -> ">="
    BinaryOp.Lt -> "<"
    BinaryOp.Le -> "<="
    BinaryOp.And -> "AND"
    BinaryOp.Or -> "OR"
}

class BinaryExpr(
    val left: DynamoExpression<*>,
    val right: DynamoExpression<*>,
    val op: BinaryOp
) : DynamoExpression<Boolean> {
    override fun render(ctx: RenderContext): String {

        val left = if (!left.isSimpleExpression && left !is FunctionExpr<*>) {
            "(${left.render(ctx)})"
        } else {
            left.render(ctx)
        }

        val right = if (!right.isSimpleExpression && right !is FunctionExpr<*>) {
            "(${right.render(ctx)})"
        } else {
            right.render(ctx)
        }

        return "$left ${op.render()} $right"
    }
}

/**
 * Represents a logical NOT operation
 * Example: NOT expr
 */
class NotExpr(
    val expr: DynamoExpression<Boolean>
) : DynamoExpression<Boolean> {
    override fun render(ctx: RenderContext): String {
        return "NOT (${expr.render(ctx)})"
    }
}

/**
 * Represents a DynamoDB function call, e.g., begins_with(column, value)
 */
open class FunctionExpr<TReturn>(
    val functionName: String,
    val args: List<DynamoExpression<*>>
) : DynamoExpression<TReturn> {
    override fun render(ctx: RenderContext): String {
        val renderedArgs = args.joinToString(", ") { it.render(ctx) }
        return "$functionName($renderedArgs)"
    }
}


// ----------------- Between Expression -----------------
/** Represents a BETWEEN expression for numeric or comparable columns. */
class BetweenExpr<T>(
    val column: DynamoColumn<T>,
    val min: T,
    val max: T
) : DynamoExpression<Boolean> {
    override fun render(ctx: RenderContext) =
        "${ctx.placeName(column)} BETWEEN ${ctx.placeValue(min, column)} AND ${ctx.placeValue(max, column)}"
}


// ----------------- IN List Expression -----------------
/** Represents a column IN (value1, value2, ...) expression. */
class InListExpr<T>(val column: DynamoColumn<T>, val values: List<T>) : DynamoExpression<Boolean> {
    init {
        require(values.isNotEmpty()) { "IN list cannot be empty" }
    }

    override fun render(ctx: RenderContext): String {
        val placeholders = values.joinToString(", ") { ctx.placeValue(it, column) }
        return "${ctx.placeName(column)} IN ($placeholders)"
    }
}
