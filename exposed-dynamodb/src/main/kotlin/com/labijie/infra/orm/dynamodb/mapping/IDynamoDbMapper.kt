/**
 * This file is part of Exposed-DynamoDB project.
 * Copyright (c) 2025
 * @author Anders Xiao
 *
 * File Create: 2025/9/4
 */

package com.labijie.infra.orm.dynamodb.mapping

import software.amazon.awssdk.services.dynamodb.model.AttributeValue


interface IDynamoDbMapper {
    fun <T: Any> populateFromDb(tableName: String, value: T, attributes: Map<String, AttributeValue>)

    fun <T: Any> extractToDb(tableName: String, value: T): Map<String, AttributeValue>
}