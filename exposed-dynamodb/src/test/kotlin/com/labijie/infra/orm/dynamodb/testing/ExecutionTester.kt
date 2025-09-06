/**
 * This file is part of Exposed-DynamoDB project.
 * Copyright (c) 2025
 * @author Anders Xiao
 *
 * File Create: 2025/9/6
 */

package com.labijie.infra.orm.dynamodb.testing

import com.labijie.infra.orm.dynamodb.batchGet
import com.labijie.infra.orm.dynamodb.execution.execute
import com.labijie.infra.orm.dynamodb.get
import com.labijie.infra.orm.dynamodb.put
import com.labijie.infra.orm.dynamodb.query
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ExecutionTester : TesterBase() {

    private fun insetTestData(count: Int) {
        repeat(count) { index ->
            TestTable.put {
                it[TestTable.pk] = "pk_proj"
                it[TestTable.sk] = "P#$index"
                it[TestTable.name] = UUID.randomUUID().toString().replace("-", "") // 这个字段在索引投影中
                it[TestTable.boolValue] = true
                it[TestTable.stringValue] = "ss${index + 1}"
                it[TestTable.intValue] = index + 1
            }.request().also { client.putItem(it) }
        }
    }

    private fun assetEntity(entity: TestEntity) {
        assertEquals("pk_proj", entity.pk)
        assertEquals("P#", entity.sk.substring(0, 2))
        assert(!entity.name.isNullOrBlank())
        assertEquals(true, entity.boolValue)
        assert(!entity.stringValue.isNullOrBlank())
        assert((!entity.stringValue.isNullOrBlank()) && entity.stringValue?.startsWith("ss") ?: false)
        assert(entity.intValue != null)
    }

    @ParameterizedTest
    @ValueSource(ints = [10])
    fun `test batch get`(times: Int) {
        insetTestData(times)

        val result = client.execute {
            TestTable.batchGet {
                get {
                    repeat(times) { index ->
                        keys("pk_proj", "P#${index}")
                    }
                    consistentRead(false)
                }

            }.exec().readValues(TestEntity::class.java)
        }

        assertEquals(times, result.size)
        result.forEach {
            assetEntity(it)
        }
    }


    @Test
    fun `test get`() {
        insetTestData(5)

        val result = client.execute {
            val v = TestTable.get()
                .keys("pk_proj", "P#2")
                .exec()
                .readValue(TestEntity::class.java)
            v
        }

        assertNotNull(result)

        assertEquals("pk_proj", result.pk)
        assertEquals("P#2", result.sk)
        assert(!result.name.isNullOrBlank())
        assertEquals(true, result.boolValue)
        assert(!result.stringValue.isNullOrBlank())
        assert((result.stringValue?.startsWith("ss") ?: false))
        assertEquals(3, result.intValue)
    }

    @Test
    fun `test forward query`() {
        insetTestData(5)

        val result = client.execute {
            TestTable.query()
                .keys {
                    (TestTable.pk eq "pk_proj") and (TestTable.sk beginsWith "P#")
                }

                .limit(2)
                .exec().readValue(TestEntity::class.java)
        }

        assertNotNull(result.forwardToken)
        assert(result.forwardToken.isNotBlank())
        assertEquals(2, result.list.size)
        assertEquals("P#0", result.list[0].sk)
        assertEquals("P#1", result.list[1].sk)

        val result2 = client.execute {
            TestTable.query()
                .keys {
                    (TestTable.pk eq "pk_proj") and (TestTable.sk beginsWith "P#")
                }
                .lastKeys(result.forwardToken)
                .limit(2)
                .exec().readValue(TestEntity::class.java)
        }

        assertNotNull(result2.forwardToken)
        assert(result2.forwardToken.isNotBlank())
        assertEquals(2, result2.list.size)
        assertEquals("P#2", result2.list[0].sk)
        assertEquals("P#3", result2.list[1].sk)


        val result3 = client.execute {
            TestTable.query()
                .keys {
                    (TestTable.pk eq "pk_proj") and (TestTable.sk beginsWith "P#")
                }
                .lastKeys(result2.forwardToken)
                .limit(2)
                .exec()
                .readValue(TestEntity::class.java)
        }

        assertNull(result3.forwardToken)
        assertEquals(1, result3.list.size)
        assertEquals("P#4", result3.list[0].sk)

    }

}