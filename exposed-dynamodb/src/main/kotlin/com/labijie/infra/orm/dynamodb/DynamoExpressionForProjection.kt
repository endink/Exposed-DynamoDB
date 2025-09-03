/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb

import com.labijie.infra.orm.dynamodb.exception.DynamodbExpressionFormatException


interface IDynamoProjection



interface IProjectedValueExpression<T> : DynamoExpression<T>, IDynamoProjection {
    val column: DynamoColumn<*>
}

class ListItemValueExpr<T>(override val column: DynamoColumn<List<Any>>, val index: Int) : IComputedValueExpr<T>, IProjectedValueExpression<T>, IDynamoProjection {
    init {
        if (index < 0) {
            throw DynamodbExpressionFormatException("Invalid ListItemValueExpr: 'index' cannot be negative. Got index=$index, column=${column.name}")
        }
    }

    override fun render(ctx: RenderContext): String {
        return "${ColumnExpr(column).render(ctx)}[${index}]"
    }
}

class MapItemValueExpr<T>(override val column: DynamoColumn<Map<String,*>>, val key: String) : IComputedValueExpr<T>, IProjectedValueExpression<T> {
    init {
        if (key.isBlank()) {
            throw DynamodbExpressionFormatException("Invalid MapItemValueExpr: key cannot be empty at column='column.name")
        }
    }

    override fun render(ctx: RenderContext): String {
        return "${ColumnExpr(column).render(ctx)}.${key}"
    }
}

class NestedMapItemValueExpr(
    override val column: DynamoColumn<*>,
    val map: IProjectedValueExpression<*>,
    val key: String
) : IProjectedValueExpression<Any>, IDynamoProjection {

    init {
        if (key.isBlank()) {
            throw DynamodbExpressionFormatException("Map column 'key' cannot be negative. Got key=$key, column=${column.name}")
        }
    }

    override fun render(ctx: RenderContext): String {
        return "${map.render(ctx)}.${key}"
    }
}

class NestedListItemValueExpr(
    override val column: DynamoColumn<*>,
    val map: IProjectedValueExpression<*>,
    val index: Int
) : IProjectedValueExpression<Any>, IDynamoProjection {

    init {
        if (index < 0) {
            throw DynamodbExpressionFormatException("Invalid ListItemValueExpr: 'index' cannot be negative. Got index=$index, column=${column.name}")
        }
    }

    override fun render(ctx: RenderContext): String {
        return "${map.render(ctx)}[${index}]"
    }
}

