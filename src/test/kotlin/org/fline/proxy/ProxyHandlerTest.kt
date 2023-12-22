package org.fline.proxy

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import io.kotest.matchers.shouldBe
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpObject
import io.netty.handler.codec.http.HttpRequest as ProxyHttpRequest
import io.netty.handler.codec.http.HttpResponse
import org.awaitility.Awaitility
import org.awaitility.Awaitility.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.littleshoot.proxy.HttpFilters
import org.littleshoot.proxy.HttpFiltersAdapter
import org.littleshoot.proxy.HttpFiltersSource
import org.littleshoot.proxy.HttpProxyServer
import org.littleshoot.proxy.ProxyAuthenticator
import org.littleshoot.proxy.impl.DefaultHttpProxyServer
import java.net.InetSocketAddress
import java.net.ProxySelector
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.time.Duration

class ProxyHandlerTest {
    companion object {
        init {
            System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true")
        }

        lateinit var proxy: HttpProxyServer

        @AfterAll
        @JvmStatic
        fun stopProxy() {
            proxy.abort()
        }

        @BeforeAll
        @JvmStatic
        fun startProxy() {
            proxy = DefaultHttpProxyServer.bootstrap().withPort(0)
                .withProxyAuthenticator(object : ProxyAuthenticator {
                    override fun authenticate(userName: String, password: String): Boolean {
                        return userName == "foo" && password == "bar"
                    }

                    override fun getRealm(): String {
                        return "Test Proxy"
                    }

                })
                .withFiltersSource(object : HttpFiltersSource {
                    override fun filterRequest(
                        originalRequest: ProxyHttpRequest,
                        ctx: ChannelHandlerContext
                    ): HttpFilters {
                        return object : HttpFiltersAdapter(originalRequest) {
                            override fun clientToProxyRequest(httpObject: HttpObject?): HttpResponse? {
                                return null
                            }

                            override fun serverToProxyResponse(httpObject: HttpObject?): HttpObject {
                                return super.serverToProxyResponse(httpObject)
                            }
                        }
                    }

                    override fun getMaximumRequestBufferSizeInBytes() = 0

                    override fun getMaximumResponseBufferSizeInBytes() = 0

                })
                .start()
        }
    }

    lateinit var runner: CoroutineProxyRunner
    lateinit var client: HttpClient
    lateinit var baseHttp: String
    lateinit var baseHttps: String

//    lateinit var proxy: HttpProxyServer

    @RegisterExtension
    val wm = WireMockExtension.newInstance()
        .options(WireMockConfiguration.wireMockConfig().dynamicPort().dynamicHttpsPort())
        .build()

    @BeforeEach
    fun init() {
        val proxySelector = UriProxySelector().apply {
            val host = proxy.listenAddress.hostString
            val port = proxy.listenAddress.port
            val event =
                ProxyConfigEvent(
                    ProxyConfig(
                        URI("http://$host:$port"),
                        "foo",
                        "bar",
                        ProxyType.PROXY
                    )
                )
            onProxyConfigEvent(event)
        }
        runner = CoroutineProxyRunner(proxySelector, org.fline.proxy.Metrics())

        runner.run(0)
        await().until { runner.port > 0 }

        //TODO this is so lame!!!
        val host = proxy.listenAddress.hostString
        val port = proxy.listenAddress.port
        val event =
            ProxyConfigEvent(ProxyConfig(URI("http://$host:$port"), "foo", "bar", ProxyType.PROXY))
        runner.onProxyConfigEvent(event)

        client = HttpClient.newBuilder()
            .sslContext(insecureSsl())
            .connectTimeout(Duration.ofMillis(50))
            .proxy(ProxySelector.of(InetSocketAddress.createUnresolved("localhost", runner.port)))
            .build()
        baseHttp = "http://localhost:${wm.port}"
        baseHttps = "https://localhost:${wm.httpsPort}"
    }

    @AfterEach
    fun stop() {
        runner.stop()
    }

    @Test
    fun `should work when connecting through double proxy http`() {
        wm.stubFor(WireMock.get("/").willReturn(WireMock.ok("nonsecure double proxy!")))
        val req = HttpRequest.newBuilder(URI("$baseHttp/")).GET().build()
        val res = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString())
        println(res.body())
        res.body() shouldBe "nonsecure double proxy!"
    }
//
//    @Test
//    fun `just pause`(){
//        wm.stubFor(WireMock.get("/").willReturn(WireMock.ok("proxy proxy java java")))
//        println("Http server: $baseHttp")
//        println("Https server: $baseHttp")
//        println("Http proxy: ${proxy.listenAddress}")
//        Thread.sleep(60_000_000)
//    }


    @Test
    fun `should work when connecting through double proxy https`() {
        wm.stubFor(WireMock.get("/").willReturn(WireMock.ok("secure double proxy!")))
        val req = HttpRequest.newBuilder(URI("$baseHttps/")).GET().build()
        val res = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString())
        res.body() shouldBe "secure double proxy!"
    }

    @Test
    fun `should work when connecting directly to proxy https`() {
        wm.stubFor(WireMock.get("/").willReturn(WireMock.ok("secure single proxy!")))
        client = HttpClient.newBuilder()
            .sslContext(insecureSsl())
            .connectTimeout(Duration.ofMillis(50))
            .proxy(
                ProxySelector.of(
                    InetSocketAddress.createUnresolved(
                        proxy.listenAddress.hostString,
                        proxy.listenAddress.port
                    )
                )
            )
            .build()
        val req = HttpRequest.newBuilder(URI("$baseHttps/")).GET().build()
        val res = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString())
        res.statusCode() shouldBe 407
//        res.body() shouldBe "secure single proxy!"
    }

    @Test
    fun `should work when connecting directly to proxy http`() {
        wm.stubFor(WireMock.get("/").willReturn(WireMock.ok("nonsecure single proxy!")))
        client = HttpClient.newBuilder()
            .sslContext(insecureSsl())
            .connectTimeout(Duration.ofMillis(50))
            .proxy(
                ProxySelector.of(
                    InetSocketAddress.createUnresolved(
                        proxy.listenAddress.hostString,
                        proxy.listenAddress.port
                    )
                )
            )
            .build()
        val req = HttpRequest.newBuilder(URI("$baseHttp/")).GET().build()
        val res = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString())
        res.statusCode() shouldBe 407
    }

}
