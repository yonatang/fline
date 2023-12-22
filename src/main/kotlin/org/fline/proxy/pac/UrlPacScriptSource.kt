//package org.fline.fline.proxy.pac
//
//import mu.KotlinLogging
//import java.io.*
//import java.net.MalformedURLException
//import java.net.URISyntaxException
//import java.net.URL
//import java.util.*
//
//
//class UrlPacScriptSource
///*************************************************************************
// * Constructor
// * @param url the URL to download the script from.
// */(  // Reset it again with next download we should get a new expire info
//    private val scriptUrl: String
//) : PacScriptSource {
//
//    private val log = KotlinLogging.logger { }
//
//    /*************************************************************************
//     * getScriptContent
//     * @see com.btr.proxy.selector.pac.PacScriptSource.getScriptContent
//     */
////    @get:Throws(IOException::class)
////    @get:Synchronized
//    override var scriptContent: String
//        get() {
//            if (field == null ||
//                (expireAtMillis > 0
//                        && expireAtMillis < System.currentTimeMillis())
//            ) {
//                try {
//                    // Reset it again with next download we should get a new expire info
//                    expireAtMillis = 0
//                    field = if (scriptUrl.startsWith("file:/") || scriptUrl.indexOf(":/") == -1) {
//                        readPacFileContent(scriptUrl)
//                    } else {
//                        downloadPacContent(scriptUrl)
//                    }
//                } catch (e: IOException) {
//                    log.error(e) { "Loading script failed from: $scriptUrl with error $e" }
//                    field = ""
//                    throw e
//                }
//            }
//            return field
//        }
//        private set
//    private var expireAtMillis: Long = 0
//
//    /*************************************************************************
//     * Reads a PAC script from a local file.
//     * @param scriptUrl
//     * @return the content of the script file.
//     * @throws IOException
//     * @throws URISyntaxException
//     */
//    @Throws(IOException::class)
//    private fun readPacFileContent(scriptUrl: String): String {
//        return try {
//            var file: File? = null
//            if (scriptUrl.indexOf(":/") == -1) {
//                file = File(scriptUrl)
//            } else {
//                file = File(URL(scriptUrl).toURI())
//            }
//            val r = BufferedReader(FileReader(file))
//            val result = StringBuilder()
//            try {
//                var line: String?
//                while (r.readLine().also { line = it } != null) {
//                    result.append(line).append("\n")
//                }
//            } finally {
//                r.close()
//            }
//            result.toString()
//        } catch (e: Exception) {
//            println(System.getProperty("user.dir"))
//            log.error(e) { "File reading error." }
//            throw IOException(e.message)
//        }
//    }
//
//    /*************************************************************************
//     * Downloads the script from a webserver.
//     * @param url the URL to the script file.
//     * @return the script content.
//     * @throws IOException on read error.
//     */
//    @Throws(IOException::class)
//    private fun downloadPacContent(url: String?): String {
//        if (url == null) {
//            throw IOException("Invalid PAC script URL: null")
//        }
//        setPacProxySelectorEnabled(false)
//        var con: HttpURLConnection? = null
//        return try {
//            con = setupHTTPConnection(url)
//            if (con.getResponseCode() !== 200) {
//                throw IOException(("Server returned: " + con.getResponseCode()) + " " + con.getResponseMessage())
//            }
//            // Read expire date.
//            expireAtMillis = con.getExpiration()
//            val r = getReader(con)
//            val result = readAllContent(r)
//            r.close()
//            result
//        } finally {
//            setPacProxySelectorEnabled(true)
//            if (con != null) {
//                con.disconnect()
//            }
//        }
//    }
//
//    /*************************************************************************
//     * Enables/disables the PAC proxy selector while we download to prevent recursion.
//     * See issue: 26 in the change tracker.
//     */
//    private fun setPacProxySelectorEnabled(enable: Boolean) {
//        PacProxySelector.setEnabled(enable)
//    }
//
//    /*************************************************************************
//     * Reads the whole content available into a String.
//     * @param r to read from.
//     * @return the complete PAC file content.
//     * @throws IOException
//     */
//    @Throws(IOException::class)
//    private fun readAllContent(r: BufferedReader): String {
//        val result = StringBuilder()
//        var line: String?
//        while (r.readLine().also { line = it } != null) {
//            result.append(line).append("\n")
//        }
//        return result.toString()
//    }
//
//    /*************************************************************************
//     * Build a BufferedReader around the open HTTP connection.
//     * @param con to read from
//     * @return the BufferedReader.
//     * @throws UnsupportedEncodingException
//     * @throws IOException
//     */
//    @Throws(UnsupportedEncodingException::class, IOException::class)
//    private fun getReader(con: HttpURLConnection?): BufferedReader {
//        val charsetName = parseCharsetFromHeader(con.getContentType())
//        return BufferedReader(InputStreamReader(con.getInputStream(), charsetName))
//    }
//
//    /*************************************************************************
//     * Configure the connection to download from.
//     * @param url to get the pac file content from
//     * @return a HTTPUrlConnecion to this url.
//     * @throws IOException
//     * @throws MalformedURLException
//     */
//    @Throws(IOException::class, MalformedURLException::class)
//    private fun setupHTTPConnection(url: String): HttpURLConnection {
//        val con: HttpURLConnection = URL(url).openConnection(Proxy.NO_PROXY) as HttpURLConnection
//        con.setConnectTimeout(getTimeOut(OVERRIDE_CONNECT_TIMEOUT, DEFAULT_CONNECT_TIMEOUT))
//        con.setReadTimeout(getTimeOut(OVERRIDE_READ_TIMEOUT, DEFAULT_READ_TIMEOUT))
//        con.setInstanceFollowRedirects(true)
//        con.setRequestProperty("accept", "application/x-ns-proxy-autoconfig, */*;q=0.8")
//        return con
//    }
//
//    /*************************************************************************
//     * Gets the timeout value from a property or uses the given default value if
//     * the property cannot be parsed.
//     * @param overrideProperty the property to define the timeout value in milliseconds
//     * @param defaultValue the default timeout value in milliseconds.
//     * @return the value to use.
//     */
//    protected fun getTimeOut(overrideProperty: String?, defaultValue: Int): Int {
//        var timeout = defaultValue
//        val prop = System.getProperty(overrideProperty)
//        if (prop != null && prop.trim { it <= ' ' }.length > 0) {
//            try {
//                timeout = prop.trim { it <= ' ' }.toInt()
//            } catch (e: NumberFormatException) {
//                Logger.log(
//                    javaClass,
//                    LogLevel.DEBUG,
//                    "Invalid override property : {0}={1}",
//                    overrideProperty,
//                    prop
//                )
//                // In this case use the default value.
//            }
//        }
//        return timeout
//    }
//
//    /*************************************************************************
//     * Response Content-Type could be something like this:
//     * application/x-ns-proxy-autoconfig; charset=UTF-8
//     * @param contentType header field.
//     * @return the extracted charset if set else a default charset.
//     */
//    fun parseCharsetFromHeader(contentType: String?): String {
//        var result = "ISO-8859-1"
//        if (contentType != null) {
//            val paramList = contentType.split(";".toRegex()).dropLastWhile { it.isEmpty() }
//                .toTypedArray()
//            for (param in paramList) {
//                if (param.lowercase(Locale.getDefault()).trim { it <= ' ' }
//                        .startsWith("charset") && param.indexOf("=") != -1) {
//                    result = param.substring(param.indexOf("=") + 1).trim { it <= ' ' }
//                }
//            }
//        }
//        return result
//    }
//
//    /***************************************************************************
//     * @see java.lang.Object.toString
//     */
//    override fun toString(): String {
//        return scriptUrl
//    }
//
//    /*************************************************************************
//     * isScriptValid
//     * @see com.btr.proxy.selector.pac.PacScriptSource.isScriptValid
//     */
//    override val isScriptValid: Boolean
//        get() = try {
//            val script = scriptContent
//            if (script == null || script.trim { it <= ' ' }.length == 0) {
//                Logger.log(javaClass, LogLevel.DEBUG, "PAC script is empty. Skipping script!")
//                return false
//            }
//            if (script.indexOf("FindProxyForURL") == -1) {
//                Logger.log(
//                    javaClass,
//                    LogLevel.DEBUG,
//                    "PAC script entry point FindProxyForURL not found. Skipping script!"
//                )
//                return false
//            }
//            true
//        } catch (e: IOException) {
//            Logger.log(javaClass, LogLevel.DEBUG, "File reading error: {0}", e)
//            false
//        }
//
//    companion object {
//        private const val DEFAULT_CONNECT_TIMEOUT = 15 * 1000 // seconds
//        private const val DEFAULT_READ_TIMEOUT = 20 * 1000 // seconds
//        const val OVERRIDE_CONNECT_TIMEOUT = "com.btr.proxy.url.connectTimeout"
//        const val OVERRIDE_READ_TIMEOUT = "com.btr.proxy.url.readTimeout"
//    }
//}
