/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb.testing


import com.labijie.infra.orm.dynamodb.get
import com.labijie.infra.orm.dynamodb.put
import com.labijie.infra.orm.dynamodb.query
import com.labijie.infra.orm.dynamodb.schema.DynamodbSchemaUtils
import com.labijie.infra.orm.dynamodb.setOfNumber
import com.labijie.infra.orm.dynamodb.setOfString
import com.labijie.infra.orm.dynamodb.testing.TestingUtils.client
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.*
import kotlin.test.fail

class ProjectionTester {

    @BeforeEach
    fun setup() {
        // 清空表以确保测试独立性
        DynamodbSchemaUtils.createTableIfNotExist(client, TestTable)

        // 插入测试数据
        TestTable.put {
            it[TestTable.pk] = "projection_test"
            it[TestTable.sk] = "item1"
            it[TestTable.name] = "Test Item"
            it[TestTable.boolValue] = true
            it[TestTable.intValue] = 42
            it[TestTable.floatValue] = 3.14f
            it[TestTable.doubleValue] = 3.14159
            it[TestTable.longValue] = 123456789L
            it[TestTable.stringValue] = "Test String"

            // 集合类型
            it[TestTable.stringSet] = setOfString("set1", "set2", "set3")
            it[TestTable.numberSet] = setOfNumber(1, 2, 3)

            // 复杂类型
            it[TestTable.listValue] = listOf(
                "list_item1",
                123,
                mapOf(
                    "nested_key" to "nested_value",
                    "nested_number" to 456
                )
            )

            it[TestTable.mapValue] = mapOf(
                "key1" to "value1",
                "key2" to mapOf(
                    "nested_key" to "nested_value",
                    "nested_list" to listOf("a", "b", "c")
                ),
                "key3" to 789
            )
        }.request().also { client.putItem(it) }
    }

    @Test
    fun `test basic projection with get`() {
        // 使用投影只获取 name 和 intValue 字段
        val getRequest = TestTable.get {
            project(TestTable.name, TestTable.intValue)
        }.keys {
            (TestTable.pk eq "projection_test") and (TestTable.sk eq "item1")
        }.request()

        val response = client.getItem(getRequest)
        val item = response.item() ?: fail("Item not found")

        // 断言只有投影的字段存在
        assertNotNull(item[TestTable.name.name])
        assertNotNull(item[TestTable.intValue.name])

        // 断言其他字段不存在
        assertNull(item[TestTable.boolValue.name])
        assertNull(item[TestTable.floatValue.name])
        assertNull(item[TestTable.doubleValue.name])
        assertNull(item[TestTable.longValue.name])
        assertNull(item[TestTable.stringValue.name])
        assertNull(item[TestTable.stringSet.name])
        assertNull(item[TestTable.numberSet.name])
        assertNull(item[TestTable.listValue.name])
        assertNull(item[TestTable.mapValue.name])

        // 断言投影字段的值正确
        assertEquals("Test Item", item[TestTable.name.name]?.s())
        assertEquals(42, item[TestTable.intValue.name]?.n()?.toInt())
    }

    @Test
    fun `test nested projection with get`() {

        // 使用投影获取嵌套字段
        val getRequest = TestTable.get {
            project(
                TestTable.name,
                TestTable.listValue[0], // 列表的第一个元素
                TestTable.listValue[2]["nested_key"], // 列表中第三个元素的 nested_key
                TestTable.mapValue["key1"], // 映射中的 key1
                TestTable.mapValue["key2"]["nested_key"], // 嵌套映射中的 nested_key
                TestTable.mapValue["key2"]["nested_list"][1] // 嵌套列表中的第二个元素
            )
        }.keys {
            (TestTable.pk eq "projection_test") and (TestTable.sk eq "item1")
        }.request()

        val response = client.getItem(getRequest)
        val item = response.item() ?: fail("Item not found")

        // 断言投影的字段存在
        assertNotNull(item[TestTable.name.name])

        // 断言嵌套字段的值正确
        assertEquals("Test Item", item[TestTable.name.name]?.s())

        // 检查列表投影
        val listValue = item[TestTable.listValue.name]?.l()
        assertNotNull(listValue)
        assertEquals(2, listValue.size)

        assertEquals("list_item1", listValue[0].s())

        val thirdListItem = listValue[1].m()
        assertNotNull(thirdListItem)
        //断言嵌套 map 只返回一个值
        assertEquals(1, thirdListItem.size)

        assertEquals("nested_value", thirdListItem["nested_key"]?.s())

        // 检查映射投影
        val mapValue = item[TestTable.mapValue.name]?.m()
        assertNotNull(mapValue)
        assertEquals("value1", mapValue["key1"]?.s())

        val key2Map = mapValue["key2"]?.m()
        assertNotNull(key2Map)
        assertEquals("nested_value", key2Map["nested_key"]?.s())

        val nestedList = key2Map["nested_list"]?.l()
        assertNotNull(nestedList)
        assertEquals(1, nestedList.size)
        assertEquals("b", nestedList[0].s())

        // 断言其他字段不存在
        assertNull(item[TestTable.boolValue.name])
        assertNull(item[TestTable.intValue.name])
        assertNull(item[TestTable.floatValue.name])
    }

