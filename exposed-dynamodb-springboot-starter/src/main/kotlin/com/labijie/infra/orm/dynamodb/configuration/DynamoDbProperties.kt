/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode
import java.net.URI


@ConfigurationProperties("infra.exposed.dynamodb")
class DynamoDbProperties {
    var useLocal: Boolean = false
    var endpointOverride: URI? = null
    var region: String? = null
    var mode: DefaultsMode = DefaultsMode.IN_REGION
}