package org.fline.proxy.pac

import org.fline.proxy.ProxyException


class ProxyEvaluationException : ProxyException {
    /*************************************************************************
     * Constructor
     */
    constructor() : super() {}

    /*************************************************************************
     * Constructor
     * @param message the error message.
     * @param cause the causing exception for exception chaining.
     */
    constructor(message: String?, cause: Throwable?) : super(message, cause) {}

    /*************************************************************************
     * Constructor
     * @param message the error message.
     */
    constructor(message: String?) : super(message) {}

    /*************************************************************************
     * Constructor
     * @param cause the causing exception for exception chaining.
     */
    constructor(cause: Throwable?) : super(cause) {}

    companion object {
        private const val serialVersionUID = 1L
    }
}
