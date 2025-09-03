/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb

import com.labijie.infra.orm.dynamodb.exception.DuplicateDynamoIndexException
import software.amazon.awssdk.services.dynamodb.model.ProjectionType

@Suppress("SameParameterValue")
abstract class DynamoTable(val tableName: String): IDynamoProjection {

    init {
        DynamodbUtils.checkDynamoName(tableName, 3)
    }

    data class DynamoKeys(val partitionKey: IColumnIndexable<*, *>, val sortKey: IColumnIndexable<*, *>? = null)

    val columns = mutableSetOf<DynamoColumn<*>>()
    val indexes = mutableMapOf<String, DynamoIndex>()

    val columnNames =  mutableSetOf<String>()

    abstract val keys: DynamoKeys

    fun <C : IColumnIndexable<C, TValue>, TValue> C.index(indexName: String, projection: ProjectionType = ProjectionType.ALL, vararg projectedColumns: DynamoColumn<*>): C {

        DynamodbUtils.checkDynamoName(indexName, 3)

       return this.also {
           if(indexName.isBlank()) {
               throw IllegalArgumentException("DynamoDB index name cannot be blank")
           }
           if(indexes.containsKey(indexName)) {
               throw DuplicateDynamoIndexException(indexName, this.getColumn().name)
           }
           indexes.putIfAbsent(indexName, DynamoIndex(indexName, this.getColumn(), projection, *projectedColumns))
       }
    }

    fun <C : DynamoColumn<TValue>, TValue> C.default(value: TValue): C {
        this.defaultValue = value
        return this
    }

    private fun addColumn(column: DynamoColumn<*>) {
        if(!columnNames.add(column.name)) {
            throw IllegalArgumentException("Column '${column.name}' already existed in table $tableName")
        }
        columns.add(column)
    }

    // ----------------- 工厂函数 -----------------
    protected fun string(name: String): StringColumn =
        StringColumn(name, tableName).also { addColumn(it) }

    protected fun integer(name: String): NumericColumn<Int> =
        NumericColumn<Int>(name, tableName, Int::class.java).also { addColumn(it) }

    protected fun short(name: String): NumericColumn<Short> =
        NumericColumn<Short>(name, tableName, Short::class.java).also { addColumn(it) }

    protected fun long(name: String): NumericColumn<Long> =
        NumericColumn<Long>(name, tableName, Long::class.java).also { addColumn(it) }

    protected fun float(name: String): NumericColumn<Float> =
        NumericColumn<Float>(name, tableName, Float::class.java).also { addColumn(it) }

    protected fun double(name: String): NumericColumn<Double> =
        NumericColumn<Double>(name, tableName, Double::class.java).also { addColumn(it) }

    protected fun boolean(name: String): DynamoColumn<Boolean> =
        DynamoColumn<Boolean>(name, tableName, DynamoDataType.BOOLEAN).also { addColumn(it) }

    protected fun binary(name: String): DynamoColumn<ByteArray> =
        BinaryColumn(name, tableName).also { addColumn(it) }

    protected fun <T : Enum<T>> enum(name: String, enumClass: Class<T>): EnumColumn<T> =
        EnumColumn(name, enumClass, tableName).also { addColumn(it) }

    protected fun stringSet(name: String): DynamoSetColumn<String> =
        DynamoSetColumn<String>(name, tableName, DynamoDataType.STRING_SET).also { addColumn(it) }

    protected fun numberSet(name: String): DynamoSetColumn<Number> =
        DynamoSetColumn<Number>(name, tableName, DynamoDataType.NUMBER_SET).also { addColumn(it) }

    protected fun binarySet(name: String): DynamoSetColumn<ByteArray> =
        DynamoSetColumn<ByteArray>(name, tableName, DynamoDataType.BINARY_SET).also { addColumn(it) }

    protected fun list(name: String): ListColumn =
        ListColumn(name, tableName).also { addColumn(it)}

    protected fun map(name: String): MapColumn =
        MapColumn(name, tableName).also { addColumn(it) }

}
