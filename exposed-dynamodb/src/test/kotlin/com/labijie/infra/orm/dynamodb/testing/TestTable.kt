/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb.testing

import com.labijie.infra.orm.dynamodb.DynamoTable
import com.labijie.infra.orm.dynamodb.IColumnIndexable
import com.labijie.infra.orm.dynamodb.StringKeysDynamoTable
import software.amazon.awssdk.services.dynamodb.model.ProjectionType


object Table1 : StringKeysDynamoTable("bigtable1") {

    val pk = string("PK")
    val sk = string("SK")

    val name = string("name")
    val boolValue = boolean("bv")
    val intValue = integer("iv").index("fff")
    val floatValue = float("iv")
    val stringSet = stringSet("str_set")
    val numSet = numberSet("num_set")
    val listVale = list("list_value")
    val mapVal = map("map_value")

    override val keys: DynamoKeys<String, String>
        get() = DynamoKeys(pk, sk)

    override fun partitionKey(): IColumnIndexable<*, String> {
        return pk
    }

    override fun sortKey(): IColumnIndexable<*, String>? {
        return sk
    }
}


object TestTable : StringKeysDynamoTable("test_table") {

    // Primary key
    val pk = string("pk")            // partition key
    val sk = string("sk")            // sort key

    // Standard columns (all types)
    val name = string("name_s")
    val boolValue = boolean("bool_value")
    val intValue = integer("int_value")
    val floatValue = float("float_value")
    val doubleValue = double("double_value")
    val shortValue = short("short_value")
    val longValue = long("long_value")
        .index("idx_long", ProjectionType.ALL) // LSI with projection
    val stringValue = string("string_value")
        .index("idx_string", ProjectionType.INCLUDE, name, boolValue) // LSI with projection

    val stringSet = stringSet("string_set")
    val numberSet = numberSet("number_set")
    val binaryValue = binary("binary_value")
    val binarySet = binarySet("binary_set")

    val listValue = list("list_value")
    val mapValue = map("map_value")

    override fun partitionKey(): IColumnIndexable<*, String> {
        return pk
    }

    override fun sortKey(): IColumnIndexable<*, String>? {
        return sk
    }
}

class TestEntity(
    // Primary Key
    var pk: String = "",
    var sk: String = "",

    var name: String? = null,
    var boolValue: Boolean? = null,
    var intValue: Int? = null,
    var floatValue: Float? = null,
    var doubleValue: Double? = null,
    var shortValue: Short? = null,
    var longValue: Long? = null,
    var stringValue: String? = null,

    var stringSet: Set<String>? = null,
    var numberSet: Set<Number>? = null,

    var binaryValue: ByteArray? = null,
    var binarySet: Set<ByteArray>? = null,

    var listValue: List<Any?>? = null,
    var mapValue: Map<String, Any?>? = null
)