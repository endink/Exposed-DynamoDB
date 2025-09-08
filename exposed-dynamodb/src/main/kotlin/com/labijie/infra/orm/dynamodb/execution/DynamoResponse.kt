/**
 * This file is part of Exposed-DynamoDB project.
 * Copyright (c) 2025
 * @author Anders Xiao
 *
 * File Create: 2025/9/5
 */

package com.labijie.infra.orm.dynamodb.execution

import software.amazon.awssdk.services.dynamodb.model.AttributeValue

class DynamoTableItemResponse<TResponse>(
    val tableName: String,
    hasItem: Boolean,
    val item: Map<String, AttributeValue>,
    val response: TResponse
)

open class DynamoTableBatchGetResponse<TResponse>(
    val tableName: String,
    hasItem: Boolean,
    val items: Map<String, List<Map<String, AttributeValue>>>,
    val response: TResponse,
) {

}

open class DynamoTableListResponse<TResponse>(
    val tableName: String,
    hasItem: Boolean,
    val items: List<Map<String, AttributeValue>>,
    val response: TResponse
) {
}


class DynamoTableForwardListResponse<TResponse>(
    tableName: String,
    hasItem: Boolean,
    items: List<Map<String, AttributeValue>>,
    response: TResponse,
    val latestKey: Map<String, AttributeValue>? = null
) : DynamoTableListResponse<TResponse>(tableName, hasItem, items, response) {
}


fun <T: DynamoTableListResponse<TResponse>, TResponse> T.filter(predicate: (Map<String, AttributeValue>) -> Boolean): DynamoTableListResponse<TResponse> {
    val filtered = this.items.filter(predicate)
    return DynamoTableListResponse(this.tableName, filtered.isNotEmpty(), filtered, this.response)
}

fun <T: DynamoTableListResponse<TResponse>, TResponse> T.find(predicate: (Map<String, AttributeValue>) -> Boolean): DynamoTableItemResponse<TResponse> {
    val filtered = this.items.find(predicate) ?: mapOf()
    return DynamoTableItemResponse(this.tableName, filtered.isNotEmpty(), filtered, this.response)
}

fun <T: DynamoTableListResponse<TResponse>, TResponse> T.firstOrEmpty(predicate: (Map<String, AttributeValue>) -> Boolean): DynamoTableItemResponse<TResponse> {
    val filtered = this.items.firstOrNull() ?: emptyMap()
    return DynamoTableItemResponse(this.tableName, filtered.isNotEmpty(), filtered, this.response)
}

fun <T: DynamoTableListResponse<TResponse>, TResponse> T.firstOrNull(predicate: (Map<String, AttributeValue>) -> Boolean): DynamoTableItemResponse<TResponse>? {
    val filtered = this.items.firstOrNull()
    return filtered?.let {
        DynamoTableItemResponse(this.tableName, filtered.isNotEmpty(), filtered, this.response)
    }
}

