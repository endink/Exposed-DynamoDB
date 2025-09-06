/**
 * This file is part of Exposed-DynamoDB project.
 * Copyright (c) 2025
 * @author Anders Xiao
 *
 * File Create: 2025/9/5
 */

package com.labijie.infra.orm.dynamodb.exception

class InvalidDynamoForwardTokenException(
    message: String? = "Invalid forward token for DynamoDB.",
    cause: Throwable? = null
) : DynamoException(message, cause)