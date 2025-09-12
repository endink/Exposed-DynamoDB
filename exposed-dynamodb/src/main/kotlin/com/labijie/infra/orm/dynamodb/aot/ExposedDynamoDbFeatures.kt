/**
 * This file is part of Exposed-DynamoDB project.
 * Copyright (c) 2025
 * @author Anders Xiao
 *
 * File Create: 2025/9/5
 */

package com.labijie.infra.orm.dynamodb.aot

import com.labijie.infra.orm.dynamodb.DynamoTable
import com.labijie.infra.orm.dynamodb.builder.DynamoUpdateBuilder
import com.labijie.infra.orm.dynamodb.mapping.ReflectionDynamoDbMapper
import com.labijie.infra.orm.dynamodb.schema.DynamodbSchemaUtils
import io.github.classgraph.ClassGraph
import io.github.classgraph.ClassInfo
import org.graalvm.nativeimage.hosted.Feature
import org.graalvm.nativeimage.hosted.RuntimeReflection
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.util.function.Consumer
import kotlin.math.max


@Suppress("unused")
class ExposedDynamoDbFeatures : Feature {

    private val logger = LoggerFactory.getLogger(ExposedDynamoDbFeatures::class.java)

    override fun beforeAnalysis(access: Feature.BeforeAnalysisAccess) {
        registerEnum(AttributeValue.Type::class.java)
        registerObject(ReflectionDynamoDbMapper::class.java)
        RuntimeReflection.register(DynamoUpdateBuilder::class.java, DynamodbSchemaUtils::class.java, DynamoTable::class.java)

        access.applicationClassLoader.definedPackages.forEach {
            val classGraph = ClassGraph()
                .acceptPackages(it.name)
                .enableClassInfo()
                .enableStaticFinalFieldConstantInitializerValues()
                .enableMemoryMapping()

            val parallelism = max(1, Runtime.getRuntime().availableProcessors() / 2)
            classGraph.scan(parallelism).use { scanResult ->
                scanResult.allClasses.forEach(Consumer { classInfo: ClassInfo ->
                    if (classInfo.superclasses.any { c -> c.name == DynamoTable::class.java.name }) {
                        access.findClass(classInfo.name)?.let {
                            tableClass->
                            if(registerObject(tableClass)) {
                                logger.info("AOT dynamo table found: ${tableClass.name}")
                            }
                        }
                    }
                })
            }

        }
    }

    private fun Feature.BeforeAnalysisAccess.findClass(clazzName: String): Class<*>? {
        return try {
            this.findClassByName(clazzName)
        }catch (e: ClassNotFoundException) {
            null
        }
    }


    private fun registerObject(clazz: Class<*>): Boolean {
        try {
            val instanceField = clazz.getDeclaredField("INSTANCE")
            RuntimeReflection.register(instanceField)
            RuntimeReflection.register(clazz)
        } catch (ex: NoSuchFieldException) {
            return false
        }

        for (constructor in clazz.declaredConstructors) {
            RuntimeReflection.register(constructor)
        }
        return true
    }

    fun registerEnum(enumClass: Class<out Enum<*>?>) {
        RuntimeReflection.register(enumClass)
        RuntimeReflection.register(*enumClass.getDeclaredConstructors())
        RuntimeReflection.register(*enumClass.getDeclaredMethods())
        RuntimeReflection.register(*enumClass.getDeclaredFields())
    }
}