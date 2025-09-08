/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb

import com.labijie.infra.orm.dynamodb.builder.*
import software.amazon.awssdk.services.dynamodb.model.ConditionCheck
import software.amazon.awssdk.services.dynamodb.model.Put


fun <T : DynamoTable<PK, SK>, PK, SK> T.batchGet(build:  DynamoBatchGetBuilder<PK, SK>.Context.()-> Unit?): DynamoBatchGetBuilder<PK, SK> {
    val builder = DynamoBatchGetBuilder(this)
    build.invoke(builder.context)
    return builder
}

fun <T : DynamoTable<PK, SK>, PK, SK> T.get(projectType: DynamoProjectionType = DynamoProjectionType.TableOnly): DynamoGetBuilder<PK, SK> {
    val builder = DynamoGetBuilder(this)
    val projection = builder.projection()
    when (projectType) {
        DynamoProjectionType.ALL -> projection.projectAll()
        DynamoProjectionType.TableOnly -> projection.project(this)
        DynamoProjectionType.KeyOnly -> projection.projectKeyOnly()
        DynamoProjectionType.TableWithoutKeys -> projection.projectTableWithoutKeys()
    }
    return builder
}

fun <T : DynamoTable<PK, SK>, PK, SK> T.get(projection: ProjectionBaseBuilder.ProjectionBuilder.()-> Unit): DynamoGetBuilder<PK, SK> {
    val builder = DynamoGetBuilder(this)
    val instance = builder.projection()
    projection.invoke(instance)
    return builder
}

fun <T : DynamoTable<PK, SK>, PK, SK> T.query(projectType: DynamoProjectionType = DynamoProjectionType.TableOnly): DynamoQueryBuilder<PK, SK> {
    val builder = DynamoQueryBuilder(this)
    val projection = builder.projection()
    when (projectType) {
        DynamoProjectionType.ALL -> projection.projectAll()
        DynamoProjectionType.TableOnly -> projection.project(this)
        DynamoProjectionType.KeyOnly -> projection.projectKeyOnly()
        DynamoProjectionType.TableWithoutKeys -> projection.projectTableWithoutKeys()
    }
    return builder
}

fun <T : DynamoTable<PK, SK>, PK, SK> T.query(projection: ProjectionBaseBuilder.ProjectionBuilder.()-> Unit): DynamoQueryBuilder<PK, SK> {
    val builder = DynamoQueryBuilder(this)
    val instance = builder.projection()
    projection.invoke(instance)
    return builder
}

fun <T : DynamoTable<PK, SK>, PK, SK> T.putIfNotExist(
    block: T.(dsl: DynamoPutBuilder<PK, SK>.DynamoUpdateSetter) -> Unit
): Put {
    val pk = this.primaryKey.partitionKey
    val sk = this.primaryKey.sortKey

    val dsl = DynamoPutBuilder(this)
    dsl.condition {
        pk.getColumn().notExists().andIfNotNull(sk) {
            it.getColumn().notExists()
        }
    }
    block.invoke(this, dsl.setter)

    return dsl.build()
}

fun <T : DynamoTable<PK, SK>, PK, SK> T.put(
    block: T.(dsl: DynamoPutBuilder<PK, SK>.DynamoUpdateSetter) -> Unit
): DynamoPutBuilder<PK, SK> {
    val dsl = DynamoPutBuilder(this)
    block.invoke(this, dsl.setter)

    return dsl
}


fun <T: DynamoTable<PK, SK>, PK, SK> T.check(where: DynamoConditionBuilder<PK, SK>.() -> DynamoConditionBuilder<PK, SK>): ConditionCheck {
    val builder = DynamoConditionBuilder<PK, SK>(this)
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


fun <T: DynamoTable<PK, SK>, PK, SK> T.delete(
    where: DynamoConditionBuilder<PK, SK>.() -> DynamoConditionBuilder<PK, SK>
): DynamoDeleteBuilder<PK, SK> {
    val builder = DynamoDeleteBuilder(this)
    where.invoke(builder.condition)
    return builder
}

fun <T: DynamoTable<PK, SK>, PK, SK> T.update(
    where: DynamoConditionBuilder<PK, SK>.() -> DynamoConditionBuilder<PK, SK>,
    block: DynamoUpdateBuilder<PK, SK>.DynamoSegmentsBuilder.(getter: DynamoUpdateGetter<PK, SK>) -> Unit
): DynamoUpdateBuilder<PK, SK> {

    val builder = DynamoUpdateBuilder(this)

    val getter = DynamoUpdateGetter(builder.segments)
    block.invoke(builder.segments, getter)
    // Build the where
    where.invoke(builder.condition)

    return builder
}





