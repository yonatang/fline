package org.fline.proxy

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private const val CRLF = "\r\n"
private val log = KotlinLogging.logger { }
class DirectHandler(
    private val channels: ClientRemoteChannels,
    private val firstLine: String,
) : Handler {

    private val log = KotlinLogging.logger { }

    override suspend fun handle(): Unit = coroutineScope {
        try {
            log.debug { "Getting ${firstLine.trim()} directly" }
            val (toClient, fromClient, toRemote, fromRemote) = channels
            val isConnect = firstLine.startsWith("CONNECT")

            if (!isConnect) {
                println("Not connect")
                launch(Dispatchers.IO) {
                    val firstLineRewrite: (String) -> String = {
                        val (method, target, version) = it.split(' ')
                        val newTarget = '/' + target.substringAfter("//").substringAfter('/')
                        "$method $newTarget $version"
                    }
                    handleClientRequest(channels, firstLine, null, firstLineRewrite)
                }
                val fromClientJob = launch(Dispatchers.IO) {
                    log.debug { "Waiting for remote to response" }
                    val written = fromRemote.copyAndClose(toClient)
                    log.trace { "Wrote $written bytes" }
                }
                fromClientJob.join()
//                log.debug { "Waiting for remote to response" }
//                val written = fromRemote.copyAndClose(toClient)
//                log.trace { "Wrote $written bytes" }
            } else {
                var line: String?
                // Skip the rest of the headers and just connect the sockets
                do {
                    line = fromClient.readUTF8Line()
                } while (!line.isNullOrEmpty())
//            } while (line != null && line != CRLF)
                toClient.writelnStringUtf8("HTTP/1.1 200 Connection established")
                toClient.writelnStringUtf8("")
                connect(channels)
            }
        } catch (ex:Exception){
            log.warn(ex) { "error in handling" }
        }
    }
//
//    private suspend fun handleClientReadOld(fromClient: ByteReadChannel, toRemote: ByteWriteChannel) {
//        var firstLine: String = firstLine
//        do {
//            log.debug { "Req: $firstLine" }
//            val (method, target, version) = firstLine.split(' ')
//            val newTarget = '/' + target.substringAfter("//").substringAfter('/')
//            toRemote.writelnStringUtf8("$method $newTarget $version")
//            var line: String?
//            var contentLength = 0L
//            do {
//                line = fromClient.readUTF8Line()
//                log.debug { "Header: $line" }
//                if (line?.uppercase()?.startsWith("CONTENT-LENGTH: ") == true) {
//                    contentLength = line.substring(16).trim().toLong()
//                }
//                toRemote.writelnStringUtf8(line)
//            } while (!line.isNullOrEmpty())
//            fromClient.copyTo(toRemote, contentLength)
//            fromClient.readUTF8Line()?.also {
//                firstLine = it
//            } ?: break
//            //TODO test POST requests
//        } while (true)
//        toRemote.close()
//    }
}

