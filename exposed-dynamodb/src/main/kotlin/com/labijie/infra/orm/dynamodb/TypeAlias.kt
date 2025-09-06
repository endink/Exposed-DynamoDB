/**
 * This file is part of Exposed-DynamoDB project.
 * Copyright (c) 2025
 * @author Anders Xiao
 *
 * File Create: 2025/9/5
 */

package com.labijie.infra.orm.dynamodb

import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest

typealias BatchGetRequestCustomizer = (BatchGetItemRequest.Builder)-> Unit
typealias GetItemRequestCustomizer = GetItemRequest.Builder.()-> Unit
typealias QueryRequestCustomizer = QueryRequest.Builder.()-> Unit
typealias PutRequestCustomizer = (PutItemRequest.Builder)-> Unit
typealias UpdateRequestCustomizer = (UpdateItemRequest.Builder)-> Unit
typealias DeleteRequestCustomizer = (DeleteItemRequest.Builder)-> Unit