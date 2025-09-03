/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb

import software.amazon.awssdk.services.dynamodb.model.AttributeValue


class RenderContext(private val useAttributeName: Boolean) {
    private var dummy: Boolean = false

    companion object {
        val DUMMY by lazy {
            RenderContext(false).apply { dummy = true }
        }
    }

    private var counter = 0
    private var columnCounter = 0
    val values = mutableMapOf<String, AttributeValue>()
    private val names  = mutableMapOf<String, ColumnAttribute>()

    val attributeNames: Map<String, String>
        get() {
            return names.values.associate { it.maskedName to it.attributeName }
        }

    data class ColumnAttribute(val maskedName: String, val attributeName: String)

    private fun nextPlaceholder(): String = ":v${counter++}"

    private fun nextColumn(): String = "#c${columnCounter++}"

    fun placeValue(value: Any?, hint: DynamoColumn<*>?): String {
        if(dummy) {
            return ":value"
        }
        val ph = nextPlaceholder()
        val attrValue = hint?.toDbValue(value) ?: AttributeValueConverter.toDb(value)
        values[ph] = attrValue
        return ph
    }

    fun placeName(column: DynamoColumn<*>): String {

        if(useAttributeName) {
            val v = names.getOrPut(column.name) {
                val paramName = nextColumn()
                ColumnAttribute(paramName, column.name)
            }
            return v.maskedName
        }else {
            return column.name
        }
    }
}
