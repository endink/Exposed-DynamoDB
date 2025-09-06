/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb

import software.amazon.awssdk.services.dynamodb.model.AttributeValue

interface IColumnGetter<TColumn : IColumnGetter<TColumn, TValue>, TValue> {
    @Suppress("UNCHECKED_CAST")
    fun getColumn(): DynamoColumn<TValue> {
        return this as DynamoColumn<TValue>
    }
}


interface IColumnIndexable<TColumn : IColumnIndexable<TColumn, TValue>, TValue> : IColumnGetter<TColumn, TValue>


interface IColumnBounded<TColumn : IColumnBounded<TColumn, TValue>, TValue> : IColumnGetter<TColumn, TValue>


// ----------------- Column -----------------
open class DynamoColumn<TValue>(
    internal val name: String,
    internal val tableName: String,
    private val dbType: String
): IDynamoProjection {
    var defaultValue: TValue? = null

    init {
        DynamodbUtils.checkDynamoName(name)
        DynamodbUtils.checkDynamoName(tableName, 3)
    }

    open fun valueFromDb(dbValue: AttributeValue): TValue {
        @Suppress("UNCHECKED_CAST")
        return AttributeValueConverter.fromDb(dbValue, this) as TValue
    }

    open fun notNullValueToDB(value: TValue): AttributeValue {
        return AttributeValueConverter.toDb(value, this)
    }

    fun dynamoDbType(): String = dbType
    open val isEnum = false


}



open class NumericColumn<TValue : Number>(name: String, tableName: String, val type: NumericType) :
    DynamoColumn<TValue>(name, tableName, DynamoDataType.NUMBER),
    IColumnBounded<NumericColumn<TValue>, TValue>,
    IColumnIndexable<NumericColumn<TValue>, TValue> {

    enum class NumericType {
        Long,
        Int,
        Float,
        Double,
        Short,
    }
}



open class StringColumn(name: String, tableName: String) :
    DynamoColumn<String>(name, tableName, DynamoDataType.STRING),
    IColumnBounded<StringColumn, String>,
    IColumnIndexable<StringColumn, String>


open class BinaryColumn(name: String, tableName: String) :
    DynamoColumn<ByteArray>(name, tableName, DynamoDataType.BINARY),
    IColumnBounded<BinaryColumn, ByteArray>,
    IColumnIndexable<BinaryColumn, ByteArray>



// ----------------- 枚举列 -----------------
open class EnumColumn<T : Enum<T>>(name: String, internal val enumClass: Class<T>, tableName: String) :
    DynamoColumn<T>(name, tableName, DynamoDataType.NUMBER),
    IColumnBounded<EnumColumn<T>, T> {
    override val isEnum: Boolean
        get() = true
}




class DynamoSetColumn<TElement>(name: String, tableName: String, dbType: String) :
    DynamoColumn<DynamoSet<TElement>>(name, tableName, dbType)


class MapColumn(name: String, tableName: String) : DynamoColumn<Map<String, Any?>>(name, tableName, DynamoDataType.MAP)


class ListColumn(name: String, tableName: String) : DynamoColumn<List<Any>>(name, tableName, DynamoDataType.LIST)
