package org.fline.proxy

import java.net.URI

interface PPProxySelector {

    fun select(uri: URI): List<PPProxy>
}

data class PPProxy(val type: PPType, val host: String, val port: Int) {
    companion object {
        val NO_PROXY = PPProxy(PPType.DIRECT, "", 0)
    }
}

enum class PPType {
    DIRECT, HTTP, SOCKS
}
