/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb

import com.labijie.infra.orm.dynamodb.exception.DynamoNumberOverflowException
import com.labijie.infra.orm.dynamodb.exception.DynamodbTypeMismatchException
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.math.BigDecimal
import java.math.RoundingMode


object AttributeValueConverter {

    internal fun Number.toDynamoNumber(): String {

        // DynamoDB numeric limits
        val maxDynamo = DynamodbUtils.NUMBER_MAX_POSITIVE
        val minPositive = DynamodbUtils.NUMBER_MIN_POSITIVE
        val maxNegative = DynamodbUtils.NUMBER_MAX_NEGATIVE
        val minNegative = DynamodbUtils.NUMBER_MIN_NEGATIVE

        // unify zero -> "0"
        if (this.toDouble() == 0.0) return "0"

        // reject NaN/Infinity
        if (this is Double && (this.isNaN() || this.isInfinite()))
            throw DynamoNumberOverflowException("NaN/Infinity not allowed for DynamoDB numbers")

        // Construct BigDecimal in a safe way depending on Number subtype
        val bd: BigDecimal = when (this) {
            is BigDecimal -> this
            is Double -> BigDecimal.valueOf(this)      // recommended for Double
            is Float -> BigDecimal(this.toString())    // use string form for Float to avoid toDouble() rounding
            is Long, is Int, is Short, is Byte ->
                BigDecimal.valueOf(this.toLong())
            else -> BigDecimal(this.toString())
        }

        // sign-aware in-range check
        val inRange = when {
            bd.signum() > 0 -> bd >= minPositive && bd <= maxDynamo
            bd.signum() < 0 -> bd >= maxNegative && bd <= minNegative
            else -> false
        }

        // if already safe (in range and <= 38 significant digits), return original textual representation (no change)
        if (inRange) {
            val sig = bd.stripTrailingZeros().precision()
            if (sig <= 38) {
                // return original textual representation of the Number
                return this.toString()
            }
        }

        // Otherwise clamp by sign to the allowed bounds
        var fixed = bd
        if (bd.signum() > 0) {
            if (bd > maxDynamo) fixed = maxDynamo
            if (bd < minPositive) fixed = minPositive
        } else {
            if (bd < maxNegative) fixed = maxNegative
            if (bd > minNegative) fixed = minNegative
        }

        // Ensure total significant digits <= 38 by truncating the fractional part (DOWN)
        var noZeros = fixed.stripTrailingZeros()
        val sig = noZeros.precision()
        if (sig > 38) {
            val reduce = sig - 38
            val newScale = (noZeros.scale() - reduce).coerceAtLeast(0)
            val truncated = noZeros.setScale(newScale, RoundingMode.DOWN)

            // integer-part protection: truncation must not change integer part relative to the original bd
            val origInt = bd.toBigInteger()
            val newInt = truncated.toBigInteger()
            if (origInt != newInt) {
                throw DynamoNumberOverflowException("Truncation changed the integer part: original=$origInt, new=$newInt")
            }
            noZeros = truncated
        }

        // Return a readable string. use engineering string (scientific-style) to avoid extremely long plain decimals for huge numbers.
        return noZeros.toEngineeringString()
    }

    private fun enumFromDb(dbValue: AttributeValue, column: EnumColumn<*>): Any {
        val raw = dbValue.n()
            ?: throw DynamodbTypeMismatchException(
                "Expected Number for enum column '${column.name}', got $dbValue"
            )

        val intValue = raw.toIntOrNull()
            ?: throw DynamodbTypeMismatchException(
                "Expected a valid integer for enum column '${column.name}', got $raw"
            )

        val constants = column.enumClass.enumConstants
            ?: throw DynamodbTypeMismatchException(
                "Enum class ${column.enumClass.name} has no constants"
            )

        if (intValue !in constants.indices) {
            throw DynamodbTypeMismatchException(
                "Enum index $intValue out of range for enum '${column.enumClass.simpleName}' (valid: 0..${constants.lastIndex})"
            )
        }

        return constants[intValue]
    }

    private fun enumToDb(value: Any, enumClass: Class<*>, columnName: String? = null): AttributeValue {
        // 确认 value 是 enumColumn 指定的 enum 类型
        if (!enumClass.isInstance(value)) {
            throw DynamodbTypeMismatchException(
                if (!columnName.isNullOrBlank()) {
                    "Value '$value' is not of enum type ${enumClass.simpleName} for column '${columnName}'"
                } else {
                    "Value '$value' is not of enum type ${enumClass.simpleName}"
                }
            )
        }

        val enumValue = value as Enum<*>
        return AttributeValue.builder().n(enumValue.ordinal.toString()).build()
    }

