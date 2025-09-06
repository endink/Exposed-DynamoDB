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
import com.labijie.infra.orm.dynamodb.mapping.ReflectionDynamoDbMapper
import com.labijie.infra.orm.dynamodb.model.DynamoOffsetList
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.*

@JvmInline
value class DynamoDbExecution internal constructor(val client: DynamoDbClient) {

    fun <PK, SK> DynamoBatchGetBuilder<PK, SK>.exec(customizer: BatchGetRequestCustomizer? = null): DynamoTableBatchGetResponse<BatchGetItemResponse> {

        var request = this.request(customizer)
        val merged = mutableMapOf<String, MutableList<Map<String, AttributeValue>>>()
        var response: BatchGetItemResponse
        do {
            response = client.batchGetItem(request)

            response.responses().forEach {
                if (it.value.isNotEmpty()) {
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

        return DynamoTableBatchGetResponse(this.tableName, merged.isNotEmpty(), merged, response)
    }

    fun <PK, SK> DynamoGetBuilder<PK, SK>.exec(customizer: GetItemRequestCustomizer? = null): DynamoTableItemResponse<GetItemResponse> {
        val request = this.request(customizer)
        val result = client.getItem(request)
        return result.forTable(this.tableName)
    }

    fun <PK, SK> DynamoQueryBuilder<PK, SK>.exec(customizer: QueryRequestCustomizer? = null): DynamoTableForwardListResponse<QueryResponse> {
        val request = this.request(customizer)
        return client.query(request).forTable(this.tableName)
    }

    fun <PK, SK> DynamoPutBuilder<PK, SK>.exec(
        returnValue: ReturnValue = ReturnValue.NONE,
        customizer: PutRequestCustomizer? = null
    ): DynamoTableItemResponse<PutItemResponse> {
        val request = this.request(returnValue, customizer)
        return client.putItem(request).forTable(this.tableName)
    }

    fun <PK, SK> DynamoDeleteBuilder<PK, SK>.exec(
        returnValue: ReturnValue = ReturnValue.NONE,
        customizer: DeleteRequestCustomizer? = null
    ): DynamoTableItemResponse<DeleteItemResponse> {
        val request = this.request(returnValue, customizer)
        return client.deleteItem(request).forTable(this.tableName)
    }

    fun <PK, SK> DynamoUpdateBuilder<PK, SK>.exec(
        returnValue: ReturnValue = ReturnValue.NONE,
        customizer: UpdateRequestCustomizer? = null
    ): DynamoTableItemResponse<UpdateItemResponse> {
        val request = this.request(returnValue, customizer)
        return client.updateItem(request).forTable(this.tableName)
    }


    inline fun <reified T : Any> DynamoTableBatchGetResponse<*>.readValues(mapper: (Map<String, AttributeValue>) -> T): List<T> {
        return this.items[tableName]?.map {
            mapper(it)
        } ?: emptyList()
    }


    inline fun <reified T : Any> DynamoTableBatchGetResponse<*>.readValues(
        tableName: String, mapper: (Map<String, AttributeValue>) -> T
    ): List<T> {
        return this.items[tableName]?.map {
            mapper(it)
        } ?: emptyList()
    }

    inline fun <reified T : Any> DynamoTableItemResponse<*>.readValue(mapper: (Map<String, AttributeValue>) -> T): T {
        return mapper(this.item)
    }

    inline fun <reified T : Any> DynamoTableListResponse<*>.readValue(mapper: (Map<String, AttributeValue>) -> T): List<T> {
        return this.items.map {
            mapper(it)
        }
    }

    inline fun <reified T : Any> DynamoTableForwardListResponse<*>.readValue(mapper: (Map<String, AttributeValue>) -> T): DynamoOffsetList<T> {
        val list = this.items.map {
            mapper(it)
        }
        return DynamoOffsetList(list, this.latestKey?.let { LastEvaluatedKeyCodec.encode(it) })
    }

    /************************************ Reflection Read Value *********************************************/

    private fun <T : Any> create(clazz: Class<T>, table: String, attributeValues: Map<String, AttributeValue>): T {
        return clazz.getDeclaredConstructor().newInstance().apply {
            ReflectionDynamoDbMapper.populateFromDb(table, this, attributeValues)
        }
    }

    fun <T : Any> DynamoTableBatchGetResponse<*>.readValues(itemClass: Class<T>): List<T> {
        return this.items[tableName]?.map {
            create(itemClass, this.tableName, it)
        } ?: emptyList()
    }


    fun <T : Any> DynamoTableBatchGetResponse<*>.readValues(
        tableName: String, itemClass: Class<T>
    ): List<T> {
        return this.items[tableName]?.map {
            create(itemClass, tableName, it)
        } ?: emptyList()
    }

    fun <T : Any> DynamoTableItemResponse<*>.readValue(itemClass: Class<T>): T {
        return create(itemClass, this.tableName, this.item)
    }

    fun <T : Any> DynamoTableListResponse<*>.readValue(itemClass: Class<T>): List<T> {
        return this.items.map {
            create(itemClass, tableName, it)
        }
    }

    fun <T : Any> DynamoTableForwardListResponse<*>.readValue(itemClass: Class<T>): DynamoOffsetList<T> {
        val list = this.items.map {
            create(itemClass, tableName, it)
        }
        return DynamoOffsetList(list, this.latestKey?.let { LastEvaluatedKeyCodec.encode(it) })
    }

}