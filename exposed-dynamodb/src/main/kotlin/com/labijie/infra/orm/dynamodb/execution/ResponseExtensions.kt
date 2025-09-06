/**
 * This file is part of Exposed-DynamoDB project.
 * Copyright (c) 2025
 * @author Anders Xiao
 *
 * File Create: 2025/9/5
 */

package com.labijie.infra.orm.dynamodb.execution

import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse
import software.amazon.awssdk.services.dynamodb.model.QueryResponse
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse



fun GetItemResponse.forTable(tableName: String): DynamoTableItemResponse<GetItemResponse> {
    return DynamoTableItemResponse(tableName, this.hasItem(), this.item(), this)
}

fun QueryResponse.forTable(tableName: String): DynamoTableForwardListResponse<QueryResponse> {
    val latest = if(this.hasLastEvaluatedKey()) {
        this.lastEvaluatedKey()
    }else null
    return DynamoTableForwardListResponse(tableName, this.hasItems(), this.items(), this, latest)
}

fun PutItemResponse.forTable(tableName: String): DynamoTableItemResponse<PutItemResponse> {
    return DynamoTableItemResponse(tableName, this.hasAttributes(), this.attributes(), this)
}

fun DeleteItemResponse.forTable(tableName: String): DynamoTableItemResponse<DeleteItemResponse> {
    return DynamoTableItemResponse(tableName, this.hasAttributes(), this.attributes(), this)
}

fun UpdateItemResponse.forTable(tableName: String): DynamoTableItemResponse<UpdateItemResponse> {
    return DynamoTableItemResponse(tableName, this.hasAttributes(), this.attributes(), this)
}