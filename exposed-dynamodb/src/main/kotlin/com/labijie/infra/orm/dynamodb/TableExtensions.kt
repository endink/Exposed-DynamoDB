/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb

import com.labijie.infra.orm.dynamodb.DynamodbUtils.prettyString
import com.labijie.infra.orm.dynamodb.builder.DynamoConditionBuilder
import com.labijie.infra.orm.dynamodb.builder.DynamoGetBuilder
import com.labijie.infra.orm.dynamodb.builder.DynamoPutBuilder
import com.labijie.infra.orm.dynamodb.builder.DynamoQueryBuilder
import com.labijie.infra.orm.dynamodb.builder.DynamoUpdateBuilder
import com.labijie.infra.orm.dynamodb.builder.ProjectionBaseBuilder
import software.amazon.awssdk.services.dynamodb.model.*


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
    block: T.(dsl: DynamoUpdateSetter) -> Unit
): Put {
    val pk = this.keys.partitionKey
    val sk = this.keys.sortKey

    val dsl = DynamoPutBuilder(this)
    val setter = DynamoUpdateSetter(dsl)
    dsl.condition {
        pk.getColumn().notExists().andIfNotNull(sk) {
            it.getColumn().notExists()
        }
    }
    block.invoke(this, setter)

    return dsl.build()
}

fun <T : DynamoTable> T.put(
    block: T.(dsl: DynamoUpdateSetter) -> Unit
): DynamoPutBuilder {
    val dsl = DynamoPutBuilder(this)
    val setter = DynamoUpdateSetter(dsl)

    block.invoke(this, setter)

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


private fun <T : DynamoTable> T.delete(
    returnValue: Boolean,
    where: DynamoConditionBuilder.() -> DynamoConditionBuilder
): Delete {

    val builder = DynamoConditionBuilder()
    where.invoke(builder)
    val result = builder.buildCondition()

    val ctx = RenderContext(true)
    val expr = result.conditionExpression?.render(ctx)

    return Delete.builder()
        .tableName(this.tableName)
        .key(result.keys)
        .ifNullOrBlankInput(expr) { conditionExpression(it) }
        .ifNotNullOrEmpty(ctx.values) { expressionAttributeValues(it) }
        .ifNotNullOrEmpty(ctx.attributeNames) { expressionAttributeNames(it) }
        .returnValuesOnConditionCheckFailure(if(returnValue) ReturnValuesOnConditionCheckFailure.ALL_OLD else ReturnValuesOnConditionCheckFailure.NONE)
        .build()
}

private inline fun <T : DynamoTable> T.update(
    returnValue: Boolean,
    where: DynamoConditionBuilder.() -> DynamoConditionBuilder,
    block: DynamoUpdateBuilder.(getter: DynamoUpdateGetter) -> Unit
): Update {
    // Build the update DSL
    val builder = DynamoUpdateBuilder(this)
    val getter = DynamoUpdateGetter(builder)
    block.invoke(builder, getter)

    // Build the where
    val whereBuilder = DynamoConditionBuilder()
    where.invoke(whereBuilder)
    val result = whereBuilder.buildCondition()


    val ctx = RenderContext(true)
    val updateExpression = builder.render(ctx)

    val conditionExpression = result.conditionExpression?.render(ctx)

    // Build the UpdateItemRequest
    val update = Update.builder()
        .tableName(tableName)
        .key(result.keys)
        .updateExpression(updateExpression)
        .ifNullOrBlankInput(conditionExpression) { conditionExpression(it) }
        .ifNotNullOrEmpty(ctx.values) { expressionAttributeValues(it) }
        .ifNotNullOrEmpty(ctx.attributeNames) { expressionAttributeNames(it) }
        .returnValuesOnConditionCheckFailure(if(returnValue) ReturnValuesOnConditionCheckFailure.ALL_OLD else ReturnValuesOnConditionCheckFailure.NONE)
        .build()

    println(update.prettyString())

    return update
}

fun <T : DynamoTable> T.updateReturnOnFailure(
    where: DynamoConditionBuilder.() -> DynamoConditionBuilder,
    block: DynamoUpdateBuilder.(getter: DynamoUpdateGetter) -> Unit
): Update {
    return update(true, where, block)
}

fun <T : DynamoTable> T.update(
    where: DynamoConditionBuilder.() -> DynamoConditionBuilder,
    block: DynamoUpdateBuilder.(getter: DynamoUpdateGetter) -> Unit
): Update {
    return update(true, where, block)
}


fun <T : DynamoTable> T.deleteReturnOnFailure(
    where: DynamoConditionBuilder.() -> DynamoConditionBuilder
): Delete {
    return delete(true, where)
}

fun <T : DynamoTable> T.delete(
    where: DynamoConditionBuilder.() -> DynamoConditionBuilder
): Delete {
    return delete(false, where)
}





