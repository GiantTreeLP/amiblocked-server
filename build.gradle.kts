import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "2.0.21"

    java
    application
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

application {
    mainClass.set("de.gianttree.amiblocked.AmIBlockedServerKt")
}

dependencies {

    val ktorVersion = "3.0.0"
    val kotlinxSerializationVersion = "1.7.3"
    val exposedVersion = "0.55.0"
    val slf4jVersion = "2.0.16"
    val mariadbVersion = "3.4.1"
    val hikaricpVersion = "6.0.0"
    val caffeineVersion = "3.1.8"

    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation("org.jetbrains.kotlinx", "kotlinx-serialization-json", kotlinxSerializationVersion)

    implementation("io.ktor", "ktor-server-cio", ktorVersion)
    implementation("io.ktor", "ktor-server-content-negotiation", ktorVersion)
    implementation("io.ktor", "ktor-serialization-kotlinx-json", ktorVersion)
    implementation("io.ktor", "ktor-server-call-logging", ktorVersion)
    implementation("io.ktor", "ktor-server-cors", ktorVersion)
    implementation("io.ktor", "ktor-server-caching-headers", ktorVersion)

    implementation("org.jetbrains.exposed", "exposed-core", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-dao", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-jdbc", exposedVersion)

    implementation("org.slf4j", "slf4j-simple", slf4jVersion)
    implementation("org.mariadb.jdbc", "mariadb-java-client", mariadbVersion)
    implementation("com.zaxxer", "HikariCP", hikaricpVersion)
    implementation("com.github.ben-manes.caffeine", "caffeine", caffeineVersion)
}

tasks.withType<JavaCompile>() {
    targetCompatibility = JavaVersion.VERSION_21.toString()
}

tasks.withType<KotlinCompile>() {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}
