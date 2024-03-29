package de.gianttree.amiblocked

import kotlinx.serialization.Serializable

@Serializable
data class Configuration(
    val host: String,
    val port: Int,
    val database: DatabaseConfiguration,
    val cors: List<CorsConfiguration>
) {
    companion object {
        fun default() = Configuration(
            "0.0.0.0",
            8080,
            DatabaseConfiguration.default(),
            listOf(CorsConfiguration.default())
        )
    }
}

@Serializable
data class DatabaseConfiguration(
    val jdbcUrl: String,
    val driver: String,
    val username: String,
    val password: String
) {
    companion object {
        fun default() = DatabaseConfiguration(
            "jdbc:mariadb://localhost:3006/amiblocked",
            "org.mariadb.jdbc.Driver",
            "root",
            ""
        )
    }
}

@Serializable
data class CorsConfiguration(
    val host: String,
    val schemes: List<String>
) {
    companion object {
        fun default() = CorsConfiguration(
            "localhost",
            listOf("http")
        )
    }
}
