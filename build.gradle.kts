plugins {
    java
    application
    kotlin("jvm") version "1.3.72"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

application {
    mainClassName = "de.gianttree.amiblocked.AmIBlockedServerKt"
}

dependencies {

    val ktorVersion = "1.5.3"
    val exposedVersion = "0.31.1"
    val slf4jVersion = "1.7.30"
    val mariadbVersion = "2.7.2"
    val hikaricpVersion = "4.0.3"
    val caffeineVersion = "3.0.1"

    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation("io.ktor", "ktor-server-cio", ktorVersion)
    implementation("io.ktor", "ktor-jackson", ktorVersion)

    implementation("org.jetbrains.exposed", "exposed-core", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-dao", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-jdbc", exposedVersion)

    implementation("org.slf4j", "slf4j-simple", slf4jVersion)
    implementation("org.mariadb.jdbc", "mariadb-java-client", mariadbVersion)
    implementation("com.zaxxer", "HikariCP", hikaricpVersion)
    implementation("com.github.ben-manes.caffeine", "caffeine", caffeineVersion)
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    withType<Jar> {
        manifest {
            attributes(
                mapOf(
                    "Main-Class" to application.mainClassName
                )
            )
        }
    }
}
