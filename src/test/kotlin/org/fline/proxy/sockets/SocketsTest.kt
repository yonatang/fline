package org.fline.proxy.sockets

import io.ktor.utils.io.*
import org.apache.commons.io.IOUtils
import org.junit.jupiter.api.Test
import java.net.ServerSocket
import java.net.Socket
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

class SocketsTest {

    fun echoServerPort9991() {
        Thread {
            val server = ServerSocket(9991)
            server.accept().use { socket ->
                val rs = socket.getInputStream()
                val ws = socket.getOutputStream()
                IOUtils.copy(rs, ws).also { println("Echoed $it bytes") }
            }
        }.apply { start() }
    }

    fun proxyServerPort9992() {
        Thread {
            val server = ServerSocket(9992)
            server.accept().use { serverSocket ->
                val serverIs = serverSocket.getInputStream()
                val serverOs = serverSocket.getOutputStream()
                Socket("localhost", 9991).use { clientSocket ->
                    val clientIs = clientSocket.getInputStream()
                    val clientOs = clientSocket.getOutputStream()
                    val t = Thread {
                        println("Copy from server IS to client OS")
                        IOUtils.copy(serverIs, clientOs)
                            .also { println("SIS->COS moved $it bytes") }
                        println("Done copy! SIS->COS")
                    }.apply { start() }
                    println("Copy from client IS to server OS")
                    IOUtils.copy(clientIs, serverOs).also { println("CIS-SOS moved $it bytes") }
                    println("Done copy CIS->SOS")
                    t.join()
                    clientIs.close()
                    clientOs.close()
                }
                serverIs.close()
                serverOs.close()
            }
        }.start()
    }

    fun traffic(toPort: Int) {
        val line = "${"foo;".repeat(200)}\n"
        Socket("localhost", toPort).use {
            val input = it.getInputStream()
            val t = Thread {
                var counter = 0
                input.bufferedReader().lines().forEach {
                    if (counter++ % 100000 == 0) {
                        println("Inc")
                    }
                }
                input.close()
            }.apply {
                start()
            }
            it.getOutputStream()
                .bufferedWriter().use { writer ->
                    repeat(10 * 1024 * 1024) {
                        writer.write(line)
//                    writer.newLine()
                        writer.flush()
                    }
                }
            t.join()
        }
    }

    @Test
    fun runTest() {
        println("Pid: ${ProcessHandle.current().pid()}")
        println("Starting the servers")
        echoServerPort9991()
        proxyServerPort9992()
        Thread.sleep(20_000)
        println("Starting the traffic")
        traffic(9992)
        println("Done!")
    }
}
