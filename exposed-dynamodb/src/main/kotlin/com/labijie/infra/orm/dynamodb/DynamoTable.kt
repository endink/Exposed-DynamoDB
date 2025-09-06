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

    val columns = mutableSetOf<DynamoColumn<*>>()
    val indexes = mutableMapOf<String, DynamoIndex>()

    internal val columnNames = mutableMapOf<String, DynamoColumn<*>>()

    init {
        TableRegistry.registryTable(this)
    }


    internal val primaryKey by lazy {
        keys
    }

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
            if (indexes.containsKey(indexName)) {
                throw DuplicateDynamoIndexException(this.getColumn().tableName, indexName, this.getColumn().name)
            }
            indexes.putIfAbsent(indexName, DynamoIndex(indexName, this.getColumn(), projection, *projectedColumns))
        }
    }

    fun <C : DynamoColumn<TValue>, TValue> C.default(value: TValue): C {
        this.defaultValue = value
        return this
    }

    private fun addColumn(column: DynamoColumn<*>) {
        if (columnNames.containsKey(column.name)) {
            throw IllegalArgumentException("Column '${column.name}' already existed in table '$tableName'.")
        }
        columnNames[column.name] = column
        columns.add(column)
    }

    // ----------------- 工厂函数 -----------------
    protected fun string(name: String): StringColumn =
        StringColumn(name, tableName).also { addColumn(it) }

    protected fun integer(name: String): NumericColumn<Int> =
        NumericColumn<Int>(name, tableName, NumericColumn.NumericType.Int).also { addColumn(it) }

    protected fun short(name: String): NumericColumn<Short> =
        NumericColumn<Short>(name, tableName, NumericColumn.NumericType.Short).also { addColumn(it) }

    protected fun long(name: String): NumericColumn<Long> =
        NumericColumn<Long>(name, tableName, NumericColumn.NumericType.Long).also { addColumn(it) }

    protected fun float(name: String): NumericColumn<Float> =
        NumericColumn<Float>(name, tableName, NumericColumn.NumericType.Float).also { addColumn(it) }

    protected fun double(name: String): NumericColumn<Double> =
        NumericColumn<Double>(name, tableName, NumericColumn.NumericType.Double).also { addColumn(it) }

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
        ListColumn(name, tableName).also { addColumn(it) }

    protected fun map(name: String): MapColumn =
        MapColumn(name, tableName).also { addColumn(it) }

}
