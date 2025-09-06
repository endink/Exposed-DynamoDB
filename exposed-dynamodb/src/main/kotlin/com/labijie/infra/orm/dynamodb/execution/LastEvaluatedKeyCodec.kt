/**
 * This file is part of Exposed-DynamoDB project.
 * Copyright (c) 2025
 * @author Anders Xiao
 *
 * File Create: 2025/9/5
 */

package com.labijie.infra.orm.dynamodb.execution

import com.labijie.infra.orm.dynamodb.exception.DynamoException
import com.labijie.infra.orm.dynamodb.exception.InvalidDynamoForwardTokenException
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Base64
import kotlin.collections.joinToString

object LastEvaluatedKeyCodec {

    private fun AttributeValue.asString(): String {
        val s = when (this.type()) {
            AttributeValue.Type.S -> this.s()
            AttributeValue.Type.N -> this.n()
            AttributeValue.Type.B -> this.b().asString(Charsets.UTF_8)
            AttributeValue.Type.NUL -> "null"
            else-> throw DynamoException("Unsupported value type for key: ${this.type()}")
        }
        return "${this.type()}:${s}"
    }

    private fun String.toAttribute(): AttributeValue {
        val v = this.split(":")
        if(v.size != 2) {
            throw InvalidDynamoForwardTokenException()
        }
        val type = v[0].let { AttributeValue.Type.valueOf(it) }
        val s = when (type) {
            AttributeValue.Type.S -> AttributeValue.builder().s(v[1]).build()
            AttributeValue.Type.N -> AttributeValue.builder().n(v[1]).build()
            AttributeValue.Type.B -> AttributeValue.builder().b(SdkBytes.fromString(v[1], Charsets.UTF_8)).build()
            AttributeValue.Type.NUL -> AttributeValue.builder().nul(true).build()
            else-> throw DynamoException("Unsupported value type for key: $type")
        }
        return s
    }

    fun encode(values: Map<String, AttributeValue>) : String {
        val map =  values.map {
            it.key to it.value.asString()
        }

        val str = map.joinToString("&") { "${it.first}=${URLEncoder.encode(it.second, Charsets.UTF_8)}" }
        return Base64.getUrlEncoder().encode(str.toByteArray(Charsets.UTF_8)).toString(Charsets.UTF_8)
    }

    fun decode(stringValue: String): Map<String, AttributeValue> {
        val str = Base64.getUrlDecoder().decode(stringValue.toByteArray(Charsets.UTF_8)).toString(Charsets.UTF_8)
        val list =  str.split("&")
        val map = mutableMapOf<String, AttributeValue>()
        list.forEach { values ->
            val keyValue = values.split("=")
            if(keyValue.size == 2) {
                val name = keyValue[0]
                val value = URLDecoder.decode(keyValue[1], Charsets.UTF_8).toAttribute()
                map.putIfAbsent(name, value)
            }
        }
        return map
    }
}