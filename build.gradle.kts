import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    kotlin("jvm") version "2.0.20"
}

application.mainClass.set("me.melijn.siteapi.MelijnSiteKt")
group = "me.melijn.siteapi"
version = "1.0.0"

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

val ktx = "1.9.0"
val ktor = "2.3.12"
val jackson = "2.18.0"
val kotlin = "2.0.20"

dependencies {
    // https://mvnrepository.com/artifact/org.springframework.security/spring-security-web
    implementation("org.springframework.security:spring-security-web:6.3.3")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$ktx")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-jdk8
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$ktx")

    // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
    implementation("ch.qos.logback:logback-classic:1.5.9")

    // https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-core
    implementation("com.fasterxml.jackson.core:jackson-core:$jackson")

    // https://github.com/FasterXML/jackson-module-kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jackson")

    // https://mvnrepository.com/artifact/io.ktor/ktor-client-cio
    implementation("io.ktor:ktor:$ktor")
    implementation("io.ktor:ktor-client-okhttp:$ktor")
    implementation("io.ktor:ktor-client-jackson:$ktor")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor")
    implementation("io.ktor:ktor-server-netty:$ktor")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor")
    implementation("io.ktor:ktor-serialization-jackson:$ktor")


    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-stdlib-jdk8
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin")

    // https://github.com/jwtk/jjwt
    // https://mvnrepository.com/artifact/io.jsonwebtoken/jjwt-impl
    implementation("io.jsonwebtoken:jjwt-impl:0.11.2")
    implementation("io.jsonwebtoken:jjwt-jackson:0.11.2")

    // https://search.maven.org/artifact/com.zaxxer/HikariCP
    implementation("com.zaxxer:HikariCP:5.1.0")

    // implementation("com.github.husnjak:IGDB-API-JVM:0.7")
    // https://mvnrepository.com/artifact/io.lettuce/lettuce-core
    implementation("io.lettuce:lettuce-core:6.1.8.RELEASE")

    // https://github.com/cdimascio/dotenv-kotlin
    implementation("io.github.cdimascio:dotenv-kotlin:6.2.2")
}

tasks {
    withType(JavaCompile::class) {
        options.encoding = "UTF-8"
    }
    withType(KotlinCompile::class) {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }
    }

    shadowJar {
        archiveFileName.set("melijn-backend.jar")
    }
}
