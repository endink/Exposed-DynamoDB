/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb

import java.util.function.IntFunction


@JvmInline
value class DynamoSet<T>(val values: MutableSet<T> = mutableSetOf()) : MutableSet<T> by values {
    @Deprecated("Dont user it in kotlin")
    override fun <T : Any?> toArray(generator: IntFunction<Array<out T?>?>): Array<out T?>? {
        val arr = generator.apply(values.size) // 创建目标数组
        if(arr == null) return null

        var i = 0
        for (v in values) {
            @Suppress("UNCHECKED_CAST")
            (arr as Array<T?>)[i++] = v as? T // 安全转换，如果不匹配则放 null
        }
        return arr
    }
}

fun setOfString(vararg v: String) = DynamoSet(v.toMutableSet())
fun setOfNumber(vararg v: Number) = DynamoSet(v.toMutableSet())
fun setOfBinary(vararg v: ByteArray) = DynamoSet(v.toMutableSet())

fun <T> Set<T>.asDynamoSet(): DynamoSet<T> {
    return DynamoSet(this.toMutableSet())
}

fun <T> Collection<T>.toDynamoSet(): DynamoSet<T> {
    return DynamoSet(this.toMutableSet())
}





