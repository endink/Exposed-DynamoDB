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


abstract class ProjectionBaseBuilder(protected val table: DynamoTable<*, *>) {

    private val selective: MutableSet<IDynamoProjection> = mutableSetOf()

    val tableName
        get() = table.tableName

    protected fun IDynamoProjection.addProjection() {
        selective.add(this)
    }

    fun <PK, SK> DynamoTable<PK, SK>.exclude(vararg columns: DynamoColumn<*>): IDynamoProjection {
        return DynamoColumnsBuilder(table, columns.asIterable())
    }

    fun <PK, SK> DynamoTable<PK, SK>.exclude(columns: Collection<DynamoColumn<*>>): IDynamoProjection {
        return DynamoColumnsBuilder(table, columns)
    }

    protected fun renderProjection(ctx: RenderContext): String? {
        val list = LinkedHashSet<String>(selective.size)
        for (p in selective) {
            when (p) {
                is DynamoColumn<*> -> list.add(ctx.placeName(p))
                is DynamoTable<*, *> -> {
                    p.columns.forEach { column ->
                        list.add(ctx.placeName(column))
                    }
                }

                is DynamoExpression<*> -> list.add(p.render(ctx))
                is DynamoColumnsBuilder<*, *> ->  {
                    p.build().forEach {
                        list.add(ctx.placeName(it))
                    }
                }
                else -> throw DynamodbTypeMismatchException("Unsupported projection type: '${this::class.simpleName}'$")
            }
        }
        return if (list.isEmpty()) null else list.joinToString(",")
    }

    inner class ProjectionBuilder internal constructor() : IDynamoProjectionBuilder {
        fun projectAll(): ProjectionBuilder {
            selective.clear()
            return this
        }

        fun projectTableWithoutKeys(): ProjectionBuilder {
            selective.clear()
            selective.add(
                table.exclude(
                    listOfNotNull(
                        table.primaryKey.partitionKey.getColumn(),
                        table.primaryKey.sortKey?.getColumn()
                    )
                )
            )
            return this
        }

        fun projectKeyOnly(): ProjectionBuilder {
            selective.clear()
            table.primaryKey.partitionKey.getColumn().addProjection()
            table.primaryKey.sortKey?.getColumn()?.addProjection()
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