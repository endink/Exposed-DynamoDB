rootProject.name = "exposed-dynamodb-root"

pluginManagement {

    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

include("exposed-dynamodb")
include("exposed-dynamodb-springboot-starter")