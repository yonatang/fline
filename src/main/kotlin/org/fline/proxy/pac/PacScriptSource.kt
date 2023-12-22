package org.fline.proxy.pac

import java.io.IOException


interface PacScriptSource {
    /*************************************************************************
     * Gets the PAC script content as String.
     * @return a script.
     * @throws IOException on read error.
     */
    val scriptContent: String

    /*************************************************************************
     * Checks if the content of the script is valid and if it is possible
     * to use this script source for a PAC selector.
     * Note that this might trigger a download of the script content from
     * a remote location.
     * @return true if everything is fine, else false.
     */
    val isScriptValid: Boolean
}
