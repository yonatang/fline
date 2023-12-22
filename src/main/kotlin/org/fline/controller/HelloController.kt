//package org.fline.fline.controller
//
//import javafx.application.Platform
//import javafx.fxml.FXML
//import javafx.scene.control.Button
//import javafx.scene.control.Label
//import javafx.scene.control.PasswordField
//import javafx.scene.control.RadioButton
//import javafx.scene.control.TextField
//import javafx.scene.control.ToggleGroup
//import javafx.scene.layout.AnchorPane
//import javafx.stage.Stage
//import net.rgielen.fxweaver.core.FxmlView
//import org.fline.fline.proxy.*
//import org.springframework.context.ApplicationEventPublisher
//import org.springframework.context.event.EventListener
//import org.springframework.stereotype.Component
//import java.net.URI
//
//
//@Component
//@FxmlView("/hello-view.fxml")
//class HelloController(
//    private val publisher: ApplicationEventPublisher,
//    private val proxyRunner: CoroutineProxyRunner
//) {
//
//    @EventListener
//    fun onProxyRequestEvent(event: ProxyRequestEvent) {
//        Platform.runLater { welcomeText.text = "Welcome to JavaFX Application! ${event.count}" }
//    }
//
//    @FXML
//    private lateinit var welcomeText: Label
//
//    @FXML
//    private lateinit var hostTextField: TextField
//
//    @FXML
//    private lateinit var portNumberField: TextField
//
//    @FXML
//    private lateinit var usernameTextField: TextField
//
//    @FXML
//    private lateinit var passwordField: PasswordField
//
//    @FXML
//    private lateinit var ap: AnchorPane
//
//    @FXML
//    private lateinit var closeButton: Button
//
//    @FXML
//    private lateinit var pacRadioButton: RadioButton
//
//    @FXML
//    private lateinit var urlRadioButton: RadioButton
//
//    @FXML
//    private lateinit var directRadioButton: RadioButton
//
//    @FXML
//    private fun onHideButtonClick() {
//        val stage = closeButton.scene.window as Stage
//        stage.hide()
//    }
//
//    @FXML
//    fun initialize() {
//        val toggleGroup = ToggleGroup()
//        directRadioButton.toggleGroup = toggleGroup
//        urlRadioButton.toggleGroup = toggleGroup
//        pacRadioButton.toggleGroup = toggleGroup
//        pacRadioButton.isSelected=true
//        hostTextField.text=System.getenv("DEFAULTURL")
//        usernameTextField.text=System.getenv("USER")
//        passwordField.text=""
//
//    }
//
//    @FXML
//    private fun onHelloButtonClick() {
//        val proxy: URI //= URI("http://${hostTextField.text}:${portNumberField.text}")
//        val type: ProxyType
//
//        when {
//            pacRadioButton.isSelected -> {
//                type = ProxyType.PAC
//                proxy = URI(hostTextField.text)
//            }
//            urlRadioButton.isSelected -> {
//                type = ProxyType.PROXY
//                proxy = URI("http://${hostTextField.text}:${portNumberField.text}")
//            }
//            else -> {
//                proxy = URI("http://dummy.com")
//                type = ProxyType.DIRECT
//            }
//        }
//        val config = ProxyConfig(proxy, usernameTextField.text, passwordField.text, type)
//        publisher.publishEvent(ProxyConfigEvent(config))
////        publisher.publishEvent(CredentialsChangeEvent())
////        welcomeText.text = "Welcome to JavaFX Application!"// ${proxyRunner.requestCounter}"
////        welcomeText.text = "Welcome to JavaFX Application! ${proxyRunner.requestCounter}"
//    }
//}
