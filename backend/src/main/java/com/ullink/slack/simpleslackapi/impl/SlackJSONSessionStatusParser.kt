package com.ullink.slack.simpleslackapi.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.util.HashMap
import com.ullink.slack.simpleslackapi.SlackChannel
import com.ullink.slack.simpleslackapi.SlackUser
import com.ullink.slack.simpleslackapi.SlackPersona

class SlackJSONSessionStatusParser(private val toParse: String) {

    private val channels = HashMap<String, SlackChannel>()
    private val users = HashMap<String, SlackUser>()

    public var sessionPersona: SlackPersona? = null
        private set

    public var webSocketURL: String? = null
        private set

    public var error: String? = null
        private set

    fun getChannels(): Map<String, SlackChannel> {
        return channels
    }

    fun getUsers(): Map<String, SlackUser> {
        return users
    }

    fun parse() {
        print("parsing session status : " + toParse)

        val mapper = ObjectMapper()
        val jsonResponse = mapper.readTree(toParse)
        val ok = jsonResponse.get("ok").booleanValue()
        if (java.lang.Boolean.FALSE == ok) {
            error = jsonResponse.get("error").textValue()
            return
        }
        val usersJson = jsonResponse.get("users")

        for (jsonObject in usersJson) {
            val jsonUser = jsonObject
            val slackUser = SlackJSONParsingUtils.buildSlackUser(jsonUser)
            print("slack user found : " + slackUser.id)
            users.put(slackUser.id, slackUser)
        }

        val botsJson = jsonResponse.get("bots")
        if (botsJson != null) {
            for (jsonObject in botsJson) {
                val jsonBot = jsonObject
                val slackUser = SlackJSONParsingUtils.buildSlackUser(jsonBot)
                print("slack bot found : " + slackUser.id)
                users.put(slackUser.id, slackUser)
            }
        }

        val channelsJson = jsonResponse.get("channels")

        for (jsonObject in channelsJson) {
            val jsonChannel = jsonObject
            val channel = SlackJSONParsingUtils.buildSlackChannel(jsonChannel, users)
            print("slack public channel found : " + channel.id)
            channels.put(channel.id, channel)
        }

        val groupsJson = jsonResponse.get("groups")

        for (jsonObject in groupsJson) {
            val jsonChannel = jsonObject
            val channel = SlackJSONParsingUtils.buildSlackChannel(jsonChannel, users)
            print("slack private group found : " + channel.id)
            channels.put(channel.id, channel)
        }

        val imsJson = jsonResponse.get("ims")

        for (jsonObject in imsJson) {
            val jsonChannel = jsonObject
            val channel = SlackJSONParsingUtils.buildSlackImChannel(jsonChannel, users)
            print("slack im channel found : " + channel.id)
            channels.put(channel.id, channel)
        }

        val selfJson = jsonResponse.get("self")
        sessionPersona = SlackJSONParsingUtils.buildSlackUser(selfJson)


        webSocketURL = jsonResponse.get("url") as String

    }
}