class ProxyHandler(
    private val channels: ClientRemoteChannels,
    private val firstLine: String,
    private val authHeader: String
) : Handler {

    private val log = KotlinLogging.logger { }
    override suspend fun handle(): Unit = coroutineScope {
        try {
            log.debug { "Getting $firstLine though proxy" }
            val isConnect = firstLine.startsWith("CONNECT")

            val (toClient, fromClient, toRemote, fromRemote) = channels
            var line: String?
            if (!isConnect) {
                launch { handleClientRequest(channels, firstLine, authHeader, { it }) }
                val written = fromRemote.copyAndClose(toClient)
                log.trace { "Wrote $written bytes" }
            } else {
                toRemote.writelnStringUtf8(firstLine)
                toRemote.writelnStringUtf8(authHeader)
                do {
                    line = fromClient.readUTF8Line()
                    log.debug { "Header: $line" }
                    toRemote.writelnStringUtf8(line)
                } while (!line.isNullOrEmpty())
                connect(channels)
            }
        } catch (ex:Exception){
            log.warn(ex) { "Error handling" }
        }

        // TODO inject auth header for each request when persisted request, not applicable when CONNECT
        // Need to read Content-Length
    }

//    private suspend fun handleClientRequest(channels: ClientRemoteChannels, firstLine: String,authHeader: String) {
//        var firstLine: String = firstLine
//        val (_, fromClient, toRemote, _) = channels
//
//        do {
//            log.debug { "Req: $firstLine" }
//            toRemote.writelnStringUtf8(firstLine)
//            toRemote.writelnStringUtf8(authHeader)
//            var line: String?
//            var contentLength = 0L
//            do {
//                line = fromClient.readUTF8Line()
//                log.debug { "Header: $line" }
//                if (line?.uppercase()?.startsWith("CONTENT-LENGTH: ") == true) {
//                    contentLength = line.substring(16).trim().toLong()
//                }
//                toRemote.writelnStringUtf8(line)
//            } while (!line.isNullOrEmpty())
//            fromClient.copyTo(toRemote, contentLength)
//            fromClient.readUTF8Line()?.also {
//                firstLine = it
//            } ?: break
//            //TODO test POST requests
//        } while (true)
//        toRemote.close()
//    }
}

private suspend fun handleClientRequest(
    channels: ClientRemoteChannels,
    firstLine: String,
    authHeader: String?,
    firstLineRewrite: (String) -> String
) {
    var firstLine: String = firstLine
    val (_, fromClient, toRemote, _) = channels

    try {
        do {
            log.debug { "Req: $firstLine" }
//        println("Req: $firstLine")
            toRemote.writelnStringUtf8(firstLineRewrite(firstLine))
            toRemote.writelnStringUtf8(authHeader)
            var line: String?
            var contentLength = 0L
            do {
                line = fromClient.readUTF8Line()
                log.debug { "Header: $line" }
                if (line?.uppercase()?.startsWith("CONTENT-LENGTH: ") == true) {
                    contentLength = line.substring(16).trim().toLong()
                }
                if (line?.isEmpty()==true) {
//                    toRemote.writelnStringUtf8("Via: 1.1 LM-SJC-11025615")
                    log.debug { "Recieved empty line, sending and waiting for response" }
                }
                toRemote.writelnStringUtf8(line)
            } while (!line.isNullOrEmpty())
            log.debug { "Headers were all sent" }
            val totalBytes = fromClient.copyTo(toRemote, contentLength)
            toRemote.flush()
            log.debug { "Submitted payload of $totalBytes" }

            log.debug { "Reading from client another request" }
            fromClient.readUTF8Line()?.also {
                log.debug { "Got another request from client $it" }
                firstLine = it
            } ?: break
            log.debug { "Done waiting" }
            //TODO test POST requests
        } while (true)
    } catch (ex:Exception) {
        log.warn(ex) { "Error handling request" }
    } finally {
        log.debug { "Remote closed" }
        toRemote.close()
    }
}

private suspend fun connect(channels: ClientRemoteChannels) = coroutineScope {
    with(channels) {
        launch(Dispatchers.IO) {
            try {
                fromClient.copyAndClose(toRemote)
            } catch (ex:Exception){
                log.warn(ex) { "Error communicating from client to remote" }
            }
        }
        try {
            fromRemote.copyAndClose(toClient)
        } catch (ex:Exception){
            log.warn(ex) { "Error communicating from remote to client" }
        }
    }
}

private suspend fun ByteWriteChannel.writelnStringUtf8(line: String?) {
    if (line == null) {
        return
    }
    if (line.isNotEmpty()) {
        writeStringUtf8(line)
    }
    writeStringUtf8(CRLF)
    flush()
}

interface Handler {
    suspend fun handle()
}


data class ClientRemoteChannels(
    val toClient: ByteWriteChannel,
    val fromClient: ByteReadChannel,
    val toRemote: ByteWriteChannel,
    val fromRemote: ByteReadChannel,
)
