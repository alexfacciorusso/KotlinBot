package com.ullink.slack.simpleslackapi.impl

import com.fasterxml.jackson.databind.JsonNode

object SlackJSONReplyParser {
    fun decode(obj: JsonNode): SlackReplyImpl {
        val ok = obj.get("ok").booleanValue()
        val replyTo = obj.get("reply_to").longValue()
        val timestamp = obj.get("ts").textValue()
        return SlackReplyImpl(ok, replyTo, timestamp)
    }
}
