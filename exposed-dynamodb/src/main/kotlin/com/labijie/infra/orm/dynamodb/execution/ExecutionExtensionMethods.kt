/**
 * This file is part of Exposed-DynamoDB project.
 * Copyright (c) 2025
 * @author Anders Xiao
 *
 * File Create: 2025/9/5
 */

package com.labijie.infra.orm.dynamodb.execution

import software.amazon.awssdk.services.dynamodb.DynamoDbClient

fun <R> DynamoDbClient.execute(exe: DynamoDbExecution.() -> R): R {
    val context = DynamoDbExecution(this)
    return exe.invoke(context)
}


fun <R> DynamoDbClient.executeTransaction(exe: DynamoDbExecution.() -> R): R {
    val context = DynamoDbExecution(this)
    return exe.invoke(context)
}