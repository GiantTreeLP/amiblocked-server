package de.gianttree.amiblocked

import com.github.benmanes.caffeine.cache.AsyncLoadingCache
import com.github.benmanes.caffeine.cache.Caffeine
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.*
import io.ktor.http.ContentType.Application
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.cachingheaders.*
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.future.await
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.mapLazy
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.ZonedDateTime
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

val mapper = Json
val prettyMapper = Json(from = mapper) {
    prettyPrint = true
}

val configPath: Path = Paths.get("config.json")

@ExperimentalSerializationApi
fun main() {
    AmIBlockedServer().apply { startServer() }
}

@ExperimentalSerializationApi
class AmIBlockedServer {

    private val configuration = loadConfiguration()

    private val database = loadDatabase()

    private val queryCache = createCache()

    private fun loadConfiguration(): Configuration {
        return if (Files.exists(configPath)) {
            val inputStream = configPath.inputStream()
            inputStream.use {
                mapper.decodeFromStream(inputStream)
            }
        } else {
            Configuration.default()
        }.also {
            val outputStream = configPath.outputStream()
            outputStream.use { stream ->
                prettyMapper.encodeToStream(it, stream)
            }
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
            .expireAfterWrite(12.hours.toJavaDuration())
            .executor(Dispatchers.IO.asExecutor())
            .buildAsync { key ->
                mapper.encodeToString(
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
            install(ContentNegotiation)
            install(CallLogging)
            install(CORS) {
                configuration.cors.forEach {
                    allowHost(it.host, it.schemes)
                }
            }
            install(CachingHeaders) {
                options { _, outgoingContent ->
                    when (outgoingContent.contentType?.withoutParameters()) {
                        Application.Json -> CachingOptions(
                            cacheControl = CacheControl.MaxAge(
                                maxAgeSeconds = 1.days.inWholeSeconds.toInt()
                            ),
                            expires = ZonedDateTime.now().plusDays(1)
                        )

                        else -> null
                    }
                }
            }
            routing {
                post("/api/v1/find") {
                    val params = try {
                        this.call.receiveParameters()
                    } catch (_: ContentTransformationException) {
                        parametersOf()
                    }
                    if ("search" !in params) {
                        this.call.respond(HttpStatusCode.BadRequest)
                        return@post
                    }
                    val searchParam = params["search"]!!.trim()

                    val response = queryCache[searchParam]

                    this.call.respondText(response.await(), Application.Json)
                }
            }
        }
        server.start(true)
    }
}
