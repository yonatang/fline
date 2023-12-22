package org.fline.proxy.pac

import io.github.oshai.kotlinlogging.KotlinLogging
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.script.ScriptException


/*****************************************************************************
 * PAC parser using the Rhino JavaScript engine bundled with Java 1.6<br></br>
 * If you need PAC support with Java 1.5 then you should have a look at
 * RhinoPacScriptParser.
 *
 * More information about PAC can be found there:<br></br>
 * [Proxy_auto-config](http://en.wikipedia.org/wiki/Proxy_auto-config)<br></br>
 * [web-browser-auto-proxy-configuration](http://homepages.tesco.net/~J.deBoynePollard/FGA/web-browser-auto-proxy-configuration.html)
 *
 * @author Bernd Rosstauscher (proxyvole@rosstauscher.de) Copyright 2009
 */
class JavaxPacScriptParser(source: PacScriptSource) : PacScriptParser {
    private val source: PacScriptSource
    private val engine: ScriptEngine

    private val log = KotlinLogging.logger { }
    /*************************************************************************
     * Constructor
     *
     * @param source
     * the source for the PAC script.
     * @throws ProxyEvaluationException
     * on error.
     */
    init {
        this.source = source
        engine = setupEngine()
    }

    /*************************************************************************
     * Initializes the JavaScript engine and adds aliases for the functions
     * defined in ScriptMethods.
     *
     * @throws ProxyEvaluationException
     * on error.
     */
//    @Throws(ProxyEvaluationException::class)
    private fun setupEngine(): ScriptEngine {
        val mng = ScriptEngineManager()
        val engine = mng.getEngineByMimeType("text/javascript")
        engine.put(SCRIPT_METHODS_OBJECT, PacScriptMethods())
        val scriptMethodsClazz: Class<*> = ScriptMethods::class.java
        val scriptMethods = scriptMethodsClazz.methods
        for (method in scriptMethods) {
            val name = method.name
            val args = method.parameterTypes.size
            val toEval = StringBuilder(name).append(" = function(")
            for (i in 0 until args) {
                if (i > 0) {
                    toEval.append(",")
                }
                toEval.append("arg").append(i)
            }
            toEval.append(") {return ")
            var functionCall = buildFunctionCallCode(name, args)

            // If return type is java.lang.String convert it to a JS string
            if (String::class.java.isAssignableFrom(method.returnType)) {
                functionCall = "String($functionCall)"
            }
            toEval.append(functionCall).append("; }")
            try {
                engine.eval(toEval.toString())
            } catch (e: ScriptException) {
                log.error(e) { "JS evaluation error when creating alias for $name." }
                throw ProxyEvaluationException(
                    "Error setting up script engine", e
                )
            }
        }
        return engine
    }

    /*************************************************************************
     * Builds a JavaScript code snippet to call a function that we bind.
     * @param functionName of the bound function
     * @param args of the bound function
     * @return the JS code to invoke the method.
     */
    private fun buildFunctionCallCode(functionName: String, args: Int): String {
        val functionCall = StringBuilder()
        functionCall.append(SCRIPT_METHODS_OBJECT)
            .append(".").append(functionName).append("(")
        for (i in 0 until args) {
            if (i > 0) {
                functionCall.append(",")
            }
            functionCall.append("arg").append(i)
        }
        functionCall.append(")")
        return functionCall.toString()
    }

    /***************************************************************************
     * Gets the source of the PAC script used by this parser.
     *
     * @return a PacScriptSource.
     */
    override val scriptSource: PacScriptSource
        get() = source

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
//    @Throws(ProxyEvaluationException::class)
    override fun evaluate(url: String, host: String): String {
        return try {
            val script: StringBuilder = StringBuilder(
                source.scriptContent
            )
            val evalMethod = " ;FindProxyForURL (\"$url\",\"$host\")"
            script.append(evalMethod)
            val result = engine.eval(script.toString())
            result as String
        } catch (e: Exception) {
            log.error(e) { "JS evaluation error." }
            throw ProxyEvaluationException(
                "Error while executing PAC script: " + e.message, e
            )
        }
    }

    companion object {
        const val SCRIPT_METHODS_OBJECT = "__pacutil"
    }
}
