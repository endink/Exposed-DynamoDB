/**
 * This file is part of Exposed-DynamoDB project.
 * Copyright (c) 2025
 * @author Anders Xiao
 *
 * File Create: 2025/9/12
 */

package com.labijie.infra.orm.dynamodb.execution

interface IDynamoExceptionTranslator {
    fun translate(e: Throwable) : RuntimeException
}