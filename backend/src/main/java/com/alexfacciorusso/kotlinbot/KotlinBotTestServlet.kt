package com.alexfacciorusso.kotlinbot

import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

public class KotlinBotTestServlet : HttpServlet() {
    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        val session = SlackSessionFactory.createWebSocketSlackSession("authenticationtoken");
        session.connect();
    }
}

