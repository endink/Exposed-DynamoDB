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
import java.lang.reflect.Field

class TableRegistration(val table: DynamoTable<*, *>) {

    companion object {
        fun getAllFields(clazz: Class<*>): List<Field> {
            val fields = mutableListOf<Field>()
            var current: Class<*>? = clazz
            while (current != null && current != Any::class.java) {
                fields += current.declaredFields
                current = current.superclass
            }
            return fields
        }
    }

    private class Mapping(val fieldToColumn: Map<String, DynamoColumn<*>>, val columnToField: Map<String, Field>)

    private val mapping by lazy {
        val fieldToColumn = mutableMapOf<String, DynamoColumn<*>>()
        val columnToField = mutableMapOf<String, Field>()

        getAllFields(table::class.java)
            .filter { DynamoColumn::class.java.isAssignableFrom(it.type) }
            .forEach {
                property->
                property.isAccessible = true
                val col = property.get(table) as DynamoColumn<*>
                if(table.columns.contains(col)) {
                    fieldToColumn.putIfAbsent(property.name, col)
                    columnToField.putIfAbsent(col.name, property)
                }
            }

        Mapping(fieldToColumn, columnToField)
    }

    fun findFieldByColumn(columnName: String): Field? {
        return mapping.columnToField[columnName]
    }

    fun findColumnByField(fieldName: String): DynamoColumn<*>? {
        return mapping.fieldToColumn[fieldName]
    }

    fun findColumnByName(columnName: String): DynamoColumn<*>? {
        return table.columnNames[columnName]
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TableRegistration

        return table == other.table
    }

    override fun hashCode(): Int {
        return table.hashCode()
    }
}