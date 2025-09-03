/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb.exception


class DuplicateDynamoIndexException(
    val indexName: String,
    val columnName: String
) : RuntimeException(
    "Duplicate index detected: index '$indexName' already exists for column '$columnName'."
)