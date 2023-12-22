//package org.fline.fline.ui
//
//import javafx.application.Platform
//import javafx.scene.Parent
//import javafx.scene.Scene
//import javafx.stage.Modality
//import javafx.stage.StageStyle
//import net.rgielen.fxweaver.core.FxWeaver
//import org.fline.fline.StageReadyEvent
//import org.fline.fline.controller.HelloController
//import org.springframework.context.ApplicationContext
//import org.springframework.context.Lifecycle
//import org.springframework.context.event.EventListener
//import org.springframework.stereotype.Component
//import kotlin.system.exitProcess
//
//@Component
//class StageInitilizer(
//    private val fxWeaver: FxWeaver,
//    private val sysTray: SysTray,
//    private val lifecycle: Lifecycle
//) {
//    @EventListener
//    fun onApplicationStartedEvent(event: StageReadyEvent) {
//        println("Started! $event")
//
////        SysTray().createSysTray()
//        val stage = event.stage
//        stage.initStyle(StageStyle.UTILITY)
//        stage.isAlwaysOnTop = true
//        stage.scene = Scene(fxWeaver.loadView(HelloController::class.java), 320.0, 400.0)
//        stage.title = "ProxyProxy Configuration"
//        stage.isMaximized = false
//        stage.isFullScreen = false
////        stage.is
//
////        stage.isAlwaysOnTop=true
////        stage.initStyle(StageStyle.TRANSPARENT)
////        stage.initStyle(StageStyle.UNDECORATED);
////        stage.setOnHiding { e->e.consume() }
////        stage.setOnHidden { e->e.consume() }
////        stage.initModality(Modality.APPLICATION_MODAL);
////        stage.setResizable(false);
////        stage.setOnCloseRequest({ e-> println("Event $e");
////            e.consume()
////            stage.hide()
////        })
//        sysTray.show({
//            stage.onCloseRequest = null
//            Platform.exit()
//            exitProcess(0)
//
//        }, { Platform.runLater { stage.show() } })
//        stage.show()
//    }
//}
