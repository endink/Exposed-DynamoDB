/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb.builder

import com.labijie.infra.orm.dynamodb.*
import com.labijie.infra.orm.dynamodb.exception.DynamodbTypeMismatchException


abstract class ProjectionBaseBuilder(protected val table: DynamoTable) {

    private val selective: MutableSet<IDynamoProjection> = mutableSetOf()

    protected val tableName
        get() = table.tableName

    protected fun IDynamoProjection.addProjection() {
        selective.add(this)
    }


    protected fun renderProjection(ctx: RenderContext): String? {
        val list = LinkedHashSet<String>(selective.size)
        for (p in selective) {
            when (p) {
                is DynamoColumn<*> -> list.add(ctx.placeName(p))
                is DynamoTable -> {
                    p.columns.forEach { column ->
                        list.add(ctx.placeName(column))
                    }
                }
                is DynamoExpression<*> -> list.add(p.render(ctx))
                else-> throw DynamodbTypeMismatchException("Unsupported projection type: '${this::class.simpleName}'$")
            }
        }
        return if(list.isEmpty()) null else list.joinToString(",")
    }

    inner class ProjectionBuilder internal constructor() : IDynamoProjectionBuilder {
        fun projectAll(): ProjectionBuilder {
            selective.clear()
            return this
        }

        fun projectKeyOnly(): ProjectionBuilder {
            selective.clear()
            table.keys.partitionKey.getColumn().addProjection()
            table.keys.sortKey?.getColumn()?.addProjection()
            return this
        }

        fun project(vararg projections: IDynamoProjection): ProjectionBuilder {
            selective.clear()
            projections.forEach {
                it.addProjection()
            }
            return this
        }
    }


    internal fun projection(): ProjectionBuilder {
        return ProjectionBuilder()
    }
}