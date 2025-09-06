/**
 * This file is part of Exposed-DynamoDB project.
 * Copyright (c) 2025
 * @author Anders Xiao
 *
 * File Create: 2025/9/5
 */

package com.labijie.infra.orm.dynamodb.exception

class DuplicateDynamoAttributeException(
    tableName: String,
    columnName: String
) : DynamoException(
    "Duplicate attribute name detected in '${tableName}': attribute '$columnName' already exists."
)