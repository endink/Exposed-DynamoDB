/**
 * This file is part of Exposed-DynamoDB project .
 * Copyright (c) 2025
 * @author Anders Xiao
 * 
 * File Create: 2025-09-03
 */


package com.labijie.infra.orm.dynamodb.configuration

import com.labijie.infra.orm.dynamodb.DummyCredentialsProvider
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.http.SdkHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import java.net.URI


@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DynamoDbProperties::class)
class DynamodbAutoConfiguration {

    companion object {
        private val logger by lazy {
            LoggerFactory.getLogger(DynamodbAutoConfiguration::class.java)
        }
    }

    @Bean
    @ConditionalOnMissingBean(DynamoDbClient::class)
    fun dynamoDbClient(
        @Autowired(required = false)
        awsCredentials: AwsCredentialsProvider? = null,
        @Autowired(required = false)
        sdkHttpClient: SdkHttpClient? = null,
        properties: DynamoDbProperties,
        @Autowired(required = false)
        awsRegion: Region? = null
    ): DynamoDbClient {

        val regionConfigured = properties.region

        val region = if(!regionConfigured.isNullOrBlank()) {
            Region.of(regionConfigured)
        }else {
            awsRegion ?: Region.of("us-west-1")
        }

        return if (properties.useLocal) {
            val endpointOverride = properties.endpointOverride
            if (endpointOverride == null || !endpointOverride.isAbsolute) {
                logger.warn("Local dynamodb enabled, but endpoint-override not configured or invalid, use default: 'http://localhost:8000' .")
            }
            val uri = endpointOverride ?: URI.create("http://localhost:8000")
            DynamoDbClient.builder()
                .endpointOverride(uri) // 本地 DynamoDB 地址
                .credentialsProvider(DummyCredentialsProvider)
                .region(region)
                .let {
                    sdkHttpClient?.let { client -> it.httpClient(client) } ?: it
                }
                .build()

        } else {
            DynamoDbClient.builder()
                .endpointOverride(URI.create("http://localhost:8000")) // 本地 DynamoDB 地址
                .credentialsProvider(awsCredentials ?: DummyCredentialsProvider)
                .region(region)
                .let {
                    properties.endpointOverride?.let { endpointOverride -> it.endpointOverride(endpointOverride) } ?: it
                }
                .defaultsMode(properties.mode)
                .let {
                    sdkHttpClient?.let { client -> it.httpClient(client) } ?: it
                }
                .build()
        }
    }

}