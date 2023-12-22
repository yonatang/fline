package org.fline.proxy

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension


class WiremockTest {

    @RegisterExtension
    var wm1 = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort().dynamicHttpsPort())
        .build()

    @Test
    fun test_something_with_wiremock() {
        // You can get ports, base URL etc. via WireMockRuntimeInfo
        val wm1RuntimeInfo = wm1.runtimeInfo
        val httpsPort = wm1RuntimeInfo.httpsPort

        // or directly via the extension field
        val httpPort = wm1.port
//        val httpsPort = wm1.httpsPort

        // You can use the DSL directly from the extension field
        wm1.stubFor(get("/api-1-thing").willReturn(ok()))
        println(httpPort)
        println(httpsPort)
    }
}
