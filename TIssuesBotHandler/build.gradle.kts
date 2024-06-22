plugins {
    id("com.google.devtools.ksp") version "2.0.0-1.0.22"
    kotlin("plugin.serialization") version "2.0.0"
    kotlin("jvm") version "2.0.0"
}

group = "suhov.vitaly"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("eu.vendeli:telegram-bot:6.1.0")
    implementation("com.google.devtools.ksp:symbol-processing-api:2.0.0-1.0.21")
    ksp("eu.vendeli:ksp:6.1.0")

    //Сериализация/десереализация
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0")
    //Асинхронщина
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    implementation("org.apache.logging.log4j:log4j-api:2.19.0")
    implementation("org.apache.logging.log4j:log4j-core:2.19.0")

    implementation ("com.google.api-client:google-api-client:2.0.0")
    implementation ("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
    implementation ("com.google.apis:google-api-services-sheets:v4-rev20220927-2.0.0")

}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(20)
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}