dependencies {
//    implementation("cglib:cglib:${Versions.cglib}")
    api(project(":exposed-dynamodb"))
    api("org.springframework.boot:spring-boot-starter")
    compileOnly("software.amazon.awssdk:dynamodb")

    testImplementation("software.amazon.awssdk:dynamodb")
    testImplementation(project(":exposed-dynamodb-springboot-starter"))

}