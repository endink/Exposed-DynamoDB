/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb.testing

import com.labijie.infra.orm.dynamodb.delete
import com.labijie.infra.orm.dynamodb.get
import com.labijie.infra.orm.dynamodb.put
import com.labijie.infra.orm.dynamodb.request
import com.labijie.infra.orm.dynamodb.schema.DynamodbSchemaUtils
import com.labijie.infra.orm.dynamodb.testing.TestingUtils.client
import org.junit.jupiter.api.BeforeAll
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class DeleteTester {
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

    /** ---------------- Delete Operations ---------------- */
    @Test
    fun `test delete existed item`() {
        // 插入项目
        TestTable.put {
            it[TestTable.pk] = "pk_delete"
            it[TestTable.sk] = "sk_delete"
            it[TestTable.name] = "deleteItem"
            it[TestTable.intValue] = 42
        }.request().also { client.putItem(it) }

        // 验证项目存在
        val getReq = TestTable.get().keys {
            (TestTable.pk eq "pk_delete") and (TestTable.sk eq "sk_delete")
        }.request()

        assertNotNull(client.getItem(getReq).item())

        // 删除项目
        val deleteReq = TestTable.delete({
            keys { (TestTable.pk eq "pk_delete") and (TestTable.sk eq "sk_delete") }
        }).request()

        client.deleteItem(deleteReq)

        // 验证项目已被删除
        val result = client.getItem(getReq)

        assertEquals(false, result.hasItem())
    }

    @Test
    fun `test delete not existed item`() {

        // 验证项目存在
        val getReq = TestTable.get().keys {
            (TestTable.pk eq "pk_delete") and (TestTable.sk eq "sk_delete")
        }.request()

        assertEquals(false, client.getItem(getReq).hasItem())

        // 删除项目
        val deleteReq = TestTable.delete({
            keys { (TestTable.pk eq "pk_delete") and (TestTable.sk eq "sk_delete") }
        }).request()

        client.deleteItem(deleteReq)
    }
}