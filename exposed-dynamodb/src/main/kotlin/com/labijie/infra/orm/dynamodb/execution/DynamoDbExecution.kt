/**
 * This file is part of Exposed-DynamoDB project.
 * Copyright (c) 2025
 * @author Anders Xiao
 *
 * File Create: 2025/9/5
 */

package com.labijie.infra.orm.dynamodb.execution

import com.labijie.infra.orm.dynamodb.*
import com.labijie.infra.orm.dynamodb.builder.*
import com.labijie.infra.orm.dynamodb.model.DynamoOffsetList
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.*

@JvmInline
value class DynamoDbExecution internal constructor(val client: DynamoDbClient) {

    fun DynamoBatchGetBuilder.exec(customizer: BatchGetRequestCustomizer? = null): DynamoTableBatchGetResponse<BatchGetItemResponse> {

        var request = this.request(customizer)
        val merged = mutableMapOf<String, MutableList<Map<String, AttributeValue>>>()
        var response: BatchGetItemResponse
        do {
            response = client.batchGetItem(request)

            response.responses().forEach {
                if(it.value.isNotEmpty()) {
                    val list = merged.getOrPut(it.key) {
                        mutableListOf()
                    }
                    list.addAll(it.value)
                }
            }

            // 如果还有未处理的 Key，则继续请求
            val requestItems = response.unprocessedKeys();
            if (requestItems.isNotEmpty()) {
                request = this.next(requestItems)
            }
        } while (response.unprocessedKeys().isNotEmpty())

        return DynamoTableBatchGetResponse(this.tableName, merged.isNotEmpty(), merged,  response)
    }

    fun DynamoGetBuilder.exec(customizer: GetItemRequestCustomizer? = null): DynamoTableItemResponse<GetItemResponse> {
        val request = this.request(customizer)
        val result = client.getItem(request)
        return result.forTable(this.tableName)
    }

    fun DynamoQueryBuilder.exec(customizer: QueryRequestCustomizer? = null): DynamoTableForwardListResponse<QueryResponse> {
        val request = this.request(customizer)
        return client.query(request).forTable(this.tableName)
    }

    fun DynamoPutBuilder.exec(
        returnValue: ReturnValue = ReturnValue.NONE,
        customizer: PutRequestCustomizer? = null
    ): DynamoTableItemResponse<PutItemResponse> {
        val request = this.request(returnValue, customizer)
        return client.putItem(request).forTable(this.tableName)
    }

    fun DynamoDeleteBuilder.exec(
        returnValue: ReturnValue = ReturnValue.NONE,
        customizer: DeleteRequestCustomizer? = null
    ): DynamoTableItemResponse<DeleteItemResponse> {
        val request = this.request(returnValue, customizer)
        return client.deleteItem(request).forTable(this.tableName)
    }

    fun DynamoUpdateBuilder.exec(
        returnValue: ReturnValue = ReturnValue.NONE,
        customizer: UpdateRequestCustomizer? = null
    ): DynamoTableItemResponse<UpdateItemResponse> {
        val request = this.request(returnValue, customizer)
        return client.updateItem(request).forTable(this.tableName)
    }


    fun <T : Any> DynamoTableItemResponse<*>.readValue(
        valueFactory: () -> T,
        mapper: ((Map<String, AttributeValue>, T) -> Unit)? = null
    ): T {
        val v = valueFactory.invoke()
        this.item.readValue(tableName, valueFactory, mapper)
        return v
    }

    fun <T : Any> DynamoTableListResponse<*>.readValue(
        valueFactory: () -> T,
        mapper: ((Map<String, AttributeValue>, T) -> Unit)? = null
    ): List<T> {
        val list = this.items.map {
            it.readValue(tableName, valueFactory, mapper)
        }
        return list
    }

    fun <T : Any> DynamoTableForwardListResponse<*>.readValue(
        valueFactory: () -> T,
        mapper: ((Map<String, AttributeValue>, T) -> Unit)? = null
    ): DynamoOffsetList<T> {
        val list = this.items.map {
            it.readValue(tableName, valueFactory, mapper)
        }
        val forwardToken = this.latestKey?.let { LastEvaluatedKeyCodec.encode(it) }

        return DynamoOffsetList(list, forwardToken)
    }

}