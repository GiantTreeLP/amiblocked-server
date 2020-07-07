package de.gianttree.amiblocked

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.parametersOf
import io.ktor.request.ContentTransformationException
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


val mapper: ObjectMapper = JsonMapper.builder()
    .addModule(KotlinModule())
    .build()

val configPath: Path = Paths.get("config.json")

@KtorExperimentalAPI
fun main() {

    val configuration = if (Files.exists(configPath)) {
        mapper.readValue(configPath.toFile().readBytes(), Configuration::class.java)
    } else {
        Configuration.default()
    }
    mapper.writerWithDefaultPrettyPrinter().writeValue(configPath.toFile(), configuration)

    val databaseConfig = HikariConfig().apply {
        jdbcUrl = configuration.database.jdbcUrl
        driverClassName = configuration.database.driver
        username = configuration.database.username
        password = configuration.database.password
        maximumPoolSize = 2
        minimumIdle = 1
    }
    val database = Database.connect(HikariDataSource(databaseConfig))
    transaction(database) {
        SchemaUtils.createMissingTablesAndColumns(BlockedUsers)
    }
    val server = embeddedServer(CIO, configuration.port, configuration.host) {
        install(CORS) {
            host("blocked-by.gianttree.de", listOf("https"))
        }
        routing {
            post("/api/v1/find") {
                val params = try {
                    this.call.receiveParameters()
                } catch (e: ContentTransformationException) {
                    parametersOf()
                }
                if ("search" !in params) {
                    this.call.respond(HttpStatusCode.BadRequest, "")
                    return@post
                }
                val searchParam = params["search"]!!.trim()

                val response = async(Dispatchers.IO) {
                    mapper.writeValueAsString(
                        newSuspendedTransaction {
                            BlockedUser.find {
                                (BlockedUsers.username eq searchParam) or
                                        (BlockedUsers.snowflake eq searchParam)
                            }.map(::BlockedUserDTO).firstOrNull() ?: BlockedUserDTO(
                                searchParam,
                                "",
                                "",
                                false
                            )
                        }
                    )
                }

                this.call.respondText(response.await(), ContentType.Application.Json)
            }
        }
    }
    server.start(true)
}
