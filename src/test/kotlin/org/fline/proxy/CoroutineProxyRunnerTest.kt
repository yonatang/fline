package org.fline.proxy

import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import io.mockk.mockk
import org.apache.hc.client5.http.auth.AuthScope
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.HttpHost
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.fline.proxy.utils.TestProxyServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import java.net.InetSocketAddress
import java.net.URI
import javax.annotation.PostConstruct

//@SpringBootTest(
//    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
//    classes = [
//        WebMvcAutoConfiguration::class,
//        ServletWebServerFactoryAutoConfiguration::class,
//        TinyServer::class]
//)
class CoroutineProxyRunnerTest {

    lateinit var proxyRunner: CoroutineProxyRunner
    lateinit var proxyServer: TestProxyServer
    lateinit var metrics: Metrics
    lateinit var client: CloseableHttpClient

    var port: Int = 0
    lateinit var server: HttpServer

    @BeforeEach
    fun init() {
        server = HttpServer.create(InetSocketAddress(0), 0)
        server.createContext("/hello") { exchange ->
            exchange.responseHeaders.add("Content-Type", "text/plain")
            exchange.sendResponseHeaders(200, 0)
            exchange.responseBody.use { os ->
                os.write("Hello World!".toByteArray())
            }
        }
        server.start()
        port = server.address.port

        metrics = mockk()
        proxyServer = TestProxyServer(0, "foo", "bar")
        proxyServer.start()
        proxyRunner = CoroutineProxyRunner(TestSelector(), metrics)
        val proxyHost = HttpHost("localhost", proxyServer.port())
        val creds = BasicCredentialsProvider()
        creds.setCredentials(AuthScope(proxyHost), UsernamePasswordCredentials("foo", "bar".toCharArray()))
        client = HttpClients.custom()
//            .setProxy(proxyHost)
            .setDefaultCredentialsProvider(creds)
            .build()


    }

    @Test
    fun t() {
        println(proxyServer.port())


        val get = HttpGet("http://localhost:${port}/hello")
        client.execute(get) { response ->
            println(response.code)
            println(response.entity.content.reader().readText())
        }
    }

    @AfterEach
    fun shutdown() {
        proxyServer.stop()
        client.close()
        server.stop(0)
    }

    inner class TestSelector : PPProxySelector {
        override fun select(uri: URI): List<PPProxy> {
            return listOf(PPProxy(PPType.HTTP, "localhost", proxyServer.port()))
        }

    }


}

@RestController
@ResponseBody
class TinyServer {

    @PostConstruct
    fun init() {
        println("init!!!")
    }

    @GetMapping("/hello")
    @ResponseBody
    fun hello(): String {
        println("Hello!!")
        return "Hello!"
    }
}
