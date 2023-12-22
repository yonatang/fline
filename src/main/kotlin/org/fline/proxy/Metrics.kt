package org.fline.proxy

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@Service
class Metrics {

    private val log = KotlinLogging.logger {}
    private val totalConnectionsCounter = AtomicLong()

    private val activeConnectionsCounter = AtomicInteger()

    val totalConnections : Long
        get() = totalConnectionsCounter.get()
    val activeConnections : Int
        get() = activeConnectionsCounter.get()

    fun newConnection(){
        log.debug { "Total connections: $totalConnectionsCounter, active connections: $activeConnectionsCounter" }
        activeConnectionsCounter.incrementAndGet()
        totalConnectionsCounter.incrementAndGet()
    }

    fun closeConnection(){
        activeConnectionsCounter.decrementAndGet()
    }
}
