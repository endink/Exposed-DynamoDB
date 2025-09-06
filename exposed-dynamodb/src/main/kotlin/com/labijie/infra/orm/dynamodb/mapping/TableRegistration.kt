/**
 * This file is part of Exposed-DynamoDB project.
 * Copyright (c) 2025
 * @author Anders Xiao
 *
 * File Create: 2025/9/5
 */

package com.labijie.infra.orm.dynamodb.mapping

import com.labijie.infra.orm.dynamodb.DynamoColumn
import com.labijie.infra.orm.dynamodb.DynamoTable
import com.labijie.infra.orm.dynamodb.exception.DuplicateDynamoAttributeException
import kotlin.reflect.full.memberProperties

class TableRegistration(val table: DynamoTable) {

    val columns by lazy {
        val columns = HashMap<String, DynamoColumn<*>>(table::class.memberProperties.size)
        table::class.memberProperties.forEach {
            if (it is DynamoColumn<*>) {
                val exist = columns.putIfAbsent(it.name, it)
                if (exist != null) {
                    throw DuplicateDynamoAttributeException(it.tableName, it.name)
                }
            }
        }
        columns
    }


    fun findColumn(fieldName: String): DynamoColumn<*>? {
        return columns[fieldName]
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TableRegistration

        if (table != other.table) return false
        if (columns != other.columns) return false

        return true
    }

    override fun hashCode(): Int {
        return table.hashCode()
    }
}