    fun fromDb(attr: AttributeValue, hint: DynamoColumn<*>): Any? {
        if (attr.nul() == true) return null

        if (hint is EnumColumn) {
            return enumFromDb(attr, hint)
        }

        return when (hint.dynamoDbType()) {
            DynamoDataType.STRING -> {
                attr.s()
            }

            DynamoDataType.STRING_SET -> {
                attr.ss()?.toSet() ?: emptySet<String>()
            }

            DynamoDataType.NUMBER -> {
                attr.n()?.let { n ->
                    if (hint !is NumericColumn) {
                        throw DynamodbTypeMismatchException("Unsupported NUMBER mapping: expected NumericColumn, but got hint type ${hint::class.simpleName}")
                    }
                    // 根据 hint 提示的 Kotlin 类型决定返回什么
                    when (hint.javaClass) {
                        Int::class.java -> n.toInt()
                        Long::class.java -> n.toLong()
                        Float::class.java -> n.toFloat()
                        Short::class.java -> n.toShort()
                        Double::class.java -> n.toDouble()
                        else -> throw DynamodbTypeMismatchException("Unsupported NUMBER mapping: expected Int/Long/Float/Short/Double, but got hint type ${hint::class.simpleName}")
                    }
                }
            }

            DynamoDataType.NUMBER_SET -> {
                try {
                    attr.ns()?.map { n -> n.toDouble() }?.toSet() ?: emptySet<Any>()
                } catch (e: NumberFormatException) {
                    throw DynamodbTypeMismatchException(
                        "Expected a set of numbers for column '${hint.name}', but got invalid value: [${
                            attr.ns().joinToString(", ")
                        }]", e
                    )
                }
            }

            DynamoDataType.BINARY -> {
                attr.b()?.asByteArray()
            }

            DynamoDataType.BINARY_SET -> {
                attr.bs()?.map { it.asByteArray() }?.toSet() ?: emptySet<ByteArray>()
            }

            DynamoDataType.BOOLEAN -> {
                attr.bool()
            }

            DynamoDataType.NULL -> null

            DynamoDataType.LIST -> {
                attr.l()?.map { av ->
                    fromDb(av, hint)
                } ?: emptyList<Any?>()
            }

            DynamoDataType.MAP -> {
                attr.m()?.mapValues { (k, v) ->
                    fromDb(v, hint)
                } ?: emptyMap<String, Any?>()
            }

