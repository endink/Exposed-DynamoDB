dependencies {
    compileOnly("software.amazon.awssdk:dynamodb")
    testImplementation("software.amazon.awssdk:dynamodb")
    api(platform("software.amazon.awssdk:bom:${Versions.awsSdk}"))

    compileOnly("org.graalvm.nativeimage:svm:${Versions.graalvmSvm}")
    compileOnly("io.github.classgraph:classgraph:${Versions.classgraph}")
}