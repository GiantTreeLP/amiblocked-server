package de.gianttree.amiblocked

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.benmanes.caffeine.cache.AsyncLoadingCache
import com.github.benmanes.caffeine.cache.Caffeine
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.request.ContentTransformationException
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.future.await
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.mapLazy
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.toJavaDuration


val mapper: ObjectMapper = jacksonObjectMapper()

val configPath: Path = Paths.get("config.json")

@ExperimentalTime
fun main() {
    AmIBlockedServer().apply { startServer() }
}

@ExperimentalTime
class AmIBlockedServer {

    private val configuration = loadConfiguration()

    private val database = loadDatabase()

    private val queryCache = createCache()

    private fun loadConfiguration(): Configuration {
        return if (Files.exists(configPath)) {
            mapper.readValue(configPath.toFile().readBytes(), Configuration::class.java)
        } else {
            Configuration.default()
        }.also {
            mapper.writerWithDefaultPrettyPrinter().writeValue(configPath.toFile(), it)
        }
    }

    private fun loadDatabase(): Database {
        val databaseConfig = HikariConfig().apply {
            jdbcUrl = configuration.database.jdbcUrl
            driverClassName = configuration.database.driver
            username = configuration.database.username
            password = configuration.database.password
            maximumPoolSize = 2
            minimumIdle = 1
        }
        return Database.connect(HikariDataSource(databaseConfig)).also {
            transaction(it) {
                SchemaUtils.createMissingTablesAndColumns(BlockedUsers)
            }

        }
    }

    private fun createCache(): AsyncLoadingCache<String, String> {
        return Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.hours(12).toJavaDuration())
            .executor(Dispatchers.IO.asExecutor())
            .buildAsync { key ->
                mapper.writeValueAsString(
                    transaction(database) {
                        BlockedUser.find {
                            (BlockedUsers.username eq key) or                       // Full match
                                    //language=RegExp
                                    (BlockedUsers.username regexp "^${Regex.escape(key)}#\\d{4}$") or // Match without discriminator
                                    (BlockedUsers.snowflake eq key)                 // Match snowflake
                        }.mapLazy(::BlockedUserDTO).singleOrNull()            // Make sure, only one result is found
                            ?: BlockedUserDTO.noResult(key)
                    }
                )
            }
    }

    fun startServer() {
        val server = embeddedServer(CIO, configuration.port, configuration.host) {
            install(CallLogging)
            install(CORS) {
                configuration.cors.forEach {
                    host(it.host, it.schemes)
                }
            }
            install(CachingHeaders) {
                options { outgoingContent ->
                    when (outgoingContent.contentType?.withoutParameters()) {
                        ContentType.Application.Json -> CachingOptions(
                            CacheControl.MaxAge(
                                maxAgeSeconds = Duration.days(1)
                                    .inWholeSeconds.toInt()
                            )
                        )
                        else -> null
                    }
                }
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

                    val response = queryCache[searchParam]

                    this.call.respondText(response.await(), ContentType.Application.Json)
                }
            }
        }
        server.start(true)
    }
}
