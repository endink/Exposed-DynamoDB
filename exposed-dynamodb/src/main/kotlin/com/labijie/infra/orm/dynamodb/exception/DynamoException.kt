/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb.exception

import java.lang.RuntimeException


open class DynamoException(
    message: String? = null,
    cause: Throwable? = null
) : RuntimeException(message, cause)


open class DynamodbTypeMismatchException(
    message: String? = null,
    cause: Throwable? = null
) : DynamoException(message, cause)


open class DynamodbExpressionFormatException(
    message: String? = null,
    cause: Throwable? = null
) : DynamoException(message, cause)