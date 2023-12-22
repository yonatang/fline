package org.fline.proxy

import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import io.kotest.matchers.shouldBe
import org.awaitility.Awaitility
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ProxySelector
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.security.cert.X509Certificate
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Timeout(3, unit = TimeUnit.SECONDS)
class DirectHandlerTest {
    companion object {
        init {
            System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true")
        }
    }

    @RegisterExtension
    val wm = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort().dynamicHttpsPort())
        .build()

    lateinit var runner: CoroutineProxyRunner
    lateinit var client: HttpClient
    lateinit var baseHttp: String
    lateinit var baseHttps: String

    lateinit var metrics: Metrics

    @BeforeEach
    fun init() {
        metrics = Metrics()
        runner = CoroutineProxyRunner(DirectProxySelector(), metrics)
        runner.run(0)
        Awaitility.await().until { runner.port > 0 }
        client = HttpClient.newBuilder()
            .sslContext(insecureSsl())
            .connectTimeout(Duration.ofMillis(50))
            .proxy(ProxySelector.of(InetSocketAddress.createUnresolved("localhost", runner.port)))
            .build()
        baseHttp = "http://localhost:${wm.port}"
        baseHttps = "https://localhost:${wm.httpsPort}"
    }

    @AfterEach
    fun afterAll() {
        runner.stop()
    }

    @Test
    fun `should get single http get request`() {
        wm.stubFor(get("/").willReturn(ok("foobar!")))
        val req = HttpRequest.newBuilder(URI("$baseHttp/")).GET().build()
        val res = client.send(req, BodyHandlers.ofString())
        res.body() shouldBe "foobar!"
    }

    @Test
    fun `should get multiple http get requests`() {
        wm.stubFor(get("/").willReturn(ok("foobar!")))

        val getReq = HttpRequest.newBuilder(URI("$baseHttp/")).GET().build()

        val res1 = client.send(getReq, BodyHandlers.ofString())
        val res2 = client.send(getReq, BodyHandlers.ofString())
        res1.body() shouldBe "foobar!"
        res2.body() shouldBe "foobar!"

        metrics.totalConnections shouldBe 1
    }

    @Test
    fun `should handle POST request`() {
        wm.stubFor(post("/").willReturn(ok("foobar!")))
        val postReq = HttpRequest.newBuilder(URI("$baseHttp/"))
            .POST(HttpRequest.BodyPublishers.ofString("foo and bar are?")).build()

        val res = client.send(postReq, BodyHandlers.ofString())

        res.body() shouldBe "foobar!"
        wm.verify(1, postRequestedFor(urlPathEqualTo("/")).withRequestBody(equalTo("foo and bar are?")))
    }

    @Test
    fun `should handle multiple POST requests`() {
        wm.stubFor(post("/foobar").willReturn(ok("foobar!")))
        wm.stubFor(post("/boofar").willReturn(ok("boofar!")))
        val postReq1 = HttpRequest.newBuilder(URI("$baseHttp/foobar"))
            .POST(HttpRequest.BodyPublishers.ofString("foo and bar are?")).build()
        val postReq2 = HttpRequest.newBuilder(URI("$baseHttp/boofar"))
            .POST(HttpRequest.BodyPublishers.ofString("boo and far are?")).build()

        val res1 = client.send(postReq1, BodyHandlers.ofString())
        val res2 = client.send(postReq2, BodyHandlers.ofString())

        res1.body() shouldBe "foobar!"
        res2.body() shouldBe "boofar!"
        wm.verify(1, postRequestedFor(urlPathEqualTo("/foobar")).withRequestBody(equalTo("foo and bar are?")))
        wm.verify(1, postRequestedFor(urlPathEqualTo("/boofar")).withRequestBody(equalTo("boo and far are?")))

        metrics.totalConnections shouldBe 1
    }

    @Test
    @Disabled
    fun `should handle multiple headers`() {
        TODO()
    }

    @Test
    fun `should get single https get request`() {
        wm.stubFor(get("/").willReturn(ok("secure foobar!")))
        val req = HttpRequest.newBuilder(URI("$baseHttps/")).GET().build()
        val res = client.send(req, BodyHandlers.ofString())
        res.body() shouldBe "secure foobar!"
    }

    @Test
    fun `should get multiple https get requests`() {
        wm.stubFor(get("/").willReturn(ok("secure foobar!")))
        val req = HttpRequest.newBuilder(URI("$baseHttps/")).GET().build()
        val res1 = client.send(req, BodyHandlers.ofString())
        val res2 = client.send(req, BodyHandlers.ofString())
        res1.body() shouldBe "secure foobar!"
        res2.body() shouldBe "secure foobar!"
    }

    @Test
    fun `should handle delay`() {
        wm.stubFor(get("/").willReturn(ok("message with delay").withChunkedDribbleDelay(3, 40)))
        val req = HttpRequest.newBuilder(URI("$baseHttps/")).GET().build()
        val res = client.send(req, BodyHandlers.ofString())
        res.body() shouldBe "message with delay"
    }

    @Test
    fun `should handle delay2`() {
        wm.stubFor(get("/").willReturn(ok("message with delay").withChunkedDribbleDelay(3, 40)))
        val req = HttpRequest.newBuilder(URI("$baseHttp/")).GET().build()
        val res = client.send(req, BodyHandlers.ofString())
        res.body() shouldBe "message with delay"
    }

    @Test
    fun `should handle fault connection reset`() {
        wm.stubFor(get("/fault").willReturn(ok("this will close").withFault(Fault.CONNECTION_RESET_BY_PEER)))
        wm.stubFor(get("/ok").willReturn(ok("this will work")))

        val req1 = HttpRequest.newBuilder(URI("$baseHttp/fault")).GET().build()
        val req2 = HttpRequest.newBuilder(URI("$baseHttp/ok")).GET().build()
        assertThrows<IOException>("Connection reset") { client.send(req1, BodyHandlers.ofString()) }
        val res2 = client.send(req2, BodyHandlers.ofString())
        res2.body() shouldBe "this will work"
    }

    @Test
    @Disabled
    fun `should fail if proxy is shut down for http`() {
        TODO()
    }

    @Test
    @Disabled
    fun `should handle multiple concurrent requests`() {
        TODO()
    }

    @Test
    @Disabled
    fun `should fail if proxy is shut down for https`() {
        TODO()
    }

    @Test
    fun `should work when hitting directly the https endpoint`() {

        wm.stubFor(get("/").willReturn(ok("secure foobar!")))
        client = HttpClient.newBuilder()
            .sslContext(insecureSsl())
            .build()
        val res = client.send(
            HttpRequest
                .newBuilder(URI("$baseHttps/"))
                .GET().build(), BodyHandlers.ofString()
        )
        res.body() shouldBe "secure foobar!"
    }
}

fun insecureSsl(): SSLContext {
    val noopTrustManager = arrayOf<TrustManager>(
        object : X509TrustManager {
            override fun checkClientTrusted(xcs: Array<X509Certificate>, string: String) {}
            override fun checkServerTrusted(xcs: Array<X509Certificate>, string: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate>? {
                return null
            }
        }
    )
    val sc = SSLContext.getInstance("ssl")
    sc.init(null, noopTrustManager, null)
    return sc
}
