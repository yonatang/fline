package org.fline.proxy

open class ProxyException : Exception {
    /*************************************************************************
     * Constructor
     */
    constructor() : super() {}

    /*************************************************************************
     * Constructor
     * @param message the error message
     * @param cause the causing exception for chaining exceptions.
     */
    constructor(message: String?, cause: Throwable?) : super(message, cause) {}

    /*************************************************************************
     * Constructor
     * @param message the error message
     */
    constructor(message: String?) : super(message) {}

    /*************************************************************************
     * Constructor
     * @param cause the causing exception for chaining exceptions.
     */
    constructor(cause: Throwable?) : super(cause) {}

    companion object {
        private const val serialVersionUID = 1L
    }
}
