/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb.configuration

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered


@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ExposedDynamoDbProperties::class)
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class ExposedDynamodbAutoConfiguration {

    companion object {
        private val logger by lazy {
            LoggerFactory.getLogger(ExposedDynamodbAutoConfiguration::class.java)
        }
    }
}