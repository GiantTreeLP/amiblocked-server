plugins {
    val kotlinVersion = "2.0.0"

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

    val ktorVersion = "2.3.12"
    val kotlinxSerializationVersion = "1.7.0"
    val exposedVersion = "0.51.1"
    val slf4jVersion = "2.0.13"
    val mariadbVersion = "3.4.0"
    val hikaricpVersion = "5.1.0"
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

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "17"
    }
    withType<Jar> {
        manifest {
            attributes(
                mapOf(
                    "Main-Class" to application.mainClass
                )
            )
        }
    }
}
