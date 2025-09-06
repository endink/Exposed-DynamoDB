/**
 * This file is part of Exposed-DynamoDB project.
 * Copyright (c) 2025
 * @author Anders Xiao
 *
 * File Create: 2025/9/5
 */

package com.labijie.infra.orm.dynamodb.aot

import com.labijie.infra.orm.dynamodb.builder.DynamoUpdateBuilder
import com.labijie.infra.orm.dynamodb.mapping.ReflectionDynamoDbMapper
import org.graalvm.nativeimage.hosted.Feature
import org.graalvm.nativeimage.hosted.RuntimeReflection
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

@Suppress("unused")
class ExposedDynamoDbFeatures : Feature {


    override fun beforeAnalysis(access: Feature.BeforeAnalysisAccess) {
        registerEnum(AttributeValue.Type::class.java)
        registerObject(ReflectionDynamoDbMapper::class.java)
        RuntimeReflection.register(DynamoUpdateBuilder::class.java)
    }

    private fun Feature.BeforeAnalysisAccess.findClass(clazzName: String): Class<*>? {
        return try {
            this.findClassByName(clazzName)
        }catch (e: ClassNotFoundException) {
            null
        }
    }


    private fun registerObject(clazz: Class<*>) {
        try {
            val instanceField = clazz.getDeclaredField("INSTANCE")
            RuntimeReflection.register(instanceField)
            RuntimeReflection.register(clazz)
        } catch (ex: NoSuchFieldException) {
            // 不是 object 就忽略
        }

        for (constructor in clazz.declaredConstructors) {
            RuntimeReflection.register(constructor)
        }
    }

    fun registerEnum(enumClass: Class<out Enum<*>?>) {
        RuntimeReflection.register(enumClass)
        RuntimeReflection.register(*enumClass.getDeclaredConstructors())
        RuntimeReflection.register(*enumClass.getDeclaredMethods())
        RuntimeReflection.register(*enumClass.getDeclaredFields())
    }
}