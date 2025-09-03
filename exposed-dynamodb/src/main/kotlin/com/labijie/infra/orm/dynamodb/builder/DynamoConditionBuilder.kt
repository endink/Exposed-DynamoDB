/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */



package com.labijie.infra.orm.dynamodb.builder

import com.labijie.infra.orm.dynamodb.*
import software.amazon.awssdk.services.dynamodb.model.AttributeValue


open class DynamoConditionBuilder :
    IDynamoWriteBuilder {

    data class BuildResult(
        val keys: Map<String, AttributeValue>, val conditionExpression: DynamoExpression<Boolean>?)

    private val keys = mutableMapOf<String, AttributeValue>()
    private var conditionExpression: DynamoExpression<Boolean>? = null


    fun keys(
        keyExpression: IDynamoExactKeyQueryBuilder.() -> DynamoExpression<Boolean>
    ): DynamoConditionBuilder {
        keys.clear()
        val keyExpr = keyExpression.invoke(IDynamoExactKeyQueryBuilder.NULL)
        IDynamoExactKeyQueryBuilder.extractKeys(keyExpr, keys)
        return this
    }

    fun condition(where: IDynamoFilterBuilder.() -> DynamoExpression<Boolean>): DynamoConditionBuilder {
        conditionExpression = where.invoke(IDynamoFilterBuilder.NULL)
        return this
    }

    internal fun buildCondition(): BuildResult {
        return BuildResult(keys, conditionExpression)
    }
}