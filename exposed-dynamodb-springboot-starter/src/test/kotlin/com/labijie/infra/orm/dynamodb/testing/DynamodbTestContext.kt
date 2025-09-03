/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 *
 * File Create: 2025-09-03
 */

package com.labijie.infra.orm.dynamodb.testing

import com.labijie.infra.orm.dynamodb.configuration.ExposedDynamodbAutoConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Configuration


@Configuration(proxyBeanMethods = false)
@ImportAutoConfiguration(ExposedDynamodbAutoConfiguration::class)
class DynamodbTestContext: ApplicationContextAware {

    private lateinit var context: ApplicationContext

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.context = applicationContext
    }
}