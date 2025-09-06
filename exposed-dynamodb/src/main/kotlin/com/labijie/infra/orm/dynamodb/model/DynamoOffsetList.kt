/**
 * This file is part of Exposed-DynamoDB project.
 * Copyright (c) 2025
 * @author Anders Xiao
 *
 * File Create: 2025/9/5
 */

package com.labijie.infra.orm.dynamodb.model

class DynamoOffsetList<T>(val list: List<T> = emptyList(), val forwardToken: String? = null) {
    companion object {

        private val EMPTY = DynamoOffsetList<Any>(emptyList(), null)

        fun <T> empty(): DynamoOffsetList<T> {

            @Suppress("UNCHECKED_CAST")
            return EMPTY as DynamoOffsetList<T>
        }
    }
}