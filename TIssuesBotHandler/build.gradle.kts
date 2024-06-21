plugins {
    id("com.google.devtools.ksp") version "2.0.0-1.0.22"
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
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
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