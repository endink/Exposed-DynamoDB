/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb


// ----------------- Column / Value Expressions -----------------
internal fun <T> DynamoColumn<T>.colExpr() = ColumnExpr(this)
internal fun <T: Any> T?.valueExpr(column: DynamoColumn<*>? = null) = ValueExpr(this, column)
internal fun <T: Any> T.constantExpr() = ConstantExpr(this.toString())

