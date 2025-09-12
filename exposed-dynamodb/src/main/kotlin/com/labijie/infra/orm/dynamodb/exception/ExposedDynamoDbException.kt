/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb.exception

import java.lang.RuntimeException


open class ExposedDynamoDbException(
    message: String? = null,
    cause: Throwable? = null
) : RuntimeException(message, cause)


open class DynamoDbTypeMismatchException(
    message: String? = null,
    cause: Throwable? = null
) : ExposedDynamoDbException(message, cause)


open class DynamoDbExpressionFormatException(
    message: String? = null,
    cause: Throwable? = null
) : ExposedDynamoDbException(message, cause) {
    companion object {
        fun sortKeyMissed(tableName: String): DynamoDbExpressionFormatException {
            return DynamoDbExpressionFormatException( "Query operation must include a sort key condition. " +
                    "The table '${tableName}' has a composite primary key (pk + sk), " +
                    "so you must specify both partition key and sort key in the query.")
        }
    }
}