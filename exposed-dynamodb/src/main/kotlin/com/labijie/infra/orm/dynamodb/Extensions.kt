/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb

import com.labijie.infra.orm.dynamodb.exception.DynamodbTypeMismatchException
import software.amazon.awssdk.services.dynamodb.model.*


internal inline fun <T> T.ifNullOrBlankInput(value: String?, block: T.(String) -> T): T {
    if (!value.isNullOrBlank()) {
        return block.invoke(this, value)
    }
    return this
}


internal inline fun <T, TKey, TValue> T.ifNotNullOrEmpty(
    value: Map<TKey, TValue>?,
    block: T.(Map<TKey, TValue>) -> T
): T {
    if (!value.isNullOrEmpty()) {
        return block.invoke(this, value)
    }
    return this
}

internal inline fun <T, TInput> T.ifNotNull(value: TInput?, block: T.(TInput) -> T): T {
    if (value != null) {
        return block.invoke(this, value)
    }
    return this
}

private fun DynamoColumn<*>.checkColumnValue(value: Any) {
    val tableInfo = "Table '${this.tableName}', Column '${this.name}'"
    tableInfo.let { }

    when (this.dynamoDbType()) {
        "S" -> if (value !is String) throw DynamodbTypeMismatchException("$tableInfo expects String, got ${value.javaClass.simpleName}")
        "N" -> if (value !is Number) throw DynamodbTypeMismatchException("$tableInfo expects Number, got ${value.javaClass.simpleName}")
        "BOOL" -> if (value !is Boolean) throw DynamodbTypeMismatchException("$tableInfo expects Boolean, got ${value.javaClass.simpleName}")
        "B" -> if (value !is ByteArray) throw DynamodbTypeMismatchException("$tableInfo expects ByteArray, got ${value.javaClass.simpleName}")
        "SS" -> if (value !is DynamoSet<*> || (value.isNotEmpty() && value.first() !is String)) throw DynamodbTypeMismatchException(
            "$tableInfo expects DynamoSet<String>"
        )

        "NS" -> if (value !is DynamoSet<*> || (value.isNotEmpty() && value.first() !is Number)) throw DynamodbTypeMismatchException(
            "$tableInfo expects DynamoSet<Number>"
        )

        "BS" -> if (value !is DynamoSet<*> || (value.isNotEmpty() && value.first() !is ByteArray)) throw DynamodbTypeMismatchException(
            "$tableInfo expects DynamoSet<ByteArray>"
        )

        "L" -> if (value !is List<*>) throw DynamodbTypeMismatchException("$tableInfo expects List")
        "M" -> if (value !is Map<*, *>) throw DynamodbTypeMismatchException("$tableInfo expects Map")
        else -> throw DynamodbTypeMismatchException("Unsupported column type '${this.dynamoDbType()}' for $tableInfo")
    }
}


internal fun DynamoColumn<*>.toDbValue(value: Any?): AttributeValue {
    val av = if (value == null) {
        AttributeValue.builder().nul(true).build()
    } else {
        this.checkColumnValue(value)
        @Suppress("UNCHECKED_CAST")
        (this as DynamoColumn<Any>).notNullValueToDB(value)
    }
    return av
}

fun Delete.request(
    returnValue: ReturnValue = ReturnValue.NONE,
    customizer: (DeleteItemRequest.Builder.() -> DeleteItemRequest.Builder)? = null
): DeleteItemRequest {

    val delete = this

    return DeleteItemRequest.builder()
        .tableName(this.tableName())
        .key(this.key())
        .ifNullOrBlankInput(this.conditionExpression()) { conditionExpression(it) }
        .ifNotNullOrEmpty(delete.expressionAttributeValues()) { expressionAttributeValues(it) }
        .ifNotNullOrEmpty(delete.expressionAttributeNames()) { expressionAttributeNames(it) }
        .returnValuesOnConditionCheckFailure(this.returnValuesOnConditionCheckFailure())
        .returnValues(returnValue)
        .ifNotNull(customizer) { it.invoke(this) }
        .build()
}

fun Update.request(
    returnValue: ReturnValue = ReturnValue.NONE,
    customizer: (UpdateItemRequest.Builder.() -> UpdateItemRequest.Builder)? = null
): UpdateItemRequest {
    val update = this
    return UpdateItemRequest.builder()
        .tableName(this.tableName())
        .key(this.key()) // 假设 Update 持有 table 对象，并提供主键映射
        .ifNullOrBlankInput(this.conditionExpression()) { conditionExpression(it) }
        .ifNullOrBlankInput(update.updateExpression()) { updateExpression(it) }
        .ifNotNullOrEmpty(update.expressionAttributeValues()) { expressionAttributeValues(it) }
        .ifNotNullOrEmpty(update.expressionAttributeNames()) { expressionAttributeNames(it) }
        .returnValuesOnConditionCheckFailure(this.returnValuesOnConditionCheckFailure())
        .returnValues(returnValue)
        .ifNotNull(customizer) { it.invoke(this) }
        .build()
}

fun Put.request(
    returnValue: ReturnValue = ReturnValue.NONE,
    customizer: (PutItemRequest.Builder.() -> PutItemRequest.Builder)? = null
): PutItemRequest {

    val put = this
    return PutItemRequest.builder()
        .tableName(this.tableName())
        .item(this.item())
        .ifNullOrBlankInput(this.conditionExpression()) { conditionExpression(it) }
        .ifNotNullOrEmpty(put.expressionAttributeValues()) { expressionAttributeValues(it) }
        .ifNotNullOrEmpty(put.expressionAttributeNames()) { expressionAttributeNames(it) }
        .returnValuesOnConditionCheckFailure(this.returnValuesOnConditionCheckFailure())
        .returnValues(returnValue)
        .ifNotNull(customizer) { it.invoke(this) }
        .build()
}