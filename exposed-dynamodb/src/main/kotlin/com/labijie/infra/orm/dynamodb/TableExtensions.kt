/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb

import com.labijie.infra.orm.dynamodb.builder.*
import com.labijie.infra.orm.dynamodb.mapping.ReflectionDynamoDbMapper
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.ConditionCheck
import software.amazon.awssdk.services.dynamodb.model.Put



fun <T : DynamoTable> T.batchGet(build:  DynamoBatchGetBuilder.Context.()-> Unit?): DynamoBatchGetBuilder {
    val builder = DynamoBatchGetBuilder(this)
    build.invoke(builder.context)
    return builder
}

fun <T : DynamoTable> T.get(projectType: DynamoProjectionType = DynamoProjectionType.TableOnly): DynamoGetBuilder {
    val builder = DynamoGetBuilder(this)
    val projection = builder.projection()
    when (projectType) {
        DynamoProjectionType.ALL -> projection.projectAll()
        DynamoProjectionType.TableOnly -> projection.project(this)
        DynamoProjectionType.KeyOnly -> projection.projectKeyOnly()
    }
    return builder
}

fun <T : DynamoTable> T.get(projection: ProjectionBaseBuilder.ProjectionBuilder.()-> Unit): DynamoGetBuilder {
    val builder = DynamoGetBuilder(this)
    val instance = builder.projection()
    projection.invoke(instance)
    return builder
}

fun <T : DynamoTable> T.query(projectType: DynamoProjectionType = DynamoProjectionType.TableOnly): DynamoQueryBuilder {
    val builder = DynamoQueryBuilder(this)
    val projection = builder.projection()
    when (projectType) {
        DynamoProjectionType.ALL -> projection.projectAll()
        DynamoProjectionType.TableOnly -> projection.project(this)
        DynamoProjectionType.KeyOnly -> projection.projectKeyOnly()
    }
    return builder
}

fun <T : DynamoTable> T.query(projection: ProjectionBaseBuilder.ProjectionBuilder.()-> Unit): DynamoQueryBuilder {
    val builder = DynamoQueryBuilder(this)
    val instance = builder.projection()
    projection.invoke(instance)
    return builder
}

fun <T : DynamoTable> T.putIfNotExist(
    block: T.(dsl: DynamoPutBuilder.DynamoUpdateSetter) -> Unit
): Put {
    val pk = this.keys.partitionKey
    val sk = this.keys.sortKey

    val dsl = DynamoPutBuilder(this)
    dsl.condition {
        pk.getColumn().notExists().andIfNotNull(sk) {
            it.getColumn().notExists()
        }
    }
    block.invoke(this, dsl.setter)

    return dsl.build()
}

fun <T : DynamoTable> T.put(
    block: T.(dsl: DynamoPutBuilder.DynamoUpdateSetter) -> Unit
): DynamoPutBuilder {
    val dsl = DynamoPutBuilder(this)
    block.invoke(this, dsl.setter)

    return dsl
}


fun <T: DynamoTable> T.check(where: DynamoConditionBuilder.() -> DynamoConditionBuilder): ConditionCheck {
    val builder = DynamoConditionBuilder()
    where.invoke(builder)
    val result = builder.buildCondition()

    val ctx = RenderContext(true)
    val expr = result.conditionExpression?.render(ctx)

    val check = ConditionCheck.builder()
        .tableName(this.tableName)
        .key(result.keys)
        .ifNullOrBlankInput(expr) { conditionExpression(it) }
        .ifNotNullOrEmpty(ctx.values) { expressionAttributeValues(it) }
        .ifNotNullOrEmpty(ctx.attributeNames) { expressionAttributeNames(it) }
        .build()

    return check
}


fun <T : DynamoTable> T.delete(
    where: DynamoConditionBuilder.() -> DynamoConditionBuilder
): DynamoDeleteBuilder {
    val builder = DynamoDeleteBuilder(this)
    where.invoke(builder.condition)
    return builder
}

fun <T : DynamoTable> T.update(
    where: DynamoConditionBuilder.() -> DynamoConditionBuilder,
    block: DynamoUpdateBuilder.DynamoSegmentsBuilder.(getter: DynamoUpdateGetter) -> Unit
): DynamoUpdateBuilder {

    val builder = DynamoUpdateBuilder(this)

    val getter = DynamoUpdateGetter(builder.segments)
    block.invoke(builder.segments, getter)
    // Build the where
    where.invoke(builder.condition)

    return builder
}





