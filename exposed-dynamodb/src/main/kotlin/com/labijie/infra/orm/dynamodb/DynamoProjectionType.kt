/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb


enum class DynamoProjectionType {
    ALL,
    TableOnly,
    KeyOnly,
    TableWithoutKeys
}