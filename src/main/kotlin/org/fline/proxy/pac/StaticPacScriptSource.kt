package org.fline.proxy.pac

class StaticPacScriptSource(
    override val scriptContent: String,
    override val isScriptValid: Boolean
) : PacScriptSource
