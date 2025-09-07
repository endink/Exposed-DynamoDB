/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 *
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb

import com.labijie.infra.orm.dynamodb.exception.DuplicateDynamoIndexException
import com.labijie.infra.orm.dynamodb.mapping.TableRegistry
import software.amazon.awssdk.services.dynamodb.model.ProjectionType
import java.util.UUID

@Suppress("SameParameterValue")
abstract class DynamoTable<TPartitionKey, TSortKey>(val tableName: String) : IDynamoProjection {

    open class DynamoKeys<TPartitionKey, TSortKey>(val partitionKey: IColumnIndexable<*, TPartitionKey>, val sortKey: IColumnIndexable<*, TSortKey>? = null)

    private val tableIndexes = mutableMapOf<String, DynamoIndex>()


    init {
        TableRegistry.registryTable(this)
    }


    val primaryKey by lazy {
        keys
    }

    val indexes: Map<String, DynamoIndex>
        get() { return tableIndexes }

    protected abstract val keys: DynamoKeys<out TPartitionKey, out TSortKey>

    fun <C : IColumnIndexable<C, TValue>, TValue> C.index(
        indexName: String,
        projection: ProjectionType = ProjectionType.ALL,
        vararg projectedColumns: DynamoColumn<*>
    ): C {

        DynamodbUtils.checkDynamoName(indexName, 3)

        return this.also {
            if (indexName.isBlank()) {
                throw IllegalArgumentException("DynamoDB index name cannot be blank")
            }
            if (tableIndexes.containsKey(indexName)) {
                throw DuplicateDynamoIndexException(this.getColumn().tableName, indexName, this.getColumn().name)
            }
            tableIndexes.putIfAbsent(indexName, DynamoIndex(indexName, this.getColumn(), projection, *projectedColumns))
        }
    }


    val columns = mutableSetOf<DynamoColumn<*>>()
    internal val columnNames = mutableMapOf<String, DynamoColumn<*>>()


    private fun addColumn(column: DynamoColumn<*>) {
        if (columnNames.containsKey(column.name)) {
            throw IllegalArgumentException("Column '${column.name}' already existed in table '$tableName'.")
        }
        columnNames[column.name] = column
        columns.add(column)
    }

    // ----------------- 工厂函数 -----------------
    protected fun string(name: String): StringColumn<String> =
        StringColumn<String>(name, this).also { addColumn(it) }

    protected fun integer(name: String): NumericColumn<Int> =
        NumericColumn<Int>(name, this, NumericColumn.NumericType.Int).also { addColumn(it) }

    protected fun short(name: String): NumericColumn<Short> =
        NumericColumn<Short>(name, this, NumericColumn.NumericType.Short).also { addColumn(it) }

    protected fun long(name: String): NumericColumn<Long> =
        NumericColumn<Long>(name, this, NumericColumn.NumericType.Long).also { addColumn(it) }

    protected fun float(name: String): NumericColumn<Float> =
        NumericColumn<Float>(name, this, NumericColumn.NumericType.Float).also { addColumn(it) }

    protected fun double(name: String): NumericColumn<Double> =
        NumericColumn<Double>(name, this, NumericColumn.NumericType.Double).also { addColumn(it) }

    protected fun boolean(name: String): DynamoColumn<Boolean> =
        BooleanColumn<Boolean>(name, this).also { addColumn(it) }

    protected fun binary(name: String): DynamoColumn<ByteArray> =
        BinaryColumn<ByteArray>(name, this).also { addColumn(it) }

    protected fun <T : Enum<T>> enum(name: String, enumClass: Class<T>): EnumColumn<T> =
        EnumColumn<T>(name, this, enumClass).also { addColumn(it) }

    protected fun stringSet(name: String): DynamoSetColumn<DynamoSet<String>, String> =
        DynamoSetColumn<DynamoSet<String>, String>(name, this, DynamoDataType.STRING_SET).also { addColumn(it) }

    protected fun numberSet(name: String): DynamoSetColumn<DynamoSet<Number>, Number> =
        DynamoSetColumn<DynamoSet<Number>, Number>(name, this, DynamoDataType.NUMBER_SET).also { addColumn(it) }

    protected fun binarySet(name: String): DynamoSetColumn<DynamoSet<ByteArray>, ByteArray> =
        DynamoSetColumn<DynamoSet<ByteArray>, ByteArray>(name, this, DynamoDataType.BINARY_SET).also { addColumn(it) }

    protected fun list(name: String): ListColumn<List<Any>> =
        ListColumn<List<Any>>(name, this).also { addColumn(it) }

    protected fun map(name: String): MapColumn<Map<String, Any?>> =
        MapColumn<Map<String, Any?>>(name, this).also { addColumn(it) }

}
