/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb

import com.labijie.infra.orm.dynamodb.builder.DynamoPutBuilder


class DynamoUpdateSetter(private val builder: DynamoPutBuilder) {

    // support it[column] = value
    operator fun <TValue> set(column: DynamoColumn<TValue>, value: TValue?) {
        builder.values[column] = value
    }
}