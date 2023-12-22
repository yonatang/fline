package org.fline.proxy.sockets

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.*
//import org.apache.commons.codec.digest.MurmurHash3
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
//import java.util.zip.CRC32

class KtorSocketsTest {


    suspend fun erectAServer(
        port: Int,
        action: suspend (rc: ByteReadChannel, wc: ByteWriteChannel) -> Unit
    ): Thread {
        return Thread {
            runBlocking {
                ActorSelectorManager(Dispatchers.IO).use { manager ->
                    aSocket(manager).tcp().bind(InetSocketAddress("127.0.0.1", port))
                        .use { server ->
                            server.accept().use { serverSocket ->
                                val readChannel = serverSocket.openReadChannel()
                                val writeChannel = serverSocket.openWriteChannel(autoFlush = false)
                                action(readChannel, writeChannel)
                            }
                        }
                }
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    suspend fun copy(r: ByteReadChannel, w: ByteWriteChannel) {
        r.copyTo(w).also { println("Echoed $it bytes") }
    }

    suspend fun proxy(r: ByteReadChannel, w: ByteWriteChannel, port: Int) = coroutineScope {
        ActorSelectorManager(Dispatchers.IO).use { manager ->
            aSocket(manager).tcp().connect("localhost", port).use { clientSocket ->
                val cr = clientSocket.openReadChannel()
                val cw = clientSocket.openWriteChannel(autoFlush = false)
                launch { r.copyAndClose(cw) }
                cr.copyAndClose(w)
            }
        }
    }

    suspend fun ktorChannels() = coroutineScope {
        erectAServer(9990) { r, w -> copy(r, w) }
        erectAServer(9991) { r, w -> proxy(r, w, 9990) }


    }
    suspend fun traffic()= coroutineScope{
        val line = "${"foo;".repeat(200)}\n"
        ActorSelectorManager(Dispatchers.IO).use { manager ->
            aSocket(manager).tcp().connect("localhost", 9991).use { socket ->
                val wc = socket.openWriteChannel(false)
                val rc = socket.openReadChannel()
                launch {
                    val counter = AtomicInteger()
                    rc.toInputStream().bufferedReader().lines()
                        .forEach {
                            if (counter.incrementAndGet() % 10000 == 0) {
                                println(it)
                                println("Inc")
                            }
                        }
                }
                repeat(1024 * 1024) { wc.writeStringUtf8(line); wc.flush() }
            }
        }
    }

    @Test
    fun ktorRun() = runBlocking {
        println("Pid: ${ProcessHandle.current().pid()}")
        println("Starting the servers")
        ktorChannels()
//        kotlinx.coroutines.time.delay(Duration.ofSeconds(20))
        println("Starting the traffic")
        traffic()
        println("Done!")
    }


}
