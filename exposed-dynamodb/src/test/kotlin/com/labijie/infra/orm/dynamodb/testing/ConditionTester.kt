/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb.testing

import com.labijie.infra.orm.dynamodb.*
import com.labijie.infra.orm.dynamodb.schema.DynamodbSchemaUtils
import com.labijie.infra.orm.dynamodb.testing.TestingUtils.client
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


class ConditionTester {
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

    /** ---------------- Condition Expressions ---------------- */
    @Test
    fun `test conditional update success`() {
        // 插入初始数据
        TestTable.put {
            it[TestTable.pk] = "pk_cond"
            it[TestTable.sk] = "sk_cond"
            it[TestTable.name] = "conditionalItem"
            it[TestTable.intValue] = 5
        }.request().also { client.putItem(it) }

        // 条件更新（条件满足）
        val updateReq = TestTable.update({
            keys { (TestTable.pk eq "pk_cond") and (TestTable.sk eq "sk_cond") }
            condition { TestTable.intValue eq 5 } // 条件：当前值等于5
        }) {
            it[TestTable.intValue] = 10
        }.request()

        // 应该成功执行
        assertDoesNotThrow { client.updateItem(updateReq) }

        // 验证更新结果
        val getReq = TestTable.get().keys {
            (TestTable.pk eq "pk_cond") and (TestTable.sk eq "sk_cond")
        }.request()
        val updated = client.getItem(getReq).item() ?: error("Item not found")
        assertEquals(10, updated[TestTable.intValue.name]?.n()?.toIntOrNull())
    }

    @Test
    fun `test conditional update failure`() {
        // 插入初始数据
        TestTable.put {
            it[TestTable.pk] = "pk_cond2"
            it[TestTable.sk] = "sk_cond2"
            it[TestTable.name] = "conditionalItem2"
            it[TestTable.intValue] = 5
        }.request().also { client.putItem(it) }

        // 条件更新（条件不满足）
        val updateReq = TestTable.update({
            keys { (TestTable.pk eq "pk_cond2") and (TestTable.sk eq "sk_cond2") }
            condition { TestTable.intValue eq 10 } // 条件：当前值等于10（实际是5）
        }) {
            it[TestTable.intValue] = 15
        }.request()

        // 应该抛出条件检查失败的异常
        assertThrows<ConditionalCheckFailedException> { client.updateItem(updateReq) }

        // 验证数据未被修改
        val getReq = TestTable.get().keys {
            (TestTable.pk eq "pk_cond2") and (TestTable.sk eq "sk_cond2")
        }.request()
        val unchanged = client.getItem(getReq).item() ?: error("Item not found")
        assertEquals(5, unchanged[TestTable.intValue.name]?.n()?.toIntOrNull())
    }

    @Test
    fun `test conditional delete success`() {
        // 插入初始数据
        TestTable.put {
            it[TestTable.pk] = "pk_cond3"
            it[TestTable.sk] = "sk_cond3"
            it[TestTable.name] = "conditionalItem3"
            it[TestTable.intValue] = 5
        }.request().also { client.putItem(it) }

        // 条件删除（条件满足）
        val deleteReq = TestTable.delete {
            keys { (TestTable.pk eq "pk_cond3") and (TestTable.sk eq "sk_cond3") }
            condition { TestTable.intValue eq 5 } // 条件：当前值等于5
        }.request()

        // 应该成功执行
        assertDoesNotThrow { client.deleteItem(deleteReq) }

        // 验证项目已被删除
        val getReq = TestTable.get().keys {
            (TestTable.pk eq "pk_cond3") and (TestTable.sk eq "sk_cond3")
        }.request()
        val result = client.getItem(getReq)
        assertEquals(false,result.hasItem())
    }

    @Test
    fun `test conditional delete failure`() {
        // 插入初始数据
        TestTable.put {
            it[TestTable.pk] = "pk_cond4"
            it[TestTable.sk] = "sk_cond4"
            it[TestTable.name] = "conditionalItem4"
            it[TestTable.intValue] = 5
        }.request().also { client.putItem(it) }

        // 条件删除（条件不满足）
        val deleteReq = TestTable.delete {
            keys { (TestTable.pk eq "pk_cond4") and (TestTable.sk eq "sk_cond4") }
            condition { TestTable.intValue eq 10 } // 实际是5，不满足条件
        }.request()

        // 应该抛出条件检查失败异常
        assertThrows<ConditionalCheckFailedException> { client.deleteItem(deleteReq) }

        // 验证数据未被删除
        val getReq = TestTable.get().keys {
            (TestTable.pk eq "pk_cond4") and (TestTable.sk eq "sk_cond4")
        }.request()
        val existing = client.getItem(getReq).item() ?: error("Item not found")
        assertEquals(5, existing[TestTable.intValue.name]?.n()?.toIntOrNull())
    }

    @Test
    fun `test conditional put success`() {
        // 条件插入（条件不满足）
        val put = TestTable.put {
            it[TestTable.pk] = "pk_cond5"
            it[TestTable.sk] = "sk_cond5"
            it[TestTable.name] = "conditionalItem5_new"
            it[TestTable.intValue] = 10
        }.condition {
            TestTable.pk.notExists()
        }.request()

        client.putItem(put)

        // 验证原数据未被覆盖
        val resp = TestTable.get().keys {
            (TestTable.pk eq "pk_cond5") and (TestTable.sk eq "sk_cond5")
        }.request().let { client.getItem(it) }

        assertEquals(true, resp.hasItem())
    }

    @Test
    fun `test conditional put failure`() {

        TestTable.put {
            it[TestTable.pk] = "pk_cond5"
            it[TestTable.sk] = "sk_cond5"
            it[TestTable.name] = "conditionalItem5_old"
            it[TestTable.intValue] = 5
        }.request().let { client.putItem(it) }

        val exist = TestTable.get().keys {
            (TestTable.pk eq "pk_cond5") and (TestTable.sk eq "sk_cond5")
        }.request().let { client.getItem(it) }

        assertEquals(5.toString(), exist.item()[TestTable.intValue.name]?.n())
        assertEquals("conditionalItem5_old", exist.item()[TestTable.name.name]?.s())


        // 条件插入（条件不满足）
        val put = TestTable.put {
            it[TestTable.pk] = "pk_cond5"
            it[TestTable.sk] = "sk_cond5"
            it[TestTable.name] = "conditionalItem5_new"
            it[TestTable.intValue] = 10
        }.condition {
            TestTable.intValue eq 10 // 实际是5，不满足条件
        }.request()

        assertFailsWith<ConditionalCheckFailedException> {
            client.putItem(put)
        }

        // 验证原数据未被覆盖
        val resp = TestTable.get().keys {
            (TestTable.pk eq "pk_cond5") and (TestTable.sk eq "sk_cond5")
        }.request().let { client.getItem(it) }

        assertEquals("conditionalItem5_old", resp.item()[TestTable.name.name]?.s())
        assertEquals(5.toString(), resp.item()[TestTable.intValue.name]?.n())
    }

}