            else -> throw DynamodbTypeMismatchException("Unsupported DynamoDB type: ${hint.dynamoDbType()}")
        }
    }

    fun fromDb(value: AttributeValue): Any? {
        return when {
            value.s() != null -> value.s()
            value.n() != null -> value.n().toDouble()   // 这里默认解析为 Double，可根据列 hint 转换成 Int/Long/Float
            value.bool() != null -> value.bool()
            value.nul() == true -> null
            value.b() != null -> value.b().asByteArray()
            value.ss() != null -> value.ss().toSet()
            value.ns() != null -> value.ns().map { it.toDouble() }.toSet()   // 默认 Double，可按列 hint 转换
            value.bs() != null -> value.bs().map { it.asByteArray() }.toSet()
            value.l() != null -> value.l().map { fromDb(it) }
            value.m() != null -> value.m().mapValues { fromDb(it.value) }
            else -> throw DynamodbTypeMismatchException("Unsupported AttributeValue: $value")
        }
    }

    fun toDb(value: Any?): AttributeValue {

        if (value == null) {
            return AttributeValue.fromNul(true)
        }

        if (value::class.java.isEnum) {
            return enumToDb(value, value::class.java)
        }

        return when (value) {
            is String -> AttributeValue.builder().s(value).build()
            is Float -> AttributeValue.builder().n(value.toDynamoNumber()).build()
            is Double -> AttributeValue.builder().n(value.toDynamoNumber()).build()
            is Number -> AttributeValue.builder().n(value.toString()).build()
            is Boolean -> AttributeValue.builder().bool(value).build()
            is ByteArray -> AttributeValue.builder().b(SdkBytes.fromByteArray(value)).build()

            is List<*> -> AttributeValue.builder().l(value.map { toDb(it) }).build()
            is Map<*, *> -> {
                val m = value.mapKeys { it.key.toString() }.mapValues { toDb(it.value) }
                AttributeValue.builder().m(m).build()
            }

            is Set<*> -> {
                if(value is DynamoSet<*>) {
                    if (value.values.isEmpty()) throw DynamodbTypeMismatchException("Empty Set cannot infer type")

                    when (val first = value.values.first()) {
                        is String -> AttributeValue.builder().ss(value.values.filterIsInstance<String>()).build()
                        is Number -> AttributeValue.builder()
                            .ns(value.values.filterIsInstance<Number>().map { it.toString() }).build()

                        is ByteArray -> AttributeValue.builder()
                            .bs(value.values.filterIsInstance<ByteArray>().map { SdkBytes.fromByteArray(it) }).build()

                        else -> throw DynamodbTypeMismatchException("Unsupported Set element type: ${first!!::class.java.simpleName}")
                    }
                }else{
                    throw DynamodbTypeMismatchException(
                        "Unsupported Set type. Use DynamoSet instead, e.g., setOfString() or setOfNumber() or setOfBinary."
                    )
                }
            }

            else -> throw DynamodbTypeMismatchException("Unsupported data type: ${value::class.java.simpleName}")
        }
    }

    fun toDb(value: Any?, hint: DynamoColumn<*>): AttributeValue {

        if (value == null) {
            return AttributeValue.fromNul(true)
        }

        if (hint is EnumColumn) {
            return enumToDb(value, hint.enumClass, hint.name)
        }

        return when (hint.dynamoDbType()) {
            DynamoDataType.STRING -> {
                if (value !is String)
                    throw DynamodbTypeMismatchException("expects String, but got ${value.javaClass.simpleName}")
                AttributeValue.builder().s(value).build()
            }

            DynamoDataType.STRING_SET -> {
                if (value !is DynamoSet<*> || value.any { it !is String })
                    throw DynamodbTypeMismatchException("expects DynamoSet<String>, but got ${value.javaClass.simpleName}")
                AttributeValue.builder().ss(value.filterIsInstance<String>()).build()
            }

            DynamoDataType.NUMBER -> {
                when (value) {
                    is Float -> AttributeValue.builder().n(value.toDynamoNumber()).build()
                    is Double -> AttributeValue.builder().n(value.toDynamoNumber()).build()
                    is Number -> AttributeValue.builder().n(value.toString()).build()
                    else -> throw DynamodbTypeMismatchException("expects Number, but got ${value.javaClass.simpleName}")
                }
            }

            DynamoDataType.NUMBER_SET -> {
                if (value !is DynamoSet<*> || value.any { it !is Number })
                    throw DynamodbTypeMismatchException("expects DynamoSet<Number>, but got ${value.javaClass.simpleName}")
                AttributeValue.builder().ns(value.map { it.toString() }).build()
            }

            DynamoDataType.BINARY -> {
                when (value) {
                    is ByteArray -> AttributeValue.builder().b(SdkBytes.fromByteArray(value)).build()
                    is SdkBytes -> AttributeValue.builder().b(value).build()
                    else -> throw DynamodbTypeMismatchException("expects ByteArray or SdkBytes, but got ${value.javaClass.simpleName}")
                }
            }

            DynamoDataType.BINARY_SET -> {
                if (value !is DynamoSet<*> || value.any { it !is ByteArray && it !is SdkBytes })
                    throw DynamodbTypeMismatchException("expects DynamoSet<ByteArray|SdkBytes>, but got ${value.javaClass.simpleName}")
                val binaries = value.map {
                    when (it) {
                        is ByteArray -> SdkBytes.fromByteArray(it)
                        is SdkBytes -> it
                        else -> throw IllegalStateException("Unexpected binary element $it")
                    }
                }
                AttributeValue.builder().bs(binaries).build()
            }

            DynamoDataType.BOOLEAN -> {
                if (value !is Boolean)
                    throw DynamodbTypeMismatchException("expects Boolean, but got ${value.javaClass.simpleName}")
                AttributeValue.builder().bool(value).build()
            }

            DynamoDataType.NULL -> AttributeValue.fromNul(true)

            DynamoDataType.LIST -> {
                if (value !is Collection<*>)
                    throw DynamodbTypeMismatchException("expects List<Any?>, but got ${value.javaClass.simpleName}")
                val listValues = value.map { toDb(it) }
                AttributeValue.builder().l(listValues).build()
            }

            DynamoDataType.MAP -> {
                if (value !is Map<*, *>)
                    throw DynamodbTypeMismatchException("expects Map<String, Any?>, but got ${value.javaClass.simpleName}")
                val mapValues = value.map { (k, v) ->
                    if (k !is String) throw DynamodbTypeMismatchException("Map key must be String, got ${k?.javaClass?.simpleName}")
                    k to toDb(v) // hint 提供子列信息
                }.toMap()
                AttributeValue.builder().m(mapValues).build()
            }

            else -> throw DynamodbTypeMismatchException("Unsupported DynamoDB type: ${hint.dynamoDbType()}")
        }
    }
}