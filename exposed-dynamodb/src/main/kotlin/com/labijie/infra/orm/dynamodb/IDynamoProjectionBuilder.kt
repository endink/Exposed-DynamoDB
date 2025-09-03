/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb

interface IDynamoProjectionBuilder {

    infix operator fun DynamoColumn<List<Any>>.get(index: Int): ListItemValueExpr<Any> {
        return ListItemValueExpr(this, index)
    }

    infix operator fun DynamoColumn<Map<String, *>>.get(key: String): MapItemValueExpr<Any> {
        return MapItemValueExpr(this, key)
    }

    infix operator fun IProjectedValueExpression<*>.get(index: Int): NestedListItemValueExpr {
        return NestedListItemValueExpr(this.column, this, index)
    }

    infix operator fun IProjectedValueExpression<*>.get(key: String): NestedMapItemValueExpr {
        return NestedMapItemValueExpr(this.column, this, key)
    }
}
