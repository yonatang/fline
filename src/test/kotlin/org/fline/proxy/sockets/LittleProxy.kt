package org.fline.proxy.sockets

import io.ktor.utils.io.jvm.nio.*
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpObject
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import org.fline.proxy.ProxyHandlerTest
import org.junit.jupiter.api.Test
import org.littleshoot.proxy.HttpFilters
import org.littleshoot.proxy.HttpFiltersAdapter
import org.littleshoot.proxy.HttpFiltersSource
import org.littleshoot.proxy.ProxyAuthenticator
import org.littleshoot.proxy.impl.DefaultHttpProxyServer
import java.nio.channels.SocketChannel

class LittleProxy {
    @Test
    fun runIt(){
        println("pid: ${ProcessHandle.current().pid()}")
        val proxy = DefaultHttpProxyServer.bootstrap().withPort(8889)
            .withProxyAuthenticator(object : ProxyAuthenticator {

                override fun authenticate(userName: String, password: String): Boolean {
//                    return true;
                    println("Authenticating $userName $password")
                    return userName == "foo" && password == "bar"
                }

                override fun getRealm() = null

            })
            .withFiltersSource(object : HttpFiltersSource {
                override fun filterRequest(
                    originalRequest: HttpRequest,
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
        println(proxy.listenAddress)
//        SocketChannel.open().copyTo()
        Thread.sleep(8000000)
    }
}
