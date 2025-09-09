/**
 * This file is part of Exposed-DynamoDB project.
 * Copyright (c) 2025
 * @author Anders Xiao
 *
 * File Create: 2025/9/5
 */

package com.labijie.infra.orm.dynamodb.model

class DynamoOffsetList<out T> {
    var list: List<@UnsafeVariance T> = emptyList()
    var forwardToken: String? = null

    constructor(list: List<T>, forwardToken: String? = null) {
        this.list = list
        this.forwardToken = forwardToken
    }

    constructor()
}

private val EMPTY_LIST = DynamoOffsetList<Nothing>()

fun <T> emptyDynamoOffsetList(): DynamoOffsetList<T> {
    return EMPTY_LIST
}

fun <T, R> DynamoOffsetList<T>.map(transform: (T) -> R): DynamoOffsetList<R> {
    return DynamoOffsetList(this.list.map(transform), this.forwardToken)
}