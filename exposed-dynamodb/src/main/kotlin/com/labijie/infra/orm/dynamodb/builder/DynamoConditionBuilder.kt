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


open class DynamoConditionBuilder<PK, SK> :
    IDynamoWriteBuilder {

    data class BuildResult(
        val keys: Map<String, AttributeValue>, val conditionExpression: DynamoExpression<Boolean>?)

    private val keys = mutableMapOf<String, AttributeValue>()
    private var conditionExpression: DynamoExpression<Boolean>? = null


    fun keys(
        keyExpression: IDynamoExactKeyQueryBuilder<PK, SK>.() -> DynamoExpression<Boolean>
    ): DynamoConditionBuilder<PK, SK> {
        keys.clear()
        val keyExpr = keyExpression.invoke(IDynamoExactKeyQueryBuilder.default())
        IDynamoExactKeyQueryBuilder.extractKeys(keyExpr, keys)
        return this
    }

    fun condition(where: IDynamoFilterBuilder<PK, SK>.() -> DynamoExpression<Boolean>): DynamoConditionBuilder<PK, SK> {
        conditionExpression = where.invoke(IDynamoFilterBuilder.default())
        return this
    }

    internal fun buildCondition(): BuildResult {
        return BuildResult(keys, conditionExpression)
    }
}