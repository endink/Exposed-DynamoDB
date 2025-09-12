/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb.exception


class DynamoNameFormatException(name: String) :
    ExposedDynamoDbException("Invalid DynamoDB name: '$name'. " +
            "Table or index name must be 3-255 characters, attribute name must be 1-255 characters and contain only letters, digits, '_', '-', or '.'")