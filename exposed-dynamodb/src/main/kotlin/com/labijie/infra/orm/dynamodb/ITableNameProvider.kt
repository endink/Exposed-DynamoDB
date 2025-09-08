/**
 * This file is part of Exposed-DynamoDB project.
 * Copyright (c) 2025
 * @author Anders Xiao
 *
 * File Create: 2025/9/8
 */

package com.labijie.infra.orm.dynamodb


@FunctionalInterface
fun interface ITableNameProvider {
    fun getTableName(): String
}