    @Test
    fun `test projection with query`() {
        // 插入另一个项目用于查询测试
        TestTable.put {
            it[TestTable.pk] = "projection_test"
            it[TestTable.sk] = "item2"
            it[TestTable.name] = "Second Item"
            it[TestTable.intValue] = 100
            it[TestTable.boolValue] = false
        }.request().also { client.putItem(it) }

        // 使用投影查询
        val queryRequest = TestTable.query {
            project(TestTable.name, TestTable.intValue)
        }.keys {
            TestTable.pk eq "projection_test"
        }.request()

        val response = client.query(queryRequest)
        val items = response.items()

        assertEquals(2, items.size)

        // 检查每个项目只包含投影的字段
        items.forEach { item ->
            assertNotNull(item[TestTable.name.name])
            assertNotNull(item[TestTable.intValue.name])

            // 断言其他字段不存在
            assertNull(item[TestTable.sk.name])
            assertNull(item[TestTable.pk.name])
            assertNull(item[TestTable.boolValue.name])
            assertNull(item[TestTable.floatValue.name])
            assertNull(item[TestTable.doubleValue.name])
            assertNull(item[TestTable.longValue.name])
            assertNull(item[TestTable.stringValue.name])
            assertNull(item[TestTable.stringSet.name])
            assertNull(item[TestTable.numberSet.name])
            assertNull(item[TestTable.listValue.name])
            assertNull(item[TestTable.mapValue.name])
        }
    }

    @Test
    fun `test projection with complex nested structures`() {
        // 测试更复杂的嵌套投影
        val getRequest = TestTable.get {
            project(
                TestTable.pk,
                TestTable.sk,
                TestTable.listValue[2]["nested_number"], // 列表中的嵌套数字
                TestTable.mapValue["key2"]["nested_list"], // 映射中的嵌套列表
                TestTable.mapValue["key3"] // 映射中的数字值
            )
        }.keys {
            (TestTable.pk eq "projection_test") and (TestTable.sk eq "item1")
        }.request()

        val response = client.getItem(getRequest)
        val item = response.item() ?: fail("Item not found")

        // 断言投影的字段存在且值正确
        assertEquals("projection_test", item[TestTable.pk.name]?.s())
        assertEquals("item1", item[TestTable.sk.name]?.s())

        // 检查列表中的嵌套数字
        val listValue = item[TestTable.listValue.name]?.l()
        assertNotNull(listValue)
        //内嵌时，列表只返回投影的项，多余项部返回
        assertEquals(1, listValue.size)
        val thirdListItem = listValue[0].m()
        assertNotNull(thirdListItem)
        assertEquals(456, thirdListItem["nested_number"]?.n()?.toInt())

        // 检查映射中的嵌套列表
        val mapValue = item[TestTable.mapValue.name]?.m()
        assertNotNull(mapValue)
        assertEquals(2, mapValue.size)

        val key2Map = mapValue["key2"]?.m()
        assertNotNull(key2Map)
        val nestedList = key2Map["nested_list"]?.l()
        assertNotNull(nestedList)
        assertEquals(3, nestedList.size)
        assertEquals("a", nestedList[0].s())
        assertEquals("b", nestedList[1].s())
        assertEquals("c", nestedList[2].s())

        // 检查映射中的数字值
        assertEquals(789, mapValue["key3"]?.n()?.toInt())

        // 断言其他字段不存在
        assertNull(item[TestTable.name.name])
        assertNull(item[TestTable.boolValue.name])
        assertNull(item[TestTable.intValue.name])
    }

