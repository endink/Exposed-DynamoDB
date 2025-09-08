/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 *
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb

abstract class StringKeysDynamoTable : DynamoTable<String, String>, IDynamoProjection {

    constructor(tableName: String) : super(tableName)

    constructor(tableNameProvider: ITableNameProvider) : super(tableNameProvider)

    override val keys: DynamoKeys<String, String>
        get() = DynamoKeys(partitionKey(), sortKey())

    protected abstract fun partitionKey(): IColumnIndexable<*, String>
    protected open fun sortKey(): IColumnIndexable<*, String>? { return null }
}