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
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals


class TableDateTypeTester {

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

    /** ---------------- Update Number ---------------- */
    @Test
    fun `test int and float operations`() {
        TestTable.put {
            it[TestTable.pk] = "pk2"
            it[TestTable.sk] = "sk2"
            it[TestTable.name] = "numItem"
            it[TestTable.intValue] = 1
            it[TestTable.floatValue] = 2.0f
        }.request().also { client.putItem(it) }

        val updateReq = TestTable.update({
            keys { (TestTable.pk eq "pk2") and (TestTable.sk eq "sk2") }
        }) {
            it[TestTable.intValue] = TestTable.intValue.ifNotExists(0) + 2
            it[TestTable.floatValue] += 3.0f
            it[TestTable.floatValue] = null
            it[TestTable.floatValue] = 4.0f
        }.request()
        client.updateItem(updateReq)

        val getReq = TestTable.get().keys {
            (TestTable.pk eq "pk2") and (TestTable.sk eq "sk2")
        }.request()

        val updated = client.getItem(getReq).item() ?: error("Item not found")
        assertEquals(3, updated[TestTable.intValue.name]?.n()?.toIntOrNull())
        assertEquals(4.0f, updated[TestTable.floatValue.name]?.n()?.toFloatOrNull())
    }

    /** ---------------- StringSet ---------------- */
    @Test
    fun `test stringSet add`() {
        TestTable.put {
            it[TestTable.pk] = "pk_str"
            it[TestTable.sk] = "sk_str"
            it[TestTable.name] = "stringSetItem"
            it[TestTable.stringSet] = setOfString("init")
        }.request().also { client.putItem(it) }

        val getReq = TestTable.get().keys { (TestTable.pk eq "pk_str") and (TestTable.sk eq "sk_str") }.request()
        val initial = client.getItem(getReq).item() ?: error("Item not found")
        assertEquals(setOf("init"), initial[TestTable.stringSet.name]?.ss()?.toSet())

        val addReq = TestTable.update({
            keys { (TestTable.pk eq "pk_str") and (TestTable.sk eq "sk_str") }
        }) {
            it[TestTable.stringSet] += setOfString("a", "b")
        }.request()
        client.updateItem(addReq)

        val updated = client.getItem(getReq).item() ?: error("Item not found after add")
        assertEquals(setOf("init", "a", "b"), updated[TestTable.stringSet.name]?.ss()?.toSet())
    }

    @Test
    fun `test stringSet remove`() {
        TestTable.put {
            it[TestTable.pk] = "pk_str2"
            it[TestTable.sk] = "sk_str2"
            it[TestTable.name] = "stringSetItem2"
            it[TestTable.stringSet] = setOfString("x", "y", "z")
        }.request().also { client.putItem(it) }

        val getReq = TestTable.get().keys { (TestTable.pk eq "pk_str2") and (TestTable.sk eq "sk_str2") }.request()
        val initial = client.getItem(getReq).item() ?: error("Item not found")
        assertEquals(setOf("x", "y", "z"), initial[TestTable.stringSet.name]?.ss()?.toSet())

        val removeReq = TestTable.update({
            keys { (TestTable.pk eq "pk_str2") and (TestTable.sk eq "sk_str2") }
        }) {
            it[TestTable.stringSet] -= setOfString("y", "z")
        }.request()
        client.updateItem(removeReq)

        val updated = client.getItem(getReq).item() ?: error("Item not found after remove")
        assertEquals(setOf("x"), updated[TestTable.stringSet.name]?.ss()?.toSet())
    }

