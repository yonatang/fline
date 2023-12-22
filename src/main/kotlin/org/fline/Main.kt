package org.fline

//import javafx.application.Application
//import javafx.application.Platform
import javafx.stage.Stage
import kotlinx.coroutines.runBlocking
import net.rgielen.fxweaver.spring.boot.autoconfigure.FxWeaverAutoConfiguration
import org.fline.proxy.CoroutineProxyRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.font.FontRenderContext
import java.awt.font.LineMetrics
import java.awt.image.BufferedImage


@SpringBootApplication
//@ImportAutoConfiguration(classes = [FxWeaverAutoConfiguration::class])
class Main

fun main(vararg args: String) {
    System.setProperty("apple.awt.UIElement", "true")
    java.awt.Toolkit.getDefaultToolkit()
    runApplication<org.fline.Main>(*args)
//    SpringApplication.run(Main::class.java, *args)
//    Application.launch(HelloApplication::class.java, *args)
}

//class HelloApplication() : Application() {
//    private lateinit var applicationContext: ConfigurableApplicationContext
//
////    val proxyRunner = CoroutineProxyRunner()
//
//    override fun init() {
//        applicationContext = SpringApplicationBuilder(Main::class.java).run()
//    }
//
//    override fun start(stage: Stage) = runBlocking {
//        Platform.setImplicitExit(false);
//        applicationContext.publishEvent(StageReadyEvent(stage));
//    }
//
//    override fun stop() {
//        println("Stop!")
////        applicationContext.stop()
////        Platform.exit()
////        proxyRunner.stop()
//    }
//
//}


data class StageReadyEvent(val stage: Stage)
