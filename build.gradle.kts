plugins {
    //id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
    id("com.labijie.infra") version Versions.infraPlugin
}

allprojects {

    group = "com.labijie.orm"
    version = "1.0.0-SNAPSHOT"

    infra {
        useDefault {
            includeSource = true
            includeDocument = true
            useMavenProxy = false
        }
    }
}



subprojects {
    infra {
        if (!project.name.startsWith("dummy")) {
            publishing(true) {
                pom {
                    description = "Type-safe kotlin DSL query syntax** for aws DynamoDB."
                    githubUrl("endink", "Exposed-DynamoDB")
                    developer("AndersXiao", "sharping@outlook.com")
                }
                toGithubPackages("endink", "exposed-dynamodb")
            }
        }
    }

}