    /** ---------------- NumberSet ---------------- */
    @Test
    fun `test numberSet add`() {
        TestTable.put {
            it[TestTable.pk] = "pk_num"
            it[TestTable.sk] = "sk_num"
            it[TestTable.name] = "numberSetItem"
            it[TestTable.numberSet] = setOfNumber(1, 2)
        }.request().also { client.putItem(it) }

        val getReq = TestTable.get().keys { (TestTable.pk eq "pk_num") and (TestTable.sk eq "sk_num") }.request()
        val initial = client.getItem(getReq).item() ?: error("Item not found")
        val initNumbers = initial[TestTable.numberSet.name]?.ns()?.map { it.toDouble() }?.toSet()
        assertEquals(setOf(1.0, 2.0), initNumbers)

        val addReq = TestTable.update({
            keys { (TestTable.pk eq "pk_num") and (TestTable.sk eq "sk_num") }
        }) {
            it[TestTable.numberSet] += setOfNumber(3, 4.5)
        }.request()
        client.updateItem(addReq)

        val updated = client.getItem(getReq).item() ?: error("Item not found after add")
        val updatedNumbers = updated[TestTable.numberSet.name]?.ns()?.map { it.toDouble() }?.toSet()
        assertEquals(setOf(1.0, 2.0, 3.0, 4.5), updatedNumbers)
    }

    @Test
    fun `test numberSet remove`() {
        TestTable.put {
            it[TestTable.pk] = "pk_num2"
            it[TestTable.sk] = "sk_num2"
            it[TestTable.name] = "numberSetItem2"
            it[TestTable.numberSet] = setOfNumber(1,2,3)
        }.request().also { client.putItem(it) }

        val getReq = TestTable.get().keys { (TestTable.pk eq "pk_num2") and (TestTable.sk eq "sk_num2") }.request()
        val removeReq = TestTable.update({
            keys { (TestTable.pk eq "pk_num2") and (TestTable.sk eq "sk_num2") }
        }) {
            it[TestTable.numberSet] -= setOfNumber(2,3)
        }.request()
        client.updateItem(removeReq)

        val updated = client.getItem(getReq).item() ?: error("Item not found after remove")
        val updatedNumbers = updated[TestTable.numberSet.name]?.ns()?.map { it.toDouble() }?.toSet()
        assertEquals(setOf(1.0), updatedNumbers)
    }

    /** ---------------- BinarySet ---------------- */
    @Test
    fun `test binarySet add`() {
        TestTable.put {
            it[TestTable.pk] = "pk_bin"
            it[TestTable.sk] = "sk_bin"
            it[TestTable.name] = "binarySetItem"
            it[TestTable.binarySet] = setOfBinary("a".toByteArray())
        }.request().also { client.putItem(it) }

        val getReq = TestTable.get().keys { (TestTable.pk eq "pk_bin") and (TestTable.sk eq "sk_bin") }.request()
        val initial = client.getItem(getReq).item() ?: error("Item not found")
        assertEquals(true, initial[TestTable.binarySet.name]?.bs()?.any { it.asByteArray().contentEquals("a".toByteArray()) })

        val addReq = TestTable.update({
            keys { (TestTable.pk eq "pk_bin") and (TestTable.sk eq "sk_bin") }
        }) {
            it[TestTable.binarySet] += setOfBinary("b".toByteArray())
        }.request()
        client.updateItem(addReq)

        val updated = client.getItem(getReq).item() ?: error("Item not found after add")
        val updatedSet = updated[TestTable.binarySet.name]?.bs()?.toSet()
        assertEquals(true, updatedSet?.any { it.asByteArray().contentEquals("a".toByteArray()) })
        assertEquals(true, updatedSet?.any { it.asByteArray().contentEquals("b".toByteArray()) })
    }

    @Test
    fun `test binarySet remove`() {
        TestTable.put {
            it[TestTable.pk] = "pk_bin2"
            it[TestTable.sk] = "sk_bin2"
            it[TestTable.name] = "binarySetItem2"
            it[TestTable.binarySet] = setOfBinary("x".toByteArray(), "y".toByteArray())
        }.request().also { client.putItem(it) }

        val getReq = TestTable.get().keys { (TestTable.pk eq "pk_bin2") and (TestTable.sk eq "sk_bin2") }.request()
        val removeReq = TestTable.update({
            keys { (TestTable.pk eq "pk_bin2") and (TestTable.sk eq "sk_bin2") }
        }) {
            it[TestTable.binarySet] -= setOfBinary("y".toByteArray())
        }.request()
        client.updateItem(removeReq)

        val updated = client.getItem(getReq).item() ?: error("Item not found after remove")
        val updatedSet = updated[TestTable.binarySet.name]?.bs()?.toSet()
        assertEquals(true, updatedSet?.any { it.asByteArray().contentEquals("x".toByteArray()) })
        assertEquals(false, updatedSet?.any { it.asByteArray().contentEquals("y".toByteArray()) })
    }

