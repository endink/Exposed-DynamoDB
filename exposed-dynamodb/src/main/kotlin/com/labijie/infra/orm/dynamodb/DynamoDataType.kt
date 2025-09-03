/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb


object DynamoDataType {
    const val STRING = "S"
    const val STRING_SET = "SS"
    const val NUMBER = "N"
    const val NUMBER_SET = "NS"
    const val BINARY = "B"
    const val BINARY_SET = "BS"

    const val BOOLEAN = "BOOL"
    const val NULL = "NULL"

    const val LIST = "L"
    const val MAP = "M"
}