package org.fline.proxy.utils

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpObject
import io.netty.handler.codec.http.HttpRequest
import org.littleshoot.proxy.HttpFilters
import org.littleshoot.proxy.HttpFiltersAdapter
import org.littleshoot.proxy.HttpFiltersSource
import org.littleshoot.proxy.HttpProxyServer
import org.littleshoot.proxy.ProxyAuthenticator
import org.littleshoot.proxy.impl.DefaultHttpProxyServer

class TestProxyServer(
    port: Int,
    val username: String? = null,
    val password: String? = null
) {
    private lateinit var proxy: HttpProxyServer

    private val proxyServerBootstrap = DefaultHttpProxyServer.bootstrap()
        .withPort(port)
        .let {
            if (username != null && password != null) {
                it.withProxyAuthenticator(object : ProxyAuthenticator {
                    override fun authenticate(reqUsername: String, reqPassword: String) =
                        reqUsername == username && reqPassword == password

                    override fun getRealm() = null
                })
            } else it
        }.withFiltersSource(object : HttpFiltersSource {
            override fun filterRequest(
                originalRequest: HttpRequest,
                ctx: ChannelHandlerContext
            ): HttpFilters {
                return object : HttpFiltersAdapter(originalRequest) {
                    override fun clientToProxyRequest(httpObject: HttpObject?) = null

                    override fun serverToProxyResponse(httpObject: HttpObject?) =
                        super.serverToProxyResponse(httpObject)

                }
            }

            override fun getMaximumRequestBufferSizeInBytes() = 0
            override fun getMaximumResponseBufferSizeInBytes() = 0
        })

    fun start() {
        stop()
        proxy = proxyServerBootstrap.start()
    }

    fun port() = proxy.listenAddress.port

    fun stop() {
        if (this::proxy.isInitialized) {
            proxy.stop()
        }
    }

}

