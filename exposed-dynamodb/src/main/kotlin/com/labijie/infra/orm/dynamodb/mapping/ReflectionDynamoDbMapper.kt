/**
 * This file is part of Exposed-DynamoDB project.
 * Copyright (c) 2025
 * @author Anders Xiao
 *
 * File Create: 2025/9/4
 */

package com.labijie.infra.orm.dynamodb.mapping

import com.labijie.infra.orm.dynamodb.DynamoColumn
import com.labijie.infra.orm.dynamodb.toDbValue
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap

object ReflectionDynamoDbMapper : IDynamoDbMapper {

    private val logger by lazy {
        LoggerFactory.getLogger(ReflectionDynamoDbMapper::class.java)
    }

    data class ValueField(val getter: Method, val setter: Method)

    private val fieldToColumnCache = ConcurrentHashMap<String, DynamoColumn<*>?>()
    private val attributeToColumnCache = ConcurrentHashMap<String, DynamoColumn<*>?>()
    private val columnToFieldCache = ConcurrentHashMap<String, String>()

    private val fieldsCache = ConcurrentHashMap<Class<*>, Map<String, ValueField>>()

    private fun findColumnByField(tableName: String, fieldName: String): DynamoColumn<*>? {
        return fieldToColumnCache.getOrPut("${tableName}.${fieldName}".lowercase()) {
            var column: DynamoColumn<*>? = null
            TableRegistry.tables[tableName]?.let { tables ->
                for (table in tables) {
                    column = table.findColumnByField(fieldName)
                    if (column != null) {
                        break
                    }
                }
            }

            column
        }
    }


    private fun findColumnByName(tableName: String, columnName: String): DynamoColumn<*>? {
        return attributeToColumnCache.getOrPut("${tableName}.${columnName}".lowercase()) {
            var column: DynamoColumn<*>? = null
            TableRegistry.tables[tableName]?.let { tables ->
                for (table in tables) {
                    column = table.findColumnByName(columnName)
                    if (column != null) {
                        break
                    }
                }
            }

            column
        }
    }

    private fun findFiledByColumn(tableName: String, columnName: String): String? {
        return columnToFieldCache.getOrPut("${tableName}.${columnName}".lowercase()) {
            var field: Field? = null
            TableRegistry.tables[tableName]?.let { tables ->
                for (table in tables) {
                    field = table.findFieldByColumn(columnName)
                    if (field != null) {
                        break
                    }
                }
            }
            field?.name
        }
    }




    private fun getFields(clazz: Class<*>): Map<String, ValueField> {
        return fieldsCache.getOrPut(clazz) {
            val methods = clazz.methods.filter { m ->
                Modifier.isPublic(m.modifiers) &&
                        !Modifier.isStatic(m.modifiers) &&
                        !m.isBridge &&
                        !m.isSynthetic
            }

            val getters = mutableMapOf<String, Method>()
            val setters = mutableMapOf<String, Method>()

            for (m in methods) {
                when {
                    // 普通 getter: getXxx()
                    m.parameterCount == 0 && m.name.startsWith("get") && m.name.length > 3 && m.returnType != Void.TYPE -> {
                        val prop = m.name.substring(3).lowercase()
                        getters[prop] = m
                    }
                    // 布尔 getter: isXxx()
                    m.parameterCount == 0 && m.name.startsWith("is") && m.name.length > 2 -> {
                        val rt = m.returnType
                        if (rt == java.lang.Boolean.TYPE || rt == java.lang.Boolean::class.java) {
                            val prop = m.name.substring(2).lowercase()
                            getters[prop] = m
                        }
                    }
                    // setter: setXxx(value)
                    m.parameterCount == 1 && m.name.startsWith("set") && m.name.length > 3 -> {
                        val prop = m.name.substring(3).lowercase()
                        setters[prop] = m
                    }
                }
            }

            // 只保留同时存在 getter & setter 的属性
            val result = mutableMapOf<String, ValueField>()
            for ((prop, g) in getters) {
                val s = setters[prop]
                if (s != null) {
                    result[prop] = ValueField(g, s)
                }
            }
            result
        }
    }

    override fun <T : Any> populateFromDb(
        tableName: String,
        value: T,
        attributes: Map<String, AttributeValue>
    ) {

        if(!TableRegistry.tables.containsKey(tableName)) {
            logger.warn("DynamoDB table $tableName does not exist.")
            return
        }

        attributes.forEach {
            attributeValue->
            val columnName = attributeValue.key
            findColumnByName(tableName, columnName)?.let {
                c ->
                findFiledByColumn(tableName, columnName)?.let {
                    fieldName->
                    val fields = getFields(value::class.java)
                    val field = fields[fieldName.lowercase()]
                    field?.let {
                        val v = c.valueFromDb(attributeValue.value)
                        if (v != null) {
                            try {
                                field.setter.invoke(value, v)
                            } catch (e: Throwable) {
                                logger.warn("DynamoDB table $tableName failed to set value (field: ${attributeValue.key}) '${attributeValue.value}'.\n${e.stackTraceToString()}")
                            }
                        }
                    }
                }
            }
        }

    }

    override fun <T : Any> extractToDb(
        tableName: String,
        value: T
    ): Map<String, AttributeValue> {

        val result = mutableMapOf<String, AttributeValue>()

        val fields = getFields(value::class.java)
        fields.forEach {
           field->
            findColumnByField(tableName, field.key)?.let {
                val v = field.value.getter.invoke(value)
                val attValue = it.toDbValue(v)
                result.putIfAbsent(it.name, attValue)
            }
        }
        return result
    }
}