    @Test
    fun `test projection with non-existent nested fields`() {
        // 测试投影不存在的嵌套字段
        val getRequest = TestTable.get {
            project(
                TestTable.name,
                TestTable.mapValue["non_existent_key"], // 不存在的键
                TestTable.listValue[5] // 不存在的索引
            )
        }.keys {
            (TestTable.pk eq "projection_test") and (TestTable.sk eq "item1")
        }.request()

        val response = client.getItem(getRequest)
        val item = response.item() ?: fail("Item not found")

        // 断言存在的字段正确返回
        assertEquals("Test Item", item[TestTable.name.name]?.s())

        // 断言不存在的字段返回null
        val mapValue = item[TestTable.mapValue.name]?.m()
        assertNull(mapValue)

        val listValue = item[TestTable.listValue.name]?.l()
        assertNull(listValue)
    }

    @Test
    fun `test projection with mixed nested and top-level fields`() {
        // 测试混合投影（顶层字段和嵌套字段）
        val getRequest = TestTable.get {
            project(
                TestTable.pk,
                TestTable.sk,
                TestTable.name,
                TestTable.intValue,
                TestTable.listValue[0],
                TestTable.mapValue["key1"],
                TestTable.mapValue["key2"]["nested_list"]
            )
        }.keys {
            (TestTable.pk eq "projection_test") and (TestTable.sk eq "item1")
        }.request()

        val response = client.getItem(getRequest)
        val item = response.item() ?: fail("Item not found")

        // 断言所有投影字段存在且值正确
        assertEquals("projection_test", item[TestTable.pk.name]?.s())
        assertEquals("item1", item[TestTable.sk.name]?.s())
        assertEquals("Test Item", item[TestTable.name.name]?.s())
        assertEquals(42, item[TestTable.intValue.name]?.n()?.toInt())

        // 检查列表的第一个元素
        val listValue = item[TestTable.listValue.name]?.l()
        assertNotNull(listValue)
        assertEquals("list_item1", listValue[0].s())

        // 检查映射中的值
        val mapValue = item[TestTable.mapValue.name]?.m()
        assertNotNull(mapValue)
        assertEquals("value1", mapValue["key1"]?.s())

        // 检查嵌套列表
        val key2Map = mapValue["key2"]?.m()
        assertNotNull(key2Map)
        val nestedList = key2Map["nested_list"]?.l()
        assertNotNull(nestedList)
        assertEquals(3, nestedList.size)
        assertEquals("a", nestedList[0].s())
        assertEquals("b", nestedList[1].s())
        assertEquals("c", nestedList[2].s())

        // 断言其他字段不存在
        assertNull(item[TestTable.boolValue.name])
        assertNull(item[TestTable.floatValue.name])
        assertNull(item[TestTable.doubleValue.name])
    }

    @Test
    fun `test projection with index`() {
        // 测试在查询索引时使用投影
        val queryRequest = TestTable.query {
            project(TestTable.name, TestTable.longValue)
        }.keys(index = "idx_long") {
            (TestTable.pk eq "projection_test") and (TestTable.longValue eq 123456789L)
        }.request()

        val response = client.query(queryRequest)
        val items = response.items()

        assertEquals(1, items.size)

        val item = items[0]
        assertNotNull(item[TestTable.name.name])
        assertNotNull(item[TestTable.longValue.name])

        // 断言投影字段的值正确
        assertEquals("Test Item", item[TestTable.name.name]?.s())
        assertEquals(123456789L, item[TestTable.longValue.name]?.n()?.toLong())

        // 断言其他字段不存在
        assertNull(item[TestTable.boolValue.name])
        assertNull(item[TestTable.intValue.name])
        assertNull(item[TestTable.floatValue.name])
    }

    @Test
    fun `test projection with included attributes in index`() {
        // 测试在包含投影的索引上使用投影
        // stringValue 索引包含了 name 和 boolValue 属性
        val queryRequest = TestTable.query {
            project(TestTable.stringValue, TestTable.name)
        }.keys(index = "idx_string") {
            (TestTable.pk eq "projection_test") and (TestTable.stringValue eq "Test String")
        }.request()

        val response = client.query(queryRequest)
        val items = response.items()

        assertEquals(1, items.size)

        val item = items[0]
        assertNotNull(item[TestTable.stringValue.name])
        assertNotNull(item[TestTable.name.name])

        // 断言投影字段的值正确
        assertEquals("Test String", item[TestTable.stringValue.name]?.s())
        assertEquals("Test Item", item[TestTable.name.name]?.s())

        // boolValue 在索引投影中包含，但是未投影
        assertNull(item[TestTable.boolValue.name])
    }
}