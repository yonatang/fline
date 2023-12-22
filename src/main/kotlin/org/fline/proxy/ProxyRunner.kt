package org.fline.proxy

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.network.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationContext
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.net.Socket
import java.net.URI
import java.util.Base64
import java.util.concurrent.atomic.AtomicInteger
import javax.annotation.PreDestroy

@Component
class CoroutineProxyRunner(
    @Qualifier("proxyProxySelector")
    private val proxySelector: PPProxySelector,
    private val metrics: Metrics,
) {
    var port: Int = -1
    private val log = KotlinLogging.logger { }
    private var toRun = true
    private lateinit var job: Job
    private lateinit var thread: Thread

    private lateinit var config: ProxyConfig
    private var authHeader = ""
    private var existingPassword=""

    private val counter = AtomicInteger(0)

    fun run(port: Int = 8889) {
        thread = Thread {
            runBlocking {
                log.info { "Starting server" }
                job = launch(Dispatchers.Default) {
                    start(port)
                }
                log.info { "Server started" }
            }
        }.apply {
            isDaemon = true
            start()
        }
    }


    @EventListener
    @Async
    fun onProxyConfigEvent(event: ProxyConfigEvent) {
        log.debug { "Handle $event" }
        config = event.proxyConfig
        val newPassword = config.password.takeIf { it.isNotEmpty() } ?: existingPassword
        val b64 = Base64.getEncoder()
            .encodeToString("${config.username}:$newPassword".toByteArray())
        authHeader = "Proxy-Authorization: Basic $b64"

        if (!this::job.isInitialized) {
            run()
            log.info { "Server initiation done" }
        }
    }

    @PreDestroy
    fun stop() {
        log.info { "Stopping server" }
        toRun = false
        job.cancel()
        thread.interrupt()
        log.info { "Server stopeed" }
    }

    private suspend fun start(toBindPort: Int) = coroutineScope {
        log.debug { "Binding port $toBindPort" }
        ActorSelectorManager(Dispatchers.IO).use { manager ->
            aSocket(manager).tcp().bind(InetSocketAddress("127.0.0.1", toBindPort)).use { server ->
                log.info { "Server bounded on port ${server.localAddress.toJavaAddress().port}" }
                port = server.localAddress.toJavaAddress().port
                while (toRun) {
                    var socket: ASocket? = null
                    try {
                        log.debug { "Waiting for a socket" }
                        socket = server.accept()
                        metrics.newConnection()
                        log.debug { "Socket accepted from ${socket.remoteAddress} to ${socket.localAddress}" }
                        launch(Dispatchers.IO) {
                            handle(socket)
                        }.invokeOnCompletion { err ->
                            socket.close()
                            metrics.closeConnection()
                            err
                                ?.also { log.debug(err) { "Error while handling socket" } }
                                ?: log.debug { "Request handled" }
                        }
                    } catch (ex: Exception) {
                        socket?.close()
                        log.error(ex) { "Error while accepting socket" }
                    }
                }
            }
        }
    }


    data class HostAndPort(val method: String, val host: String, val port: Int, val uri: URI)

    fun extractHostPort(line: String): HostAndPort {
        val (method, target) = line.split(' ')
        return when (method) {
            "CONNECT" -> {
                val (host, port) = target.split(':')
                HostAndPort(method, host, port.toInt(), URI("https://$host:$port"))
            }
            else -> {
                val uri = URI(target)
                val port = when {
                    uri.port > 0 -> uri.port
                    uri.scheme == "http" -> 80
                    uri.scheme == "https" -> 443
                    else -> 80
                }
                HostAndPort(method, uri.host, port, URI("${uri.scheme}://${uri.host}:$port"))
            }
        }
    }


    suspend fun handle(clientSocket: io.ktor.network.sockets.Socket) = coroutineScope {
        log.debug { "Handling a socket from ${clientSocket.remoteAddress} to ${clientSocket.localAddress}" }
        try {
//            clientSocket.connection().input
            val fromClient = clientSocket.openReadChannel()

            val firstLine = fromClient.readUTF8Line() ?: return@coroutineScope
            log.debug { "First line: $firstLine" }
            val (_, host, port, uri) = extractHostPort(firstLine)
            val proxy = proxySelector.select(uri).random()
            val isDirect = proxy.type == PPType.DIRECT
            val remoteAddress = if (isDirect) {
                log.debug { "Direct connection to $host:$port" }
                InetSocketAddress(host, port)
            } else {
                log.debug { "Proxy connection through $proxy to $host:$port" }
                InetSocketAddress(proxy.host, proxy.port)
            }
            ActorSelectorManager(Dispatchers.IO).use { manager ->
                aSocket(manager).tcp().connect(remoteAddress).use { remoteSocket ->
                    log.debug { "Connect to remote $remoteAddress" }
                    val toClient = clientSocket.openWriteChannel(autoFlush = false)

                    val fromRemote = remoteSocket.openReadChannel()
                    val toRemote = remoteSocket.openWriteChannel(autoFlush = false)

                    val channels = ClientRemoteChannels(
                        toClient = toClient, fromClient = fromClient,
                        toRemote = toRemote, fromRemote = fromRemote
                    )
                    val handler = if (isDirect) {
                        DirectHandler(channels, firstLine)
                    } else {
                        ProxyHandler(channels, firstLine, authHeader)
                    }
                    handler.handle()
                }
            }
        } catch (ex: Exception) {
            log.debug(ex) { "Error handling request" }
        }
    }

}


data class ProxyRequestEvent(val count: Int)
data class ProxyConfigEvent(val proxyConfig: ProxyConfig)


data class ProxyConfig(
    val proxy: URI,
    val username: String,
    val password: String,
    val proxyType: ProxyType,
)

enum class ProxyType {
    PAC, PROXY, DIRECT
}