    /** ---------------- List Operations ---------------- */
    @Test
    fun `test list set`() {
        // 插入基础数据
        TestTable.put {
            it[TestTable.pk] = "pk5"
            it[TestTable.sk] = "sk5"
            it[TestTable.name] = "listItem"
            it[TestTable.listValue] = listOf<String>()
        }.request().also { client.putItem(it) }

        val getReq = TestTable.get().keys { (TestTable.pk eq "pk5") and (TestTable.sk eq "sk5") }.request()

        // Step 1: 设置列表
        val setReq = TestTable.update({
            keys { (TestTable.pk eq "pk5") and (TestTable.sk eq "sk5") }
        }) {
            it[TestTable.listValue] = listOf("aaa", "bbb", "ccc")
        }.request()
        client.updateItem(setReq)

        var updated = client.getItem(getReq).item() ?: error("Item not found after set")
        var list = updated[TestTable.listValue.name]?.l()?.map { it.n() ?: it.s() }
        assertContentEquals(listOf("aaa", "bbb", "ccc"), list)

        //append
        val sliceReq = TestTable.update({
            keys { (TestTable.pk eq "pk5") and (TestTable.sk eq "sk5") }
        }) {
            it[TestTable.listValue] += listOf("aaa", "bbb", "ccc")
        }.request()
        client.updateItem(sliceReq)

        updated = client.getItem(getReq).item() ?: error("Item not found after slice")
        list = updated[TestTable.listValue.name]?.l()?.map { it.n() ?: it.s() }
        assertContentEquals(listOf("aaa", "bbb", "ccc", "aaa", "bbb", "ccc"), list)

        // Step 3: 修改索引 1
        val indexReq = TestTable.update({
            keys { (TestTable.pk eq "pk5") and (TestTable.sk eq "sk5") }
        }) {
            it[TestTable.listValue][1] = 3
        }.request()
        client.updateItem(indexReq)

        updated = client.getItem(getReq).item() ?: error("Item not found after index update")
        list = updated[TestTable.listValue.name]?.l()?.map { it.n() ?: it.s() }
        assertContentEquals(listOf("aaa", 3, "ccc", "aaa", "bbb", "ccc").map { it.toString() }, list)

        // Step 4: 删除索引 0
        val removeReq = TestTable.update({
            keys { (TestTable.pk eq "pk5") and (TestTable.sk eq "sk5") }
        }) {
            it[TestTable.listValue][0].remove()
        }.request()
        client.updateItem(removeReq)

        updated = client.getItem(getReq).item() ?: error("Item not found after remove")
        list = updated[TestTable.listValue.name]?.l()?.map { it.n() ?: it.s() }
        assertContentEquals(listOf(3, "ccc", "aaa", "bbb", "ccc").map { it.toString() }, list)
    }


    /** ---------------- Map Operations ---------------- */
    @Test
    fun `test map set`() {
        TestTable.put {
            it[TestTable.pk] = "pk6"
            it[TestTable.sk] = "sk6"
            it[TestTable.name] = "mapItem"
            it[TestTable.mapValue] = mutableMapOf()
        }.request().also { client.putItem(it) }

        val updateReq = TestTable.update({
            keys { (TestTable.pk eq "pk6") and (TestTable.sk eq "sk6") }
        }) {
            it[TestTable.mapValue] = mapOf(
                "bbb" to "value_bbb",
                "ccc" to "value_ccc"
            )
        }.request()
        client.updateItem(updateReq)

        val getReq = TestTable.get().keys { (TestTable.pk eq "pk6") and (TestTable.sk eq "sk6") }.request()
        val updated = client.getItem(getReq).item() ?: error("Item not found")
        assertEquals("value_bbb", updated[TestTable.mapValue.name]?.m()?.get("bbb")?.s())
        assertEquals("value_ccc", updated[TestTable.mapValue.name]?.m()?.get("ccc")?.s())
    }

