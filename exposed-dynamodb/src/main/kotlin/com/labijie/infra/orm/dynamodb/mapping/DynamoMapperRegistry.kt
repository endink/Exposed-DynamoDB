/**
 * This file is part of Exposed-DynamoDB project.
 * Copyright (c) 2025
 * @author Anders Xiao
 *
 * File Create: 2025/9/5
 */

package com.labijie.infra.orm.dynamodb.mapping

import com.labijie.infra.orm.dynamodb.DynamoTable
import java.util.concurrent.ConcurrentSkipListSet

object DynamoMapperRegistry {

    val mappers: MutableSet<IDynamoDbMapper> = mutableSetOf()

    fun registerMapper(table: DynamoTable<*, *>) {}
}