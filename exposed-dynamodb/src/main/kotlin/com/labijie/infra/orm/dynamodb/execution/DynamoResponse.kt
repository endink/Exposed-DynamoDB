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
    response: TResponse
)

open class DynamoTableBatchGetResponse<TResponse>(
    val tableName: String,
    hasItem: Boolean,
    val items: Map<String, List<Map<String, AttributeValue>>>,
    response: TResponse,
) {

}

open class DynamoTableListResponse<TResponse>(
    val tableName: String,
    hasItem: Boolean,
    val items: List<Map<String, AttributeValue>>,
    response: TResponse
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


