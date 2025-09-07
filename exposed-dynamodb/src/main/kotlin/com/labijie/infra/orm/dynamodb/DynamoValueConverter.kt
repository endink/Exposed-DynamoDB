/**
 * This file is part of Exposed-DynamoDB project.
 * Copyright (c) 2025
 * @author Anders Xiao
 *
 * File Create: 2025/9/7
 */

package com.labijie.infra.orm.dynamodb

import software.amazon.awssdk.services.dynamodb.model.AttributeValue

abstract class DynamoValueConverter<TValue> {
    abstract fun canConvert(value: Any): Boolean

    abstract fun valueFromDb(dbValue: AttributeValue): TValue
    abstract fun notNullValueToDB(value: Any): AttributeValue
}