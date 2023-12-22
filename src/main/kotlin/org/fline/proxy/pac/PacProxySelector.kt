package org.fline.proxy.pac

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.fline.proxy.PPProxy
import org.fline.proxy.PPProxySelector
import org.fline.proxy.PPType
import org.fline.proxy.ProxyConfigEvent
import org.fline.proxy.ProxyType
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.util.concurrent.ConcurrentHashMap

@Component
class PacProxySelector : PPProxySelector {
    private val log = KotlinLogging.logger { }
    fun setPacUri(uri: URI) {
        pacUri = uri
        runBlocking {
            launch { refresh() }
        }
    }

    @EventListener
    fun onProxyConfigEvent(event: ProxyConfigEvent) {
        log.debug { "Handling $event" }
        if (event.proxyConfig.proxyType == ProxyType.PAC) {
            setPacUri(event.proxyConfig.proxy)
        }
    }

    private var pacUri: URI? = null
    private var pacScriptSource: PacScriptSource? = null
    private lateinit var pacScriptParser: PacScriptParser
    private val httpClient = HttpClient.newHttpClient()
    private var lastRefreshTime: Long? = null
    suspend fun refresh() = coroutineScope {
        log.debug { "Starting to refresh PAC file" }
        val getRequest = HttpRequest.newBuilder(pacUri).GET().build()
        val response = httpClient.send(getRequest, BodyHandlers.ofString())
        if (response.statusCode() == 200) {
            val content = response.body()
            pacScriptParser = JavaxPacScriptParser(StaticPacScriptSource(content, true))
            lastRefreshTime = System.currentTimeMillis()
        }
        log.debug { "Refreshed successfully" }
    }

    val cache = ConcurrentHashMap<URI, List<PPProxy>>()

    override fun select(uri: URI): List<PPProxy> {
//        return cache.getOrPut(uri) {
//            println("!!!Resolved $uri !!!")
        val response = pacScriptParser.evaluate(uri.toASCIIString(), uri.host)
        log.trace { "For URI $uri picked $response" }
        return response.split(';')
            .map { proxyDirectiveReader(it.trim()) }
//        }
    }

    private fun proxyDirectiveReader(directive: String): PPProxy {
        if (directive.length < 6) {
            return PPProxy.NO_PROXY
        }
        val type = when (directive.substring(0, 5).uppercase()) {
            "PROXY" -> PPType.HTTP
            "SOCKS" -> PPType.SOCKS
            else -> return PPProxy.NO_PROXY
        }
        val target = directive.substring(6)
        val portIdx = target.indexOf(':')
        return if (portIdx != -1) {
            PPProxy(
                type,
                target.substring(0, portIdx),
                target.substring(portIdx + 1).trim().toInt()
            )
        } else {
            PPProxy(type, target, 80)
        }
    }

}
