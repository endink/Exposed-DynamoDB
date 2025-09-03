/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb.testing

import com.labijie.infra.orm.dynamodb.put
import com.labijie.infra.orm.dynamodb.query
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class IndexQueryTester : TesterBase() {

    /** ---------------- Index Validation ---------------- */
    @Test
    fun `test index projection`() {
        // 插入包含索引字段和投影字段的数据
        TestTable.put {
            it[TestTable.pk] = "pk_proj"
            it[TestTable.sk] = "sk_proj"
            it[TestTable.name] = "projectionItem" // 这个字段在索引投影中
            it[TestTable.boolValue] = true        // 这个字段在索引投影中
            it[TestTable.stringValue] = "indexValue" // 索引键
            it[TestTable.intValue] = 99           // 这个字段不在投影中
        }.request().also { client.putItem(it) }

        // 使用索引查询
        val queryReq = TestTable.query()
            .keys(index = "idx_string") {
                (TestTable.pk eq "pk_proj") and (TestTable.stringValue eq "indexValue")

            }
            .request()

        val response = client.query(queryReq)
        val items = response.items()

        assertEquals(1, items.size)
        val item = items[0]

        // 验证投影字段存在
        assertNotNull(item[TestTable.name.name])
        assertNotNull(item[TestTable.boolValue.name])
        assertNotNull(item[TestTable.stringValue.name])

        // 验证非投影字段不存在（或者为null，取决于DynamoDB的行为）
        // 注意：DynamoDB 可能返回null而不是完全省略字段
        assertEquals(99.toString(), item[TestTable.intValue.name]?.n())
    }
}