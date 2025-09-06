/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 *
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb

abstract class StringKeysDynamoTable(tableName: String) : DynamoTable<String, String>(tableName), IDynamoProjection {

    override val keys: DynamoKeys<String, String>
        get() = DynamoKeys(partitionKey(), sortKey())

    protected abstract fun partitionKey(): IColumnIndexable<*, String>
    protected open fun sortKey(): IColumnIndexable<*, String>? { return null }
}