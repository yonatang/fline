package org.fline.proxy

import io.github.oshai.kotlinlogging.KotlinLogging
import org.fline.proxy.pac.PacProxySelector
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.net.URI

@Component
class ProxyProxySelector(
    val pacProxySelector: PacProxySelector,
    val uriProxySelector: UriProxySelector,
    val directProxySelector: DirectProxySelector,
) : PPProxySelector {

    private val log = KotlinLogging.logger { }
    private var proxyType: ProxyType = ProxyType.DIRECT

    @EventListener
    fun onProxyConfigEvent(event: ProxyConfigEvent) {
        log.debug { "Handling $event" }
        proxyType = event.proxyConfig.proxyType
    }

    override fun select(uri: URI): List<PPProxy> {
        return when (proxyType) {
            ProxyType.DIRECT -> directProxySelector.select(uri)
            ProxyType.PROXY -> uriProxySelector.select(uri)
            ProxyType.PAC -> pacProxySelector.select(uri)
        }
    }
}
