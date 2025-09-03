/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb.testing

import com.labijie.infra.orm.dynamodb.DynamodbUtils
import com.labijie.infra.orm.dynamodb.get
import com.labijie.infra.orm.dynamodb.put
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals


class NumericBoundTester : TesterBase() {

    /** ---------------- Data Type Boundaries ---------------- */
    @Test
    fun `test numeric max boundaries`() {
        // 测试数值类型的边界值
        TestTable.put {
            it[TestTable.pk] = "pk_bound"
            it[TestTable.sk] = "sk_bound"
            it[TestTable.name] = "boundaryItem"
            it[TestTable.shortValue] = Short.MAX_VALUE
            it[TestTable.intValue] = Int.MAX_VALUE
            it[TestTable.longValue] = Long.MAX_VALUE
            it[TestTable.floatValue] = Float.MAX_VALUE
            it[TestTable.doubleValue] = Double.MAX_VALUE
        }.request().also { client.putItem(it) }

        val getReq = TestTable.get().keys {
            (TestTable.pk eq "pk_bound") and (TestTable.sk eq "sk_bound")
        }.request()
        val item = client.getItem(getReq).item() ?: error("Item not found")

        assertEquals(Short.MAX_VALUE, item[TestTable.shortValue.name]?.n()?.toShortOrNull())
        assertEquals(Int.MAX_VALUE, item[TestTable.intValue.name]?.n()?.toIntOrNull())
        assertEquals(Long.MAX_VALUE, item[TestTable.longValue.name]?.n()?.toLongOrNull())
        assertEquals(Float.MAX_VALUE, item[TestTable.floatValue.name]?.n()?.toFloatOrNull())
        assertEquals(DynamodbUtils.NUMBER_MAX_POSITIVE.toPlainString(), item[TestTable.doubleValue.name]?.n()?.let { BigDecimal(it) }?.toPlainString())
    }

    @Test
    fun `test numeric min boundaries`() {
        // 测试数值类型的边界值
        TestTable.put {
            it[TestTable.pk] = "pk_bound"
            it[TestTable.sk] = "sk_bound"
            it[TestTable.name] = "boundaryItem"
            it[TestTable.shortValue] = Short.MIN_VALUE
            it[TestTable.intValue] = Int.MIN_VALUE
            it[TestTable.longValue] = Long.MIN_VALUE
            it[TestTable.floatValue] = -Float.MAX_VALUE
            it[TestTable.doubleValue] = -Double.MAX_VALUE
        }.request().also { client.putItem(it) }

        val getReq = TestTable.get().keys {
            (TestTable.pk eq "pk_bound") and (TestTable.sk eq "sk_bound")
        }.request()
        val item = client.getItem(getReq).item() ?: error("Item not found")

        assertEquals(Short.MIN_VALUE, item[TestTable.shortValue.name]?.n()?.toShortOrNull())
        assertEquals(Int.MIN_VALUE, item[TestTable.intValue.name]?.n()?.toIntOrNull())
        assertEquals(Long.MIN_VALUE, item[TestTable.longValue.name]?.n()?.toLongOrNull())
        assertEquals(BigDecimal((-Float.MAX_VALUE).toString()).toPlainString(), item[TestTable.floatValue.name]?.n()?.let { BigDecimal(it) }?.toPlainString())
        assertEquals(DynamodbUtils.NUMBER_MAX_NEGATIVE.toPlainString(), item[TestTable.doubleValue.name]?.n()?.let { BigDecimal(it) }?.toPlainString())

    }

    @Test
    fun `test numeric scale boundaries`() {
        // 测试数值类型的边界值
        TestTable.put {
            it[TestTable.pk] = "pk_bound"
            it[TestTable.sk] = "sk_bound"
            it[TestTable.name] = "boundaryItem"
            it[TestTable.floatValue] = Float.MIN_VALUE
            it[TestTable.doubleValue] = Double.MIN_VALUE
        }.request().also { client.putItem(it) }

        val getReq = TestTable.get().keys {
            (TestTable.pk eq "pk_bound") and (TestTable.sk eq "sk_bound")
        }.request()
        val item = client.getItem(getReq).item() ?: error("Item not found")

        assertEquals(BigDecimal(Float.MIN_VALUE.toString()).toPlainString(), item[TestTable.floatValue.name]?.n()?.let { BigDecimal(it) }?.toPlainString())
        assertEquals(BigDecimal((DynamodbUtils.NUMBER_MIN_POSITIVE).toString()).toPlainString(), item[TestTable.doubleValue.name]?.n()?.let { BigDecimal(it) }?.toPlainString())

    }
}