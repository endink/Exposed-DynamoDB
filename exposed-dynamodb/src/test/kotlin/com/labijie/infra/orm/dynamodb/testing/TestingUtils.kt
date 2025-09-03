/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb.testing

import com.labijie.infra.orm.dynamodb.DummyCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import java.net.URI


object TestingUtils {
    val client: DynamoDbClient by lazy {
        DynamoDbClient.builder()
            .endpointOverride(URI.create("http://localhost:8001"))
            .credentialsProvider(DummyCredentialsProvider)
            .region(Region.of("us-east-1"))
            .build()
    }
}