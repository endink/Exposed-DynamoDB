/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb

import software.amazon.awssdk.services.dynamodb.model.ProjectionType


class DynamoIndex(
    val indexName: String,
    val column: DynamoColumn<*>,
    val projection: ProjectionType = ProjectionType.ALL,
    vararg columns: DynamoColumn<*>) {

    val projectedColumns = columns.map { it.name }.toSet()
}