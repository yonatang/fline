package org.fline.proxy

import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.net.URI

@Component
class DirectProxySelector : PPProxySelector {
    private val noProxy = listOf(PPProxy.NO_PROXY)

    override fun select(uri: URI): List<PPProxy> {
        return noProxy
    }
}
