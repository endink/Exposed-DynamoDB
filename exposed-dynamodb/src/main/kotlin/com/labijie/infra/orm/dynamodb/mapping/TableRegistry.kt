/**
 * This file is part of Exposed-DynamoDB project.
 * Copyright (c) 2025
 * @author Anders Xiao
 *
 * File Create: 2025/9/4
 */

package com.labijie.infra.orm.dynamodb.mapping

import com.labijie.infra.orm.dynamodb.DynamoTable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet

object TableRegistry {

    val tables: ConcurrentHashMap<String, MutableList<TableRegistration>> = ConcurrentHashMap()

    fun registryTable(table: DynamoTable) {
        val set = this.tables.getOrPut(table.tableName) { mutableListOf() }
        val table = TableRegistration(table)
        if(!set.contains(table)) {
            set.add(table)
        }
    }

}