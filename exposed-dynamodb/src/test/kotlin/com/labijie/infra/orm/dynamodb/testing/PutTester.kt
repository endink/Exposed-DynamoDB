/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb.testing

import com.labijie.infra.orm.dynamodb.*
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.assertThrows
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException
import kotlin.test.*


class PutTester : TesterBase() {

    @Test
    fun `test comprehensive nested map list set with all types`() {
        val bin1 = "binary1".toByteArray()
        val bin2 = "binary2".toByteArray()

        val innerStringSet = setOfString("inner_s1", "inner_s2")
        val innerNumberSet = setOfNumber(1, 2)
        val innerBinarySet = setOfBinary(bin1, bin2)

        val complexMap = mapOf(
            "map_string" to "value1",
            "map_number" to 123.45,
            "map_bool" to true,
            "map_list" to listOf(
                "list_str",
                42,
                3.14,
                true,
                mapOf(
                    "nested_map_str" to "nested",
                    "nested_map_num" to 999,
                    "nested_map_bool" to false,
                    "nested_map_list" to listOf("a", "b", 1, 2.5),
                    "nested_map_set" to innerStringSet,
                    "nested_map_binary_set" to innerBinarySet
                ),
                setOfNumber(10, 20, 30)
            ),
            "map_set" to innerNumberSet,
            "map_nested_map" to mapOf(
                "deep_str" to "deep",
                "deep_list" to listOf(1, 2, 3),
                "deep_set" to innerStringSet
            )
        )

        val complexList = listOf(
            "list_string",
            123,
            4.56f,
            7.89,
            true,
            mapOf(
                "list_map_str" to "lmap",
                "list_map_num" to 999.99,
                "list_map_bool" to false,
                "list_map_list" to listOf("x", "y"),
                "list_map_set" to innerNumberSet,
                "list_map_binary_set" to innerBinarySet
            ),
            listOf(
                "nested_list_string",
                mapOf(
                    "nested_list_map_str" to "nlmap",
                    "nested_list_map_list" to listOf(1, 2),
                    "nested_list_map_set" to innerStringSet
                ),
                setOfBinary(bin1, bin2)
            ),
            innerStringSet
        )

        val mainSet = setOfString("main_s1", "main_s2")

        // 插入
        TestTable.put {
            it[TestTable.pk] = "full_nested_test"
            it[TestTable.sk] = "all_types"

            it[TestTable.listValue] = complexList
            it[TestTable.mapValue] = complexMap
            it[TestTable.stringSet] = mainSet
            it[TestTable.numberSet] = setOfNumber(1, 2.5, 3.14)
            it[TestTable.binarySet] = innerBinarySet
        }.request().also { client.putItem(it) }

        // 查询
        val item = client.getItem(
            TestTable.get().keys {
                (TestTable.pk eq "full_nested_test") and (TestTable.sk eq "all_types")
            }.request()
        ).item() ?: fail("Item not found")

        // 顶层 set 断言
        val topStringSet = item[TestTable.stringSet.name]?.ss()
        assertContentEquals(mainSet.toList(), topStringSet)

        val topNumberSet = item[TestTable.numberSet.name]?.ns()
        assertContentEquals(listOf("1", "2.5", "3.14"), topNumberSet)

        val topBinarySet = item[TestTable.binarySet.name]?.bs()
        assertEquals(2, topBinarySet?.size)
        assertTrue(topBinarySet?.any { it.asByteArray().contentEquals(bin1) } == true)
        assertTrue(topBinarySet.any { it.asByteArray().contentEquals(bin2) })

        // Map 嵌套断言
        val mapValue = item[TestTable.mapValue.name]?.m() ?: fail("map missing")

        // 基本字段
        assertEquals("value1", mapValue["map_string"]?.s())
        assertEquals("123.45", mapValue["map_number"]?.n())
        assertEquals(true, mapValue["map_bool"]?.bool())

        // map 内嵌 list
        val mapList = mapValue["map_list"]?.l() ?: fail("map_list missing")
        assertEquals("list_str", mapList[0].s())
        assertEquals("42", mapList[1].n())
        assertEquals("3.14", mapList[2].n())
        assertEquals(true, mapList[3].bool())

        val nestedMapInList = mapList[4].m()
        assertEquals("nested", nestedMapInList["nested_map_str"]?.s())
        assertEquals("999", nestedMapInList["nested_map_num"]?.n())
        assertEquals(false, nestedMapInList["nested_map_bool"]?.bool())

        val nestedList = nestedMapInList["nested_map_list"]?.l()?.map {
            it.n() ?: it.s()
        }
        assertContentEquals(listOf("a", "b", "1", "2.5"), nestedList)

        val nestedSet = nestedMapInList["nested_map_set"]?.ss()
        assertContentEquals(innerStringSet.toList(), nestedSet)

        val nestedBinarySet = nestedMapInList["nested_map_binary_set"]?.bs()
        assertEquals(2, nestedBinarySet?.size)
        assertTrue(nestedBinarySet?.any { it.asByteArray().contentEquals(bin1) } == true)
        assertTrue(nestedBinarySet.any { it.asByteArray().contentEquals(bin2) })

        // map 内嵌 set
        val mapSet = mapList[5].ns()
        assertContentEquals(listOf("10", "20", "30"), mapSet)

        // map 嵌套 map
        val nestedMapMap = mapValue["map_nested_map"]?.m() ?: fail("map_nested_map missing")
        assertEquals("deep", nestedMapMap["deep_str"]?.s())
        val deepList = nestedMapMap["deep_list"]?.l()?.map { it.n() }
        assertContentEquals(listOf("1", "2", "3"), deepList)
        val deepSet = nestedMapMap["deep_set"]?.ss()
        assertContentEquals(innerStringSet.toList(), deepSet)

        // List 内嵌 map / list / set 断言
        val listValue = item[TestTable.listValue.name]?.l() ?: fail("list missing")

        val listMap = listValue[5].m()
        assertEquals("lmap", listMap["list_map_str"]?.s())
        assertEquals("999.99", listMap["list_map_num"]?.n())
        assertEquals(false, listMap["list_map_bool"]?.bool())
        val listMapList = listMap["list_map_list"]?.l()?.map { it.s() }
        assertContentEquals(listOf("x", "y"), listMapList)
        val listMapSet = listMap["list_map_set"]?.ns()?.map { it.toInt() }
        assertContentEquals(innerNumberSet.toList(), listMapSet)
        val listMapBinarySet = listMap["list_map_binary_set"]?.bs()
        assertEquals(2, listMapBinarySet?.size)

        val nestedListValue = listValue[6].l()
        assertEquals("nested_list_string", nestedListValue[0].s())
        val nestedListMap = nestedListValue[1].m()
        assertEquals("nlmap", nestedListMap["nested_list_map_str"]?.s())
        val nestedListMapList = nestedListMap["nested_list_map_list"]?.l()?.map { it.n() }
        assertContentEquals(listOf("1", "2"), nestedListMapList)
        val nestedListMapSet = nestedListMap["nested_list_map_set"]?.ss()
        assertContentEquals(innerStringSet.toList(), nestedListMapSet)

        val nestedListSet = nestedListValue[2].bs()
        assertEquals(2, nestedListSet?.size)

        // List 内嵌 set
        val lastListSet = listValue[7].ss()
        assertContentEquals(innerStringSet.toList(), lastListSet)
    }


