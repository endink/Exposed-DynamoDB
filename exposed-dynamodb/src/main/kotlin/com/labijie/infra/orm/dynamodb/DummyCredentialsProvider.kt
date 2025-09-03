package com.labijie.infra.orm.dynamodb

import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.CredentialUtils
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity
import java.util.UUID

/**
 * This file is part of Exposed-DynamoDB project.
 * Copyright (c) 2025
 * @author Anders Xiao
 *
 * File Create: 2025/9/3
 */
object DummyCredentialsProvider : AwsCredentialsProvider {

    val credentials: AwsCredentials

    init {
        val identity = AwsCredentialsIdentity.create(
            UUID.randomUUID().toString().replace("-", ""),
            UUID.randomUUID().toString().replace("-", ""),
        )
        credentials = CredentialUtils.toCredentials(identity)
    }
    override fun resolveCredentials(): AwsCredentials {
        return credentials
    }

    fun AwsCredentialsProvider.isDummy(): Boolean {
        return this is DummyCredentialsProvider
    }
}