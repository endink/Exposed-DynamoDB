/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb.testing

import com.labijie.infra.orm.dynamodb.schema.DynamodbSchemaUtils
import com.labijie.infra.orm.dynamodb.testing.TestingUtils.client
import org.junit.jupiter.api.BeforeAll
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import kotlin.test.BeforeTest


abstract class TesterBase {
    @BeforeTest
    fun resetTable() {
        DynamodbSchemaUtils.clearTable(client, TestTable)
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun createTable() {
            DynamodbSchemaUtils.createTableIfNotExist(client, TestTable)
        }
    }

    protected val client: DynamoDbClient by lazy { TestingUtils.client }
}