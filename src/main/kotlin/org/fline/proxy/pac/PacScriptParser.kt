package org.fline.proxy.pac

/***************************************************************************
 * Common interface for PAC script parsers.
 *
 * @author Bernd Rosstauscher (proxyvole@rosstauscher.de) Copyright 2009
 */
interface PacScriptParser {
    /***************************************************************************
     * Gets the source of the PAC script used by this parser.
     *
     * @return a PacScriptSource.
     */
    val scriptSource: PacScriptSource

    /*************************************************************************
     * Evaluates the given URL and host against the PAC script.
     *
     * @param url
     * the URL to evaluate.
     * @param host
     * the host name part of the URL.
     * @return the script result.
     * @throws ProxyEvaluationException
     * on execution error.
     */
    fun evaluate(url: String, host: String): String
}