    @Test
    fun `test putIfNotExist failure`() {

        TestTable.putIfNotExist {
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
        TestTable.putIfNotExist {
            it[TestTable.pk] = "pk_cond5"
            it[TestTable.sk] = "sk_cond5"
            it[TestTable.name] = "conditionalItem5_new"
            it[TestTable.intValue] = 10
        }.request().let {
            assertThrows<ConditionalCheckFailedException> {
                client.putItem(it)
            }
        }

        // 验证原数据未被覆盖
        val resp = TestTable.get().keys {
            (TestTable.pk eq "pk_cond5") and (TestTable.sk eq "sk_cond5")
        }.request().let { client.getItem(it) }

        assertEquals("conditionalItem5_old", resp.item()[TestTable.name.name]?.s())
        assertEquals(5.toString(), resp.item()[TestTable.intValue.name]?.n())
    }

    @Test
    fun `test with only primary key`() {
        // 测试只设置主键的情况
        TestTable.put {
            it[TestTable.pk] = "pk_only"
            it[TestTable.sk] = "sk_only"
        }.request().also { client.putItem(it) }

        // 执行查询
        val getRequest = TestTable.get().keys {
            (TestTable.pk eq "pk_only") and (TestTable.sk eq "sk_only")
        }.request()

        val response = client.getItem(getRequest)
        val item = response.item() ?: fail("Item not found")

        // 断言只有主键
        assertEquals("pk_only", item[TestTable.pk.name]?.s())
        assertEquals("sk_only", item[TestTable.sk.name]?.s())

        // 其他字段应该不存在或为null
        assertNull(item[TestTable.name.name])
        assertNull(item[TestTable.intValue.name])
    }

}