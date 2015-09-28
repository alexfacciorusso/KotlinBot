package com.ullink.slack.simpleslackapi.impl

import com.fasterxml.jackson.databind.JsonNode
import com.ullink.slack.simpleslackapi.SlackUser

object SlackJSONParsingUtils {

    fun buildSlackUser(jsonUser: JsonNode): SlackUser {
        val id = jsonUser.get("id").textValue()
        val name = jsonUser.get("name").textValue()
        val realName = jsonUser.get("real_name").textValue()
        val tz = jsonUser.get("tz").textValue()
        val tzLabel = jsonUser.get("tz_label").textValue()
        val tzOffset = jsonUser.get("tz_offset").longValue()
        val deleted = ifNullFalse(jsonUser, "deleted")
        val admin = ifNullFalse(jsonUser, "is_admin")
        val owner = ifNullFalse(jsonUser, "is_owner")
        val primaryOwner = ifNullFalse(jsonUser, "is_primary_owner")
        val restricted = ifNullFalse(jsonUser, "is_restricted")
        val ultraRestricted = ifNullFalse(jsonUser, "is_ultra_restricted")
        val bot = ifNullFalse(jsonUser, "is_bot")
        val profileJSON = jsonUser.get("profile")
        var email = ""
        if (profileJSON != null) {
            email = profileJSON.get("email").textValue()
        }
        return SlackUserImpl(id, name, realName, email, deleted!!, admin!!, owner!!, primaryOwner!!, restricted!!, ultraRestricted!!, bot!!, tz, tzLabel, tzOffset.toInt())
    }

    private fun ifNullFalse(jsonUser: JsonNode, field: String): Boolean? {
        if (jsonUser.get(field) == null) {
            return false
        }
        return true
    }

    fun buildSlackChannel(jsonChannel: JsonNode, knownUsersById: Map<String, SlackUser>): SlackChannelImpl {
        val topic: String? = null // TODO
        val purpose: String? = null // TODO
        val toReturn = SlackChannelImpl(jsonChannel.get("id").textValue(), jsonChannel.get("name").textValue(), topic, purpose, false)
        val membersJson = jsonChannel.get("members")
        if (membersJson != null) {
            for (jsonMembersObject in membersJson) {
                val user = knownUsersById[jsonMembersObject]
                if (user != null) toReturn.addUser(user)
            }
        }
        return toReturn
    }

    fun buildSlackImChannel(jsonChannel: JsonNode, knownUsersById: Map<String, SlackUser>): SlackChannelImpl {
        val toReturn = SlackChannelImpl(jsonChannel.get("id").textValue(), null, null, null, true)
        val user = knownUsersById.get(jsonChannel.get("user"))
        if (user != null) toReturn.addUser(user)
        return toReturn
    }

}// Helper class
