package org.fline.ui

import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.font.FontRenderContext
import java.awt.font.LineMetrics
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.net.URI
import kotlin.system.exitProcess

@Component
class SysTray {


    @EventListener
    fun onApplicationStartedEvent(event: ServletWebServerInitializedEvent) {
        val port = event.webServer.port
        val adminListener = OpenAdminListener(port)
        val exitListener = ExitListener()
        show(adminListener, exitListener)

        adminListener.actionPerfomed()

    }

    private class OpenAdminListener(private val port: Int) : ActionListener {
        override fun actionPerformed(e: ActionEvent) {
            actionPerfomed()
        }

        fun actionPerfomed() {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI("http://localhost:$port/admin"))
            }
        }
    }

    private class ExitListener : ActionListener {
        override fun actionPerformed(e: ActionEvent) {
            exitProcess(0)
        }
    }


    private fun show(
        adminListener: ActionListener,
        exitListener: ActionListener
    ) {
        if (!Desktop.isDesktopSupported()) {
            println("Desktop is not supported")
            return
        }
        if (!SystemTray.isSupported()) {
            println("SystemTray is not supported")
            return
        }
        val tray = SystemTray.getSystemTray()
        val trayIcon = TrayIcon(textToImage("PP", Font.decode(null), 15f))
        val rootMenu = PopupMenu()

        val configMenu = MenuItem("Config")
            .also { it.addActionListener(adminListener) }
        val exitMenu = MenuItem("Shutdown")
            .also { it.addActionListener(exitListener) }

        rootMenu.add(configMenu)
        rootMenu.add("-")
        rootMenu.add(exitMenu)

        trayIcon.popupMenu = rootMenu
        tray.add(trayIcon)
    }

    private fun textToImage(Text: String, font: Font, size: Float): BufferedImage? {
        val sizedFont = font.deriveFont(size)
        val frc = FontRenderContext(null, true, true)
        val lm: LineMetrics = sizedFont.getLineMetrics(Text, frc)
        val r2d: Rectangle2D = sizedFont.getStringBounds(Text, frc)
        val img = BufferedImage(
            Math.ceil(r2d.getWidth()).toInt(),
            Math.ceil(r2d.getHeight()).toInt(),
            BufferedImage.TYPE_INT_ARGB
        )
        val g2d = img.createGraphics()
//        g2d.setRenderingHints(RenderingProperties)
        g2d.background = Color(0, 0, 0, 0)
        g2d.color = Color.WHITE
        g2d.clearRect(0, 0, img.width, img.height)
        g2d.font = sizedFont
        g2d.drawString(Text, 0f, lm.ascent)
        g2d.dispose()
        return img
    }
}
