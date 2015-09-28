package com.ullink.slack.simpleslackapi.impl

import com.fasterxml.jackson.databind.JsonNode
import com.ullink.slack.simpleslackapi.SlackChannel
import com.ullink.slack.simpleslackapi.SlackSession
import com.ullink.slack.simpleslackapi.events.*
import java.util.*

object SlackJSONMessageParser {
    fun decode(slackSession: SlackSession, obj: JsonNode): SlackEvent {
        val type = obj.get("type") as String ?: return parseSlackReply(obj)
        val eventType = EventType.getByCode(type)
        when (eventType) {
            EventType.MESSAGE -> return extractMessageEvent(slackSession, obj)
            EventType.CHANNEL_CREATED -> return extractChannelCreatedEvent(slackSession, obj)
            EventType.CHANNEL_ARCHIVE -> return extractChannelArchiveEvent(slackSession, obj)
            EventType.CHANNEL_DELETED -> return extractChannelDeletedEvent(slackSession, obj)
            EventType.CHANNEL_RENAME -> return extractChannelRenamedEvent(slackSession, obj)
            EventType.CHANNEL_UNARCHIVE -> return extractChannelUnarchiveEvent(slackSession, obj)
            EventType.GROUP_JOINED -> return extractGroupJoinedEvent(slackSession, obj)
            else -> return SlackEvent.UNKNOWN_EVENT
        }
    }

    private fun extractGroupJoinedEvent(slackSession: SlackSession, obj: JsonNode): SlackGroupJoined {
        val channelJSONObject = obj["channel"]
        val slackChannel = parseChannelDescription(channelJSONObject)
        return SlackGroupJoinedImpl(slackChannel)
    }

    private fun extractChannelRenamedEvent(slackSession: SlackSession, obj: JsonNode): SlackChannelRenamed {
        return SlackChannelRenamedImpl(slackSession.findChannelById(obj["channel"].textValue()), obj["name"].textValue())
    }

    private fun extractChannelDeletedEvent(slackSession: SlackSession, obj: JsonNode): SlackChannelDeleted {
        return SlackChannelDeletedImpl(slackSession.findChannelById(obj["channel"].textValue()))
    }

    private fun extractChannelUnarchiveEvent(slackSession: SlackSession, obj: JsonNode): SlackChannelUnarchived {
        return SlackChannelUnarchivedImpl(slackSession.findChannelById(obj["channel"].textValue()), slackSession.findUserById(obj.get("user").textValue()))
    }

    private fun extractChannelArchiveEvent(slackSession: SlackSession, obj: JsonNode): SlackChannelArchived {
        return SlackChannelArchivedImpl(slackSession.findChannelById(obj["channel"].textValue()), slackSession.findUserById(obj["user"].textValue()))
    }

    private fun extractChannelCreatedEvent(slackSession: SlackSession, obj: JsonNode): SlackChannelCreated {
        val channelJSONObject = obj["channel"]
        val channel = parseChannelDescription(channelJSONObject)
        val creatorId = channelJSONObject["creator"].textValue()
        val user = slackSession.findUserById(creatorId)
        return SlackChannelCreatedImpl(channel, user)
    }

    private fun extractMessageEvent(slackSession: SlackSession, obj: JsonNode): SlackEvent {
        val channel = getChannel(slackSession, obj["channel"].textValue())

        val subType = SlackMessageSubType.getByCode(obj["subtype"].textValue() as String)
        when (subType) {
            SlackJSONMessageParser.SlackMessageSubType.MESSAGE_CHANGED -> return parseMessageUpdated(obj, channel, obj["ts"].textValue())
            SlackJSONMessageParser.SlackMessageSubType.MESSAGE_DELETED -> return parseMessageDeleted(obj, channel, obj.get("ts").textValue())
            SlackJSONMessageParser.SlackMessageSubType.BOT_MESSAGE -> return parseBotMessage(obj, channel, obj.get("ts").textValue(), slackSession)
            else -> return parseMessagePublished(obj, channel, obj["ts"].textValue(), slackSession)
        }
    }

    private fun parseSlackReply(obj: JsonNode): SlackEvent {
        return SlackReplyImpl(obj["ok"].booleanValue(), obj["reply_to"].longValue(), obj["ts"].textValue())
    }

    private fun getChannel(slackSession: SlackSession, channelId: String): SlackChannel {
        if (channelId.startsWith("D")) {
            // direct messaging, on the fly channel creation
            return SlackChannelImpl(channelId, channelId, "", "", true)
        } else {
            return slackSession.findChannelById(channelId)
        }
    }

    private fun parseMessageUpdated(obj: JsonNode, channel: SlackChannel, ts: String): SlackMessageUpdatedImpl {
        val message = obj["message"]
        val text = message["text"].textValue()
        val messageTs = message["ts"].textValue()
        val toto = SlackMessageUpdatedImpl(channel, messageTs, ts, text)
        return toto
    }

    private fun parseMessageDeleted(obj: JsonNode, channel: SlackChannel, ts: String): SlackMessageDeletedImpl {
        return SlackMessageDeletedImpl(channel, obj["deleted_ts"].textValue(), ts)
    }

    private fun parseBotMessage(obj: JsonNode, channel: SlackChannel, ts: String, slackSession: SlackSession): SlackMessagePostedImpl {
        val user = slackSession.findUserById(obj["bot_id"].textValue())
        return SlackMessagePostedImpl(obj["text"].textValue(), user, user, channel, ts)
    }

    private fun parseMessagePublished(obj: JsonNode, channel: SlackChannel, ts: String, slackSession: SlackSession): SlackMessagePostedImpl {
        val user = slackSession.findUserById(obj["user"].textValue())
        return SlackMessagePostedImpl(obj["text"].textValue(), user, user, channel, ts)
    }

    private fun parseChannelDescription(channelJSONObject: JsonNode): SlackChannel {
        val topic: String? = null // TODO
        val purpose: String? = null // TODO
        return SlackChannelImpl(channelJSONObject["id"].textValue(), channelJSONObject["name"].textValue(), topic, purpose, true)
    }

    public enum class SlackMessageSubType(code: String) {
        CHANNEL_JOIN("channel_join"), MESSAGE_CHANGED("message_changed"), MESSAGE_DELETED("message_deleted"), BOT_MESSAGE("bot_message"), OTHER("-");

        var code: String

        init {
            this.code = code
        }

        companion object {

            private val CODE_MAP = HashMap<String, SlackMessageSubType>()

            init {
                for (enumValue in SlackMessageSubType.values()) {
                    CODE_MAP.put(enumValue.code, enumValue)
                }
            }

            public fun getByCode(code: String): SlackMessageSubType {
                val toReturn = CODE_MAP.get(code) ?: return OTHER
                return toReturn
            }
        }
    }

}
