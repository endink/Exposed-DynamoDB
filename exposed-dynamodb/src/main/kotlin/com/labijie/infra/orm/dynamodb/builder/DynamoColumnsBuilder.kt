package com.labijie.infra.orm.dynamodb.builder

import com.labijie.infra.orm.dynamodb.DynamoColumn
import com.labijie.infra.orm.dynamodb.DynamoTable
import com.labijie.infra.orm.dynamodb.IDynamoProjection

/**
 * This file is part of Exposed-DynamoDB project.
 * Copyright (c) 2025
 * @author Anders Xiao
 *
 * File Create: 2025/9/8
 */
class DynamoColumnsBuilder<PK, SK>(
    private val table: DynamoTable<PK, SK>,
    private val exclude: Iterable<DynamoColumn<*>>
) : IDynamoProjection {
    internal fun build(): List<DynamoColumn<*>> {
        return table.columns.filter { !exclude.contains(it) }
    }
}