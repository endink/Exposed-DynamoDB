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
abstract class DynamoColumn<TValue>(
    val name: String,
    internal val table: DynamoTable<*, *>,
    private val dbType: String
) : IDynamoProjection {
    internal var defaultValue: TValue? = null

    internal var nullable: Boolean = false

    internal var mapper: DynamoValueConverter<TValue>? = null


    internal val tableName: String by lazy {
        table.tableName
    }

    init {
        DynamodbUtils.checkDynamoName(name)
    }

    protected fun setMapper(mapper: DynamoValueConverter<TValue>) {
        this.mapper = mapper
    }

    fun valueFromDb(dbValue: AttributeValue): TValue {
        val m = mapper
        if (m != null) {
            return m.valueFromDb(dbValue)
        }

        @Suppress("UNCHECKED_CAST")
        return AttributeValueConverter.fromDb(dbValue, this) as TValue
    }

    fun notNullValueToDB(value: TValue): AttributeValue {
        val nonNull: TValue = requireNotNull(value) { "value cannot be null" }

        val m = mapper
        if (m != null) {
            return m.notNullValueToDB(value as Any)
        }

        return AttributeValueConverter.toDb(value, this)
    }

    fun dynamoDbType(): String = dbType
    open val isEnum = false


}

class BooleanColumn<TValue>(
    name: String,
    table: DynamoTable<*, *>
) : DynamoColumn<Boolean>(name, table, DynamoDataType.BOOLEAN), IColumnGetter<BooleanColumn<TValue>, TValue>


open class NumericColumn<TValue>(name: String, table: DynamoTable<*, *>, internal val type: NumericType) :
    DynamoColumn<TValue>(name, table, DynamoDataType.NUMBER),
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


open class StringColumn<T>(name: String, table: DynamoTable<*, *>) :
    DynamoColumn<T>(name, table, DynamoDataType.STRING),
    IColumnBounded<StringColumn<T>, T>,
    IColumnIndexable<StringColumn<T>, T>


open class BinaryColumn<T>(name: String, table: DynamoTable<*, *>) :
    DynamoColumn<T>(name, table, DynamoDataType.BINARY),
    IColumnBounded<BinaryColumn<T>, T>,
    IColumnIndexable<BinaryColumn<T>, T>


// ----------------- 枚举列 -----------------
open class EnumColumn<T>(name: String,  table: DynamoTable<*, *>, internal val enumClass: Class<*>) :
    DynamoColumn<T>(name, table, DynamoDataType.NUMBER),
    IColumnBounded<EnumColumn<T>, T> {

    override val isEnum: Boolean
        get() = true
}


class DynamoSetColumn<TSet, TElement>(name: String, table: DynamoTable<*, *>, internal val dbType: String) :
    DynamoColumn<TSet>(name, table, dbType),
    IColumnGetter<DynamoSetColumn<TSet, TElement>, DynamoSet<TElement>>


class MapColumn<TMap>(name: String, table: DynamoTable<*, *>) :
    DynamoColumn<TMap>(name, table, DynamoDataType.MAP),
    IColumnGetter<MapColumn<TMap>, TMap>


class ListColumn<TList>(name: String, table: DynamoTable<*, *>) :
    DynamoColumn<TList>(name, table, DynamoDataType.LIST),
    IColumnGetter<ListColumn<TList>, TList>
