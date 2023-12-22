package org.fline.proxy.pac

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.IOException
import java.net.*
import java.util.*


/***************************************************************************
 * Implementation of PAC JavaScript functions.
 *
 * @author Bernd Rosstauscher (proxyvole@rosstauscher.de) Copyright 2009
 */
class PacScriptMethods
/*************************************************************************
 * Constructor
 */
    : ScriptMethods {
    private var currentTime: Calendar? = null

    private val log = KotlinLogging.logger { }
    /*************************************************************************
     * isPlainHostName
     * @see com.btr.proxy.selector.pac.ScriptMethods.isPlainHostName
     */
    override fun isPlainHostName(host: String): Boolean {
        return host.indexOf(".") < 0
    }

    /*************************************************************************
     * Tests if an URL is in a given domain.
     *
     * @param host
     * is the host name from the URL.
     * @param domain
     * is the domain name to test the host name against.
     * @return true if the domain of host name matches.
     */
    override fun dnsDomainIs(host: String, domain: String): Boolean {
        return host.endsWith(domain)
    }

    /*************************************************************************
     * Is true if the host name matches exactly the specified host name, or if
     * there is no domain name part in the host name, but the unqualified host
     * name matches.
     *
     * @param host
     * the host name from the URL.
     * @param domain
     * fully qualified host name with domain to match against.
     * @return true if matches else false.
     */
    override fun localHostOrDomainIs(host: String, domain: String): Boolean {
        return domain.startsWith(host)
    }

    /*************************************************************************
     * Tries to resolve the host name. Returns true if succeeds.
     *
     * @param host
     * is the host name from the URL.
     * @return true if resolvable else false.
     */
    override fun isResolvable(host: String): Boolean {
        try {
            InetAddress.getByName(host).hostAddress
            return true
        } catch (ex: UnknownHostException) {
            log.debug { "Hostname not resolveable $host." }
        }
        return false
    }

    /*************************************************************************
     * Returns true if the IP address of the host matches the specified IP
     * address pattern. Pattern and mask specification is done the same way as
     * for SOCKS configuration.
     *
     * Example: isInNet(host, "198.95.0.0", "255.255.0.0") is true if the IP
     * address of the host matches 198.95.*.*.
     *
     * @param host
     * a DNS host name, or IP address. If a host name is passed, it
     * will be resolved into an IP address by this function.
     * @param pattern
     * an IP address pattern in the dot-separated format.
     * @param mask
     * mask for the IP address pattern informing which parts of the
     * IP address should be matched against. 0 means ignore, 255
     * means match.
     * @return true if it matches else false.
     */
    override fun isInNet(host: String, pattern: String, mask: String): Boolean {
        var host = host
        host = dnsResolve(host)
        if (host == null || host.length == 0) {
            return false
        }
        val lhost = parseIpAddressToLong(host)
        val lpattern = parseIpAddressToLong(pattern)
        val lmask = parseIpAddressToLong(mask)
        return lhost and lmask == lpattern
    }

    /*************************************************************************
     * Convert a string representation of a IP to a long.
     * @param address to convert.
     * @return the address as long.
     */
    private fun parseIpAddressToLong(address: String): Long {
        var result: Long = 0
        val parts = address.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        var shift: Long = 24
        for (part in parts) {
            val lpart = part.toLong()
            result = result or (lpart shl shift.toInt())
            shift -= 8
        }
        return result
    }

    /*************************************************************************
     * Resolves the given DNS host name into an IP address, and returns it in
     * the dot separated format as a string.
     *
     * @param host
     * the host to resolve.
     * @return the resolved IP, empty string if not resolvable.
     */
    override fun dnsResolve(host: String): String {
        try {
            return InetAddress.getByName(host).hostAddress
        } catch (e: UnknownHostException) {
            log.debug { "DNS name not resolvable $host." }
        }
        return ""
    }

    /*************************************************************************
     * Returns the IP address of the host that the process is running on, as a
     * string in the dot-separated integer format.
     *
     * @return an IP as string.
     */
    override fun myIpAddress(): String {
        return getLocalAddressOfType(Inet4Address::class.java)
    }

    /*************************************************************************
     * Get the current IP address of the computer.
     * This will return the first address of the first network interface that is
     * a "real" IP address of the given type.
     * @param cl the type of address we are searching for.
     * @return the address as string or "" if not found.
     */
    private fun getLocalAddressOfType(cl: Class<out InetAddress>): String {
        try {
            val overrideIP = System.getProperty(OVERRIDE_LOCAL_IP)
            if (overrideIP != null && overrideIP.trim { it <= ' ' }.isNotEmpty()) {
                return overrideIP.trim { it <= ' ' }
            }
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val current = interfaces.nextElement()
                if (!current.isUp || current.isLoopback || current.isVirtual) {
                    continue
                }
                val addresses = current.inetAddresses
                while (addresses.hasMoreElements()) {
                    val adr = addresses.nextElement()
                    if (cl.isInstance(adr)) {
                        log.trace { "Local address resolved to $adr" }
                        return adr.hostAddress
                    }
                }
            }
        } catch (e: IOException) {
            log.debug {"Local address not resolvable."}
        }
        return ""
    }

    /*************************************************************************
     * Returns the number of DNS domain levels (number of dots) in the host
     * name.
     *
     * @param host
     * is the host name from the URL.
     * @return number of DNS domain levels.
     */
    override fun dnsDomainLevels(host: String): Int {
        var count = 0
        var startPos = 0
        while (host.indexOf(".", startPos + 1).also { startPos = it } > -1) {
            count++
        }
        return count
    }

    /*************************************************************************
     * Returns true if the string matches the specified shell expression.
     * Actually, currently the patterns are shell expressions, not regular
     * expressions.
     *
     * @param str
     * is any string to compare (e.g. the URL, or the host name).
     * @param shexp
     * is a shell expression to compare against.
     * @return true if the string matches, else false.
     */
    override fun shExpMatch(str: String, shexp: String): Boolean {
        val tokenizer = StringTokenizer(shexp, "*")
        var startPos = 0
        while (tokenizer.hasMoreTokens()) {
            val token = tokenizer.nextToken()
            val temp = str.indexOf(token, startPos)

            // Must start with first token
            if (startPos == 0 && !shexp.startsWith("*") && temp != 0) {
                return false
            }
            // Last one ends with last token
            if (!tokenizer.hasMoreTokens() && !shexp.endsWith("*") && !str.endsWith(token)) {
                return false
            }
            startPos = if (temp == -1) {
                return false
            } else {
                temp + token.length
            }
        }
        return true
    }

    /*************************************************************************
     * Only the first parameter is mandatory. Either the second, the third, or
     * both may be left out. If only one parameter is present, the function
     * yields a true value on the weekday that the parameter represents. If the
     * string "GMT" is specified as a second parameter, times are taken to be in
     * GMT, otherwise in local time zone. If both wd1 and wd2 are defined, the
     * condition is true if the current weekday is in between those two
     * weekdays. Bounds are inclusive. If the "GMT" parameter is specified,
     * times are taken to be in GMT, otherwise the local time zone is used.
     *
     * @param wd1
     * weekday 1 is one of SUN MON TUE WED THU FRI SAT
     * @param wd2
     * weekday 2 is one of SUN MON TUE WED THU FRI SAT
     * @param gmt
     * "GMT" for gmt time format else "undefined"
     * @return true if current day matches the criteria.
     */
    override fun weekdayRange(wd1: String?, wd2: String?, gmt: String?): Boolean {
        val useGmt = GMT.equals(wd2, ignoreCase = true) || GMT.equals(gmt, ignoreCase = true)
        val cal = getCurrentTime(useGmt)
        val currentDay = cal[Calendar.DAY_OF_WEEK] - 1
        val from = DAYS.indexOf(wd1?.uppercase(Locale.getDefault()))
        var to = DAYS.indexOf(wd2?.uppercase(Locale.getDefault()))
        if (to == -1) {
            to = from
        }
        return if (to < from) {
            currentDay >= from || currentDay <= to
        } else {
            currentDay in from..to
        }
    }

    /*************************************************************************
     * Sets a calendar with the current time. If this is set all date and time
     * based methods will use this calendar to determine the current time
     * instead of the real time. This is only be used by unit tests and is not
     * part of the public API.
     *
     * @param cal
     * a Calendar to set.
     */
    fun setCurrentTime(cal: Calendar?) {
        currentTime = cal
    }

    /*************************************************************************
     * Gets a calendar set to the current time. This is used by the date and
     * time based methods.
     *
     * @param useGmt
     * flag to indicate if the calendar is to be created in GMT time
     * or local time.
     * @return a Calendar set to the current time.
     */
    private fun getCurrentTime(useGmt: Boolean): Calendar {
        return if (currentTime != null) { // Only used for unit tests
            currentTime!!.clone() as Calendar
        } else Calendar.getInstance(if (useGmt) TimeZone.getTimeZone(GMT) else TimeZone.getDefault())
    }

    /*************************************************************************
     * Only the first parameter is mandatory. All other parameters can be left
     * out therefore the meaning of the parameters changes. The method
     * definition shows the version with the most possible parameters filled.
     * The real meaning of the parameters is guessed from it's value. If "from"
     * and "to" are specified then the bounds are inclusive. If the "GMT"
     * parameter is specified, times are taken to be in GMT, otherwise the local
     * time zone is used.
     *
     * @param day1
     * is the day of month between 1 and 31 (as an integer).
     * @param month1
     * one of JAN FEB MAR APR MAY JUN JUL AUG SEP OCT NOV DEC
     * @param year1
     * is the full year number, for example 1995 (but not 95).
     * Integer.
     * @param day2
     * is the day of month between 1 and 31 (as an integer).
     * @param month2
     * one of JAN FEB MAR APR MAY JUN JUL AUG SEP OCT NOV DEC
     * @param year2
     * is the full year number, for example 1995 (but not 95).
     * Integer.
     * @param gmt
     * "GMT" for gmt time format else "undefined"
     * @return true if the current date matches the given range.
     */
    override fun dateRange(
        day1: Any, month1: Any, year1: Any,
        day2: Any, month2: Any, year2: Any, gmt: Any
    ): Boolean {

        // Guess the parameter meanings.
        val params = mutableMapOf<String,Int?>()
        parseDateParam(params, day1)
        parseDateParam(params, month1)
        parseDateParam(params, year1)
        parseDateParam(params, day2)
        parseDateParam(params, month2)
        parseDateParam(params, year2)
        parseDateParam(params, gmt)

        // Get current date
        val useGmt = params["gmt"] != null
        val cal = getCurrentTime(useGmt)
        val current = cal.time

        // Build the "from" date
        if (params["day1"] != null) {
            cal[Calendar.DAY_OF_MONTH] = params["day1"]!!
        }
        if (params["month1"] != null) {
            cal[Calendar.MONTH] = params["month1"]!!
        }
        if (params["year1"] != null) {
            cal[Calendar.YEAR] = params["year1"]!!
        }
        val from = cal.time

        // Build the "to" date
        var to: Date
        if (params["day2"] != null) {
            cal[Calendar.DAY_OF_MONTH] = params["day2"]!!
        }
        if (params["month2"] != null) {
            cal[Calendar.MONTH] = params["month2"]!!
        }
        if (params["year2"] != null) {
            cal[Calendar.YEAR] = params["year2"]!!
        }
        to = cal.time

        // Need to increment to the next month?
        if (to.before(from)) {
            cal.add(Calendar.MONTH, +1)
            to = cal.time
        }
        // Need to increment to the next year?
        if (to.before(from)) {
            cal.add(Calendar.YEAR, +1)
            cal.add(Calendar.MONTH, -1)
            to = cal.time
        }
        return current >= from && current <= to
    }

    /*************************************************************************
     * Try to guess the type of the given parameter and put it into the params
     * map.
     *
     * @param params
     * a map to put the parsed parameters into.
     * @param value
     * to parse and specify the type for.
     */
    private fun parseDateParam(params: MutableMap<String, Int?>, value: Any) {
        if (value is Number) {
            val n = value.toInt()
            if (n <= 31) {
                // Its a day
                if (params["day1"] == null) {
                    params["day1"] = n
                } else {
                    params["day2"] = n
                }
            } else {
                // Its a year
                if (params["year1"] == null) {
                    params["year1"] = n
                } else {
                    params["year2"] = n
                }
            }
        }
        if (value is String) {
            val n = MONTH.indexOf(value.uppercase(Locale.getDefault()))
            if (n > -1) {
                // Its a month
                if (params["month1"] == null) {
                    params["month1"] = n
                } else {
                    params["month2"] = n
                }
            }
        }
        if (GMT.equals(value.toString(), ignoreCase = true)) {
            params["gmt"] = 1
        }
    }

    /*************************************************************************
     * Some parameters can be left out therefore the meaning of the parameters
     * changes. The method definition shows the version with the most possible
     * parameters filled. The real meaning of the parameters is guessed from
     * it's value. If "from" and "to" are specified then the bounds are
     * inclusive. If the "GMT" parameter is specified, times are taken to be in
     * GMT, otherwise the local time zone is used.<br></br>
     *
     * <pre>
     * timeRange(hour)
     * timeRange(hour1, hour2)
     * timeRange(hour1, min1, hour2, min2)
     * timeRange(hour1, min1, sec1, hour2, min2, sec2)
     * timeRange(hour1, min1, sec1, hour2, min2, sec2, gmt)
    </pre> *
     *
     * @param hour1
     * is the hour from 0 to 23. (0 is midnight, 23 is 11 pm.)
     * @param min1
     * minutes from 0 to 59.
     * @param sec1
     * seconds from 0 to 59.
     * @param hour2
     * is the hour from 0 to 23. (0 is midnight, 23 is 11 pm.)
     * @param min2
     * minutes from 0 to 59.
     * @param sec2
     * seconds from 0 to 59.
     * @param gmt
     * "GMT" for gmt time format else "undefined"
     * @return true if the current time matches the given range.
     */
    override fun timeRange(
        hour1: Any, min1: Any, sec1: Any,
        hour2: Any, min2: Any, sec2: Any, gmt: Any
    ): Boolean {
        val useGmt = (GMT.equals(min1.toString(), ignoreCase = true)
                || GMT.equals(sec1.toString(), ignoreCase = true)
                || GMT.equals(min2.toString(), ignoreCase = true)
                || GMT.equals(gmt.toString(), ignoreCase = true))
        val cal = getCurrentTime(useGmt)
        cal[Calendar.MILLISECOND] = 0
        val current = cal.time
        val from: Date
        var to: Date
        if (sec2 is Number) {
            cal[Calendar.HOUR_OF_DAY] = (hour1 as Number).toInt()
            cal[Calendar.MINUTE] = (min1 as Number).toInt()
            cal[Calendar.SECOND] = (sec1 as Number).toInt()
            from = cal.time
            cal[Calendar.HOUR_OF_DAY] = (hour2 as Number).toInt()
            cal[Calendar.MINUTE] = (min2 as Number).toInt()
            cal[Calendar.SECOND] = sec2.toInt()
            to = cal.time
        } else if (hour2 is Number) {
            cal[Calendar.HOUR_OF_DAY] = (hour1 as Number).toInt()
            cal[Calendar.MINUTE] = (min1 as Number).toInt()
            cal[Calendar.SECOND] = 0
            from = cal.time
            cal[Calendar.HOUR_OF_DAY] = (sec1 as Number).toInt()
            cal[Calendar.MINUTE] = hour2.toInt()
            cal[Calendar.SECOND] = 59
            to = cal.time
        } else if (min1 is Number) {
            cal[Calendar.HOUR_OF_DAY] = (hour1 as Number).toInt()
            cal[Calendar.MINUTE] = 0
            cal[Calendar.SECOND] = 0
            from = cal.time
            cal[Calendar.HOUR_OF_DAY] = min1.toInt()
            cal[Calendar.MINUTE] = 59
            cal[Calendar.SECOND] = 59
            to = cal.time
        } else {
            cal[Calendar.HOUR_OF_DAY] = (hour1 as Number).toInt()
            cal[Calendar.MINUTE] = 0
            cal[Calendar.SECOND] = 0
            from = cal.time
            cal[Calendar.HOUR_OF_DAY] = hour1.toInt()
            cal[Calendar.MINUTE] = 59
            cal[Calendar.SECOND] = 59
            to = cal.time
        }
        if (to.before(from)) {
            cal.time = to
            cal.add(Calendar.DATE, +1)
            to = cal.time
        }
        return current.compareTo(from) >= 0 && current.compareTo(to) <= 0
    }
    // Microsoft PAC extensions for IPv6 support.
    /*************************************************************************
     * isResolvableEx
     * @see com.btr.proxy.selector.pac.ScriptMethods.isResolvableEx
     */
    override fun isResolvableEx(host: String): Boolean {
        return isResolvable(host)
    }

    /*************************************************************************
     * isInNetEx
     * @see com.btr.proxy.selector.pac.ScriptMethods.isInNetEx
     */
    override fun isInNetEx(ipAddress: String?, ipPrefix: String?): Boolean {
        // TODO rossi 27.06.2011 Auto-generated method stub
        return false
    }

    /*************************************************************************
     * dnsResolveEx
     * @see com.btr.proxy.selector.pac.ScriptMethods.dnsResolveEx
     */
    override fun dnsResolveEx(host: String): String {
        val result = StringBuilder()
        try {
            val list = InetAddress.getAllByName(host)
            for (inetAddress in list) {
                result.append(inetAddress.hostAddress)
                result.append("; ")
            }
        } catch (e: UnknownHostException) {
            log.debug {"DNS name not resolvable $host" }
        }
        return result.toString()
    }

    /*************************************************************************
     * myIpAddressEx
     * @see com.btr.proxy.selector.pac.ScriptMethods.myIpAddressEx
     */
    override fun myIpAddressEx(): String {
        return getLocalAddressOfType(Inet6Address::class.java)
    }

    /*************************************************************************
     * sortIpAddressList
     * @see com.btr.proxy.selector.pac.ScriptMethods.sortIpAddressList
     */
    override fun sortIpAddressList(ipAddressList: String?): String {
        if (ipAddressList == null || ipAddressList.trim { it <= ' ' }.isEmpty()) {
            return ""
        }
        val ipAddressToken = ipAddressList.split(";".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        val parsedAddresses: MutableList<InetAddress> = ArrayList()
        for (ip in ipAddressToken) {
            try {
                parsedAddresses.add(InetAddress.getByName(ip))
            } catch (e: UnknownHostException) {
                // TODO rossi 01.11.2011 Auto-generated catch block
                e.printStackTrace()
            }
        }
        Collections.sort(parsedAddresses, null)
        // TODO rossi 27.06.2011 Implement me.
        return ipAddressList
    }

    /*************************************************************************
     * getClientVersion
     * @see com.btr.proxy.selector.pac.ScriptMethods.getClientVersion
     */
    override val clientVersion: String
        get() = "1.0"

    companion object {
        const val OVERRIDE_LOCAL_IP = "com.btr.proxy.pac.overrideLocalIP"
        private const val GMT = "GMT"
        private val DAYS = Collections.unmodifiableList(
            listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
        )
        private val MONTH = Collections.unmodifiableList(
            listOf(
                "JAN",
                "FEB",
                "MAR",
                "APR",
                "MAY",
                "JUN",
                "JUL",
                "AUG",
                "SEP",
                "OCT",
                "NOV",
                "DEC"
            )
        )
    }
}
