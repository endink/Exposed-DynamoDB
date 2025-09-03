/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb

import com.labijie.infra.orm.dynamodb.exception.DynamoNameFormatException
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.Put
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import software.amazon.awssdk.services.dynamodb.model.Update
import software.amazon.awssdk.utils.BinaryUtils
import java.math.BigDecimal


object DynamodbUtils {

    val NUMBER_MAX_POSITIVE = BigDecimal("9.9999999999999999999999999999999999999E125") // 最大
    val NUMBER_MIN_POSITIVE = BigDecimal("1E-130")
    val NUMBER_MAX_NEGATIVE = NUMBER_MAX_POSITIVE.negate()
    val NUMBER_MIN_NEGATIVE = NUMBER_MIN_POSITIVE.negate()


    fun checkDynamoName(name: String, minLength: Int = 1) {
        // Length must be between 3 and 255
        if (name.length !in minLength..255) throw DynamoNameFormatException(name)

        // Must match allowed characters: a-z, A-Z, 0-9, _, -, .
        val regex = Regex("^[a-zA-Z0-9_.-]+$")
        if (!regex.matches(name)) {
            throw DynamoNameFormatException(name)
        }
    }

    private fun StringBuilder.add(fieldName: String, field: Any?, line: Boolean = true): StringBuilder {
        if (field != null) {
            val value: String?

            if (field.javaClass.isArray) {
                val va = if (field is ByteArray) {
                    "0x" + BinaryUtils.toHex(field)
                } else {
                    (field as Array<*>).contentToString()
                }
                append("${fieldName}=${va}")
            } else {
//                when (field) {
//                    is AttributeValue -> {
//                        append("${fieldName}=")
//                        add("S", field.s())
//                        .add("N", field.n())
//                        .add("B", field.b())
//                        .add("SS", if (field.hasSs()) field.ss() else null)
//                        .add("NS", if (field.hasNs()) field.ns() else null)
//                        .add("BS", if (field.hasBs()) field.bs() else null)
//                        .add("M", if (field.hasM()) field.m() else null)
//                        .add("L", if (field.hasL()) field.l() else null)
//                        .add("BOOL", field.bool())
//                        .add("NUL", field.nul())
//                    }
//
//                    else -> append("${fieldName}=$field")
//                }
                append("${fieldName}=$field")
            }
            if (line) {
                append("\n")
            }
        }
        return this
    }


    fun Update.prettyString(): String {
        return StringBuilder("Dynamodb Update:")
            .appendLine()
            .add("Key", if (hasKey()) key() else null)
            .add("UpdateExpression", updateExpression())
            .add("TableName", tableName()).add("ConditionExpression", conditionExpression())
            .add("ExpressionAttributeNames", if (hasExpressionAttributeNames()) expressionAttributeNames() else null)
            .add("ExpressionAttributeValues", if (hasExpressionAttributeValues()) expressionAttributeValues() else null)
            .add("ReturnValuesOnConditionCheckFailure", returnValuesOnConditionCheckFailureAsString())
            .toString()
    }

    fun Put.prettyString(): String {
        return StringBuilder("Dynamodb Put:")
            .appendLine()
            .add("Item", if (hasItem()) item() else null)
            .add("TableName", tableName())
            .add("ConditionExpression", conditionExpression())
            .add("ExpressionAttributeNames", if (hasExpressionAttributeNames()) expressionAttributeNames() else null)
            .add("ExpressionAttributeValues", if (hasExpressionAttributeValues()) expressionAttributeValues() else null)
            .add("ReturnValuesOnConditionCheckFailure", returnValuesOnConditionCheckFailureAsString())
            .toString()
    }

    fun QueryRequest.prettyString(): String {
        return StringBuilder("Dynamodb Query:")
            .appendLine()
            .add("TableName", tableName())
            .add("IndexName", indexName())
            .add("Select", selectAsString())
            .add("AttributesToGet", if (hasAttributesToGet()) attributesToGet() else null)
            .add("Limit", limit()).add("ConsistentRead", consistentRead())
            .add("KeyConditions", if (hasKeyConditions()) keyConditions() else null)
            .add("QueryFilter", if (hasQueryFilter()) queryFilter() else null)
            .add("ConditionalOperator", conditionalOperatorAsString())
            .add("ScanIndexForward", scanIndexForward())
            .add("ExclusiveStartKey", if (hasExclusiveStartKey()) exclusiveStartKey() else null)
            .add("ReturnConsumedCapacity", returnConsumedCapacityAsString())
            .add("ProjectionExpression", projectionExpression())
            .add("FilterExpression", filterExpression())
            .add("KeyConditionExpression", keyConditionExpression())
            .add("ExpressionAttributeNames", if (hasExpressionAttributeNames()) expressionAttributeNames() else null)
            .add("ExpressionAttributeValues", if (hasExpressionAttributeValues()) expressionAttributeValues() else null)
            .toString()
    }
}