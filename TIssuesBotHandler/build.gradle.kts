plugins {
    id("com.google.devtools.ksp") version "2.0.0-1.0.22"
    id("com.github.johnrengelman.shadow") version "8.1.1" // Убедитесь, что версия актуальна
    kotlin("plugin.serialization") version "2.0.0"
    kotlin("jvm") version "2.0.0"
    application
}

group = "suhov.vitaly"
version = ""

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(kotlin("stdlib"))
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

application {
    mainClass.set("suhov.vitaly.MainKt")  // Укажите путь к вашему основному классу
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "suhov.vitaly.MainKt"  // Это добавляет основную секцию в манифест
    }
}

tasks {
    shadowJar {
        archiveClassifier.set("")  // Убедитесь, что результат будет my-project.jar без дополнительного суффикса
        manifest {
            attributes["Main-Class"] = "suhov.vitaly.MainKt"  // Укажите путь к вашему основному классу
        }
    }
}