    @Test
    fun `test map nested add`() {
        val insertReq = TestTable.put {
            it[TestTable.pk] = "pk6"
            it[TestTable.sk] = "sk6"
            it[TestTable.name] = "mapItem"
            it[TestTable.mapValue] = mutableMapOf(
                "aaa" to mutableMapOf(
                    "a0" to mutableMapOf(
                        "ccc" to "nested_aaa",
                        "bb" to setOfString("123"),
                        "dd" to setOfNumber(1, 6, 7)
                    )
                )
            )
        }.request()
        client.putItem(insertReq)

        val getAfterInsert = TestTable.get().keys { (TestTable.pk eq "pk6") and (TestTable.sk eq "sk6") }.request()
        val insertedItem = client.getItem(getAfterInsert).item() ?: error("Item not found")
        val levelA = insertedItem[TestTable.mapValue.name]?.m()?.get("aaa")?.m() ?: error("mapValue['aaa'] missing")
        val levelA0 = levelA["a0"]?.m() ?: error("mapValue['aaa']['a0'] missing")

        assertEquals("nested_aaa", levelA0["ccc"]?.s())
        assertEquals(setOf("123"), levelA0["bb"]?.ss()?.toSet())
        assertEquals(setOf(1.0, 6.0, 7.0), levelA0["dd"]?.ns()?.map { it.toDouble() }?.toSet())

        val updateReq = TestTable.update({
            keys { (TestTable.pk eq "pk6") and (TestTable.sk eq "sk6") }
        }) {
            it[TestTable.mapValue]["aaa"]["a0"]["bb"] += setOfString("str1", "str2")
            it[TestTable.mapValue]["aaa"]["a0"]["dd"] += setOfNumber(5, 10)
        }.request()
        client.updateItem(updateReq)

        val updated = client.getItem(getAfterInsert).item() ?: error("Item not found after update")
        val updatedLevelA0 = updated[TestTable.mapValue.name]?.m()?.get("aaa")?.m()?.get("a0")?.m() ?: error("mapValue['aaa']['a0'] missing after update")

        assertEquals(setOf("123", "str1", "str2"), updatedLevelA0["bb"]?.ss()?.toSet())
        assertEquals(setOf(1.0, 5.0, 6.0, 7.0, 10.0), updatedLevelA0["dd"]?.ns()?.map { it.toDouble() }?.toSet())
    }

    @Test
    fun `test map nested remove`() {
        val insertReq = TestTable.put {
            it[TestTable.pk] = "pk6"
            it[TestTable.sk] = "sk6"
            it[TestTable.name] = "mapItem"
            it[TestTable.mapValue] = mutableMapOf("t" to "value_t")
        }.request()
        client.putItem(insertReq)

        val getAfterInsert = TestTable.get().keys { (TestTable.pk eq "pk6") and (TestTable.sk eq "sk6") }.request()
        val insertedItem = client.getItem(getAfterInsert).item() ?: error("Item not found")
        assertEquals("value_t", insertedItem[TestTable.mapValue.name]?.m()?.get("t")?.s())

        val updateReq = TestTable.update({
            keys { (TestTable.pk eq "pk6") and (TestTable.sk eq "sk6") }
        }) {
            it[TestTable.mapValue]["t"].remove()
        }.request()
        client.updateItem(updateReq)

        val updated = client.getItem(getAfterInsert).item() ?: error("Item not found after remove")
        assertEquals(false, updated[TestTable.mapValue.name]?.m()?.containsKey("t"))
    }
}

