///**
// * THIS FILE IS PART OF HuanJing (huanjing.art) PROJECT
// * Copyright (c) 2023 huanjing.art
// * @author Huanjing Team
// */
//package art.huanjing.aws.dynamodb.testing
//
//import com.labijie.infra.orm.dynamodb.*
//import software.amazon.awssdk.services.dynamodb.DynamoDbClient
//
//
//class CRUDTester {
//
//    private lateinit var client: DynamoDbClient
//
//    fun testUpdate() {
//
//        val got = Table1.get().keys {
//            Table1.name eq "bbb"
//        }
//
//        Table1.get {
//            project(Table1.name, Table1, Table1.listVale[0]["a"])
//        }.keys {
//            Table1.name eq "bbb"
//        }
//
//        client.getItem(got.request())
//
//        Table1.update({
//            keys { Table1.name eq "bbb" }
//            condition { (Table1.intValue eq 1 ) or (Table1.intValue less 10) or (Table1.intValue greater 10) }
//        }) {
//            // Set name = :value
//            it[Table1.name] = "aaa"
//
//            // set a = a + :v1
//            it[Table1.intValue] = Table1.intValue + 1
//
//            // set a = a - :v
//            it[Table1.intValue] = Table1.intValue - 1
//
//            // Add intValue :value (value = -3)
//            it[Table1.intValue] += 3
//
//            it[Table1.floatValue] += 3.0f
//
//            it[Table1.floatValue] = null
//
//            it[Table1.floatValue] = 4.0f
//
//            //set list :values
//            it[Table1.listVale] = listOf("aaa", "bbb", "ccc")
//
//            //Add stringSet
//            it[Table1.stringSet] += setOfString("aa", "bbb")
//
//            //Delete stringSet
//            it[Table1.stringSet] -= setOfString("aa", "bbb")
//
//            //Delete
//            it[Table1.numSet] += setOfNumber(123.3, 333, 23)
//
//            //Delete
//            it[Table1.numSet] -= setOfNumber(123.3, 333, 23)
//
//            //create if not exist
//            it[Table1.stringSet] = Table1.stringSet.ifNotExists(setOfString("bbb"))
//
//            //example: append_list(if_not_exists(listVale, :values), listVale[0:])
//            it[Table1.listVale] = Table1.listVale.ifNotExists(listOf("aaa", "bbb", "ccc"))
//
//            //set a = b[3]
//            it[Table1.intValue] = Table1.listVale[3]
//
//            it[Table1.intValue] = Table1.mapVal["ddd"]
//
//            //remove field
//            it[Table1.name].remove()
//
//            //remove list
//            it[Table1.listVale].remove()
//
//            //remove map
//            it[Table1.mapVal].remove()
//
//            //remove list item
//            it[Table1.listVale][0].remove()
//
//            it[Table1.listVale][1] = 3
//
//            it[Table1.mapVal]["t"].remove()
//
//
//            it[Table1.mapVal] = mapOf("bbb" to "bbb", "ccc" to "ccc")
//
//            it[Table1.mapVal]["aaa"][1]["ccc"] = "aaa"
//
//            it[Table1.mapVal]["aaa"][1]["bb"] += setOfString("aaa", "bbb")
//
//            it[Table1.mapVal]["aaa"][1]["dd"] -= setOfNumber(1, 3, 23, 0.5f)
//        }
//
//        val query = Table1.query()
//            .keys (index = "OK") {
//                Table1.name eq "a" and (Table1.name beginsWith "aa") and
//                (Table1.shortValue eq  3.toShort())
//            }
//            .filter {
//                (Table1.name contains "aaa") and
//                (Table1.name eq "a") and
//                (Table1.name.size() lessEq 3) and
//                (Table1.name.isString()) and
//                ((Table1.name beginsWith "333") or (Table2.shortValue.between(1, 3))) or
//                (Table1.name.isString()) or
//                (Table1.intValue.exists()) or
//                (Table1.stringSet contains "1") or
//                not(Table1.intValue eq 3)
//            }
//
//        query.request()
//
//        Table1.insert {
//            it[Table1.name] = "a"
//            it[Table1.intValue] = null
//        }
//
//        Table1.delete {
//            keys {
//                Table1.name eq "bbb"
//            }
//            condition { Table1.intValue eq 0 }
//        }
//
//    }
//}