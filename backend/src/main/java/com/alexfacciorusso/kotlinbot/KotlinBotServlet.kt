package com.alexfacciorusso.kotlinbot

import com.alexfacciorusso.kotlinbot.model.Bug
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.appengine.api.urlfetch.HTTPHeader
import com.google.appengine.api.urlfetch.HTTPRequest
import com.google.appengine.api.urlfetch.URLFetchServiceFactory
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

public class KotlinBotServlet : HttpServlet() {
    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.writer.print("Method not supported")
    }

    fun makeYouTrackBugGetRequest(query: String? = "", maxItems: Int = 5): String? {
        try {
            val url = URL("https://youtrack.jetbrains.com/rest/issue/byproject/KT?max=$maxItems&filter=$query")
            val request = HTTPRequest(url)
            request.setHeader(HTTPHeader("Accept", "application/json"))
            val response = URLFetchServiceFactory.getURLFetchService().fetch(request)
            return response.content.toString("UTF-8")
        } catch (e: MalformedURLException) {
        } catch (e: IOException) {
        }
        return null
    }

    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        var token = req.getParameter("token")
        val command = req.getParameter("command")
        val query = req.getParameter("text")

        if (token == getToken()) {
            when (command) {
                "/ktbug" -> {
                    if (query.isNotBlank()) {
                        postKtbug(resp, query)
                    } else {
                        resp.writer.print("No query.")
                    }
                }
            }
        } else {
            resp.writer.print("Token not accepted.")
        }
    }

    /**
     * The /ktbug command logic.
     *
     * @param resp the response object
     * @param query the query from user
     */
    private fun postKtbug(resp: HttpServletResponse, query: String?) {
        val result = makeYouTrackBugGetRequest(query)
        val tree = ObjectMapper().readTree(result)
        val bugs = parseBugsTree(tree)

        if (bugs.isEmpty()) {
            resp.writer.print("No results.")
        } else for (bug in bugs) {
            with(bug) {
                resp.writer.println("*Reporter name:* $reporterFullname ($reporterNickname)\n" +
                        "*Summary:* $summary\n*Link:* <$link>\n")
            }
        }
    }

    private fun parseBugsTree(tree: JsonNode): List<Bug> {
        val list = arrayListOf<Bug>()

        for (el in tree) {
            val id = el["id"].textValue()
            var summary: String = ""
            var description: String = ""
            var link: String = "https://youtrack.jetbrains.com/issue/$id"
            var reporterFullname: String = ""
            var reporterNickname: String = ""

            for (field in el["field"]) {
                val value = field["value"].textValue()
                when (field["name"].textValue()) {
                    "summary" -> summary = value
                    "description" -> description = value
                    "reporterFullName" -> reporterFullname = value
                    "reporterName" -> reporterNickname = value
                }
            }
            list.add(Bug(summary, description, link, reporterFullname, reporterNickname))
        }

        return list
    }
}

