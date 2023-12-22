package org.fline.controller

import org.fline.proxy.ProxyConfig
import org.fline.proxy.ProxyConfigEvent
import org.fline.proxy.ProxyType
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.servlet.ModelAndView
import java.net.URI

@Controller
class AdminController(
    private val publisher: ApplicationEventPublisher
) {

    private var username = System.getenv("USER") ?: ""
    private var password = ""
    private var url = System.getenv("DEFAULTURL") ?: ""

    @GetMapping("/admin")
    fun admin(model: Model): ModelAndView {
        val mav = ModelAndView("admin")
        val userForm = UserForm(
            username = username,
            password = password,
            url = url
        )
        mav.addObject("userForm", userForm)
        return mav
    }

    @PostMapping("/admin")
    fun submitAdmin(@ModelAttribute("userForm") userForm: UserForm, model: Model): String {
        model.addAttribute("userForm", userForm)
        val config = ProxyConfig(URI(userForm.url), userForm.username, userForm.password, ProxyType.PAC)
        publisher.publishEvent(ProxyConfigEvent(config))
        username = userForm.username
        password = userForm.password
        url = userForm.url
        return "close"
    }
}


data class UserForm(
    var username: String = "",
    var password: String = "",
    var url: String = ""
)
