package org.fline.proxy.sockets

import io.kotest.mpp.start
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel

class NioSocketsTest {

    fun echoServer9991() {
        Thread {
            val serverSocket = ServerSocketChannel.open()
            serverSocket.socket().bind(InetSocketAddress(9991))
            val channel = serverSocket.accept()
            val buffer = ByteBuffer.allocateDirect(1024)
//        val buffer = ByteBuffer.allocate(1024)
            while (channel.read(buffer) != -1) {
                buffer.flip()
                while (buffer.hasRemaining()) {
                    channel.write(buffer)
                }
                buffer.clear()
            }
            channel.close()
        } .start()
    }

    fun proxyServer9992() {
        Thread {
            val serverSocket = ServerSocketChannel.open()
            serverSocket.socket().bind(InetSocketAddress(9992))
            val server = serverSocket.accept()
            val client = SocketChannel.open()
            client.connect(InetSocketAddress("localhost", 9991))

            val t = Thread {
                val readBuffer = ByteBuffer.allocateDirect(1024)
                while (server.read(readBuffer) != -1) {
                    readBuffer.flip()
                    while (readBuffer.hasRemaining()) {
                        client.write(readBuffer)
                    }
                    readBuffer.clear()
                }
            }.apply { start() }

            val writeBuffer = ByteBuffer.allocateDirect(1024)
            while (client.read(writeBuffer) != -1) {
                writeBuffer.flip()
                while (writeBuffer.hasRemaining()) {
                    server.write(writeBuffer)
                }
                writeBuffer.clear()
            }
            t.join()
            server.close()
            client.close()
        } .start()
    }

    fun traffic(){
        val client = SocketChannel.open()
        client.connect(InetSocketAddress("localhost", 9992))
        val line = "${"foo;".repeat(200)}\n".toByteArray()

        val t=Thread {
            val rbb=ByteBuffer.allocateDirect(4096)
            var count=0
            while(client.read(rbb)>-1){
                rbb.flip()
                val arr=rbb.asReadOnlyBuffer().array()
                count+=arr.size
                println(arr.size)
                rbb.clear()
            }
            println("Done reading!")
        }.apply { start() }
        val bb=ByteBuffer.allocateDirect(4096)
        repeat(1024 * 1024) {
            bb.put(line)
            bb.flip()
            client.write(bb)
            bb.clear()
        }
        t.join()


    }

    @Test
    fun run(){
        echoServer9991()
        proxyServer9992()
        traffic()
    }
}
