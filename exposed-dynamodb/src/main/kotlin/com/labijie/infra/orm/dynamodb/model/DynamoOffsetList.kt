/**
 * This file is part of Exposed-DynamoDB project.
 * Copyright (c) 2025
 * @author Anders Xiao
 *
 * File Create: 2025/9/5
 */

package com.labijie.infra.orm.dynamodb.model

internal object EmptyDynamoOffsetList : DynamoOffsetList<Nothing>()

open class DynamoOffsetList<out T>(val list: List<T> = emptyList(), val forwardToken: String? = null)

fun <T> emptyDynamoOffsetList(): DynamoOffsetList<T> {
    return EmptyDynamoOffsetList
}

fun <T, R> DynamoOffsetList<T>.map(transform: (T) -> R): DynamoOffsetList<R> {
    return DynamoOffsetList(this.list.map(transform), this.forwardToken)
}