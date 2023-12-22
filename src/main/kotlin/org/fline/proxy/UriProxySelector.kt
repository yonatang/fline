package org.fline.proxy

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.net.URI

@Component
class UriProxySelector : PPProxySelector {

    private val log = KotlinLogging.logger { }
    private var proxy: PPProxy? = null

    @EventListener
    fun onProxyConfigEvent(event: ProxyConfigEvent) {
        log.debug { "Handling $event" }
        if (event.proxyConfig.proxyType == ProxyType.PROXY) {
            val url = event.proxyConfig.proxy
            proxy = PPProxy(PPType.HTTP, url.host, url.port)
        }
    }

    override fun select(uri: URI): List<PPProxy> {
        return proxy?.let { listOf(it) } ?: listOf()
    }

}
