/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 *
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb


// ----------------- Column / Value Expressions -----------------
internal fun <T> DynamoColumn<T>.colExpr() = ColumnExpr(this)
internal fun <T : Any> T?.valueExpr(column: DynamoColumn<*>? = null) = ValueExpr(this, column)
internal fun <T : Any> T.constantExpr() = ConstantExpr(this.toString())


private fun DynamoTable<*, *>.replaceColumn(columnName: String, newColumn: DynamoColumn<*>) {
    this.columnNames[columnName]?.let {
        this.columns.remove(it)
    }
    this.columns.add(newColumn)
    this.columnNames[columnName] = newColumn
}


fun StringColumn<String>.nullable(): StringColumn<String?> {
    return StringColumn<String?>(this.name, this.table).apply {
        this.nullable = true
        this.defaultValue = null
    }.apply {
       this.table.replaceColumn(this.name, this)
    }
}

fun <C : DynamoColumn<TValue>, TValue : Any> C.default(value: TValue): C {
    this.defaultValue = value
    return this
}


fun BooleanColumn<Boolean>.nullable(): BooleanColumn<Boolean?> {
    return BooleanColumn<Boolean?>(this.name, this.table).apply {
        this.nullable = true
        this.defaultValue = null
    }.apply {
        this.table.replaceColumn(this.name, this)
    }
}

fun <T : Number> NumericColumn<T>.nullable(): NumericColumn<T?> {
    return NumericColumn<T?>(this.name, this.table, this.type).apply {
        this.nullable = true
        this.defaultValue = null
    }.apply {
        this.table.replaceColumn(this.name, this)
    }
}

fun BinaryColumn<ByteArray>.nullable(): BinaryColumn<ByteArray?> {
    return BinaryColumn<ByteArray?>(this.name, this.table).apply {
        this.nullable = true
        this.defaultValue = null
    }.apply {
        this.table.replaceColumn(this.name, this)
    }
}

fun <T> EnumColumn<T>.nullable(): EnumColumn<T?> {
    return EnumColumn<T?>(this.name, this.table, this.enumClass).apply {
        this.nullable = true
        this.defaultValue = null
    }.apply {
        this.table.replaceColumn(this.name, this)
    }
}

fun <TElement> DynamoSetColumn<DynamoSet<TElement>, TElement>.nullable(): DynamoSetColumn<DynamoSet<TElement>?, TElement> {
    return DynamoSetColumn<DynamoSet<TElement>?, TElement>(this.name, this.table, this.dbType).apply {
        this.nullable = true
        this.defaultValue = null
    }.apply {
        this.table.replaceColumn(this.name, this)
    }
}

fun <TList> ListColumn<TList>.nullable(): ListColumn<TList?> {
    return ListColumn<TList?>(this.name, this.table).apply {
        this.nullable = true
        this.defaultValue = null
    }.apply {
        this.table.replaceColumn(this.name, this)
    }
}

fun <TMap> MapColumn<TMap>.nullable(): MapColumn<TMap?> {
    return MapColumn<TMap?>(this.name, this.table).apply {
        this.nullable = true
        this.defaultValue = null
    }.apply {
        this.table.replaceColumn(this.name, this)
    }
}

/**

class DynamoSetColumn<TSet: DynamoSet<TElement>, TElement>(name: String, tableName: String, dbType: String) :
DynamoColumn<TSet>(name, tableName, dbType),
IColumnGetter<DynamoSetColumn<TSet, TElement>, DynamoSet<TElement>>


class MapColumn<TMap: Map<String, Any?>>(name: String, tableName: String) :
DynamoColumn<TMap>(name, tableName, DynamoDataType.MAP),
IColumnGetter<MapColumn<TMap>, TMap>



class ListColumn<TList: List<Any>>(name: String, tableName: String) :
DynamoColumn<TList>(name, tableName, DynamoDataType.LIST),
IColumnGetter<ListColumn<TList>, TList>
 */

