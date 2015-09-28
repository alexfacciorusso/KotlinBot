package com.ullink.slack.simpleslackapi.impl

import com.ullink.slack.simpleslackapi.*
import java.util.ArrayList
import java.util.HashMap
import com.ullink.slack.simpleslackapi.listeners.SlackChannelArchivedListener
import com.ullink.slack.simpleslackapi.listeners.SlackChannelCreatedListener
import com.ullink.slack.simpleslackapi.listeners.SlackChannelDeletedListener
import com.ullink.slack.simpleslackapi.listeners.SlackChannelRenamedListener
import com.ullink.slack.simpleslackapi.listeners.SlackChannelUnarchivedListener
import com.ullink.slack.simpleslackapi.listeners.SlackConnectedListener
import com.ullink.slack.simpleslackapi.listeners.SlackGroupJoinedListener
import com.ullink.slack.simpleslackapi.listeners.SlackMessageDeletedListener
import com.ullink.slack.simpleslackapi.listeners.SlackMessagePostedListener
import com.ullink.slack.simpleslackapi.listeners.SlackMessageUpdatedListener

abstract class AbstractSlackSessionImpl : SlackSession {

    protected var channels: Map<String, SlackChannel> = HashMap()
    protected var users: Map<String, SlackUser> = HashMap()
    protected var sessionPersona: SlackPersona? = null

    protected var channelArchiveListener: MutableList<SlackChannelArchivedListener> = ArrayList()
    protected var channelCreateListener: MutableList<SlackChannelCreatedListener> = ArrayList()
    protected var channelDeleteListener: MutableList<SlackChannelDeletedListener> = ArrayList()
    protected var channelRenamedListener: MutableList<SlackChannelRenamedListener> = ArrayList()
    protected var channelUnarchiveListener: MutableList<SlackChannelUnarchivedListener> = ArrayList()
    protected var groupJoinedListener: MutableList<SlackGroupJoinedListener> = ArrayList()
    protected var messageDeletedListener: MutableList<SlackMessageDeletedListener> = ArrayList()
    protected var messagePostedListener: MutableList<SlackMessagePostedListener> = ArrayList()
    protected var messageUpdatedListener: MutableList<SlackMessageUpdatedListener> = ArrayList()
    protected var slackReplyListener: MutableList<SlackReplyListener> = ArrayList()
    protected var slackConnectedLinster: List<SlackConnectedListener> = ArrayList()

    override fun getChannels(): Collection<SlackChannel> {
        return ArrayList(channels.values())
    }

    override fun getUsers(): Collection<SlackUser> {
        return ArrayList(users.values())
    }

    Deprecated("")
    override fun getBots(): Collection<SlackBot> {
        val toReturn = ArrayList<SlackBot>()
        for (user in users.values()) {
            if (user.isBot) {
                toReturn.add(user)
            }
        }
        return toReturn
    }

    override fun findChannelByName(channelName: String): SlackChannel? {
        for (channel in channels.values()) {
            if (channelName == channel.name) {
                return channel
            }
        }
        return null
    }

    override fun findChannelById(channelId: String?): SlackChannel? {
        var toReturn: SlackChannel? = channels.get(channelId)
        if (toReturn == null) {
            // direct channel case
            if (channelId != null && channelId.startsWith("D")) {
                toReturn = SlackChannelImpl(channelId, "", "", "", true)
            }
        }
        return toReturn
    }

    override fun findUserById(userId: String): SlackUser? {
        return users.get(userId)
    }

    override fun findUserByUserName(userName: String): SlackUser? {
        for (user in users.values()) {
            if (userName == user.userName) {
                return user
            }
        }
        return null
    }

    override fun findUserByEmail(userMail: String): SlackUser? {
        for (user in users.values()) {
            if (userMail == user.userMail) {
                return user
            }
        }
        return null
    }

    override fun sessionPersona(): SlackPersona? {
        return sessionPersona
    }

    @Deprecated("")
    override fun findBotById(botId: String): SlackBot? {
        return users.get(botId)
    }

    override fun sendMessage(channel: SlackChannel, message: String, attachment: SlackAttachment): SlackMessageHandle {
        return sendMessage(channel, message, attachment, DEFAULT_CONFIGURATION)
    }

    override fun addchannelArchivedListener(listener: SlackChannelArchivedListener) {
        channelArchiveListener.add(listener)
    }

    override fun removeChannelArchivedListener(listener: SlackChannelArchivedListener) {
        channelArchiveListener.remove(listener)
    }

    override fun addchannelCreatedListener(listener: SlackChannelCreatedListener) {
        channelCreateListener.add(listener)
    }

    override fun removeChannelCreatedListener(listener: SlackChannelCreatedListener) {
        channelCreateListener.remove(listener)
    }

    override fun addchannelDeletedListener(listener: SlackChannelDeletedListener) {
        channelDeleteListener.add(listener)
    }

    override fun removeChannelDeletedListener(listener: SlackChannelDeletedListener) {
        channelDeleteListener.remove(listener)
    }

    override fun addChannelRenamedListener(listener: SlackChannelRenamedListener) {
        channelRenamedListener.add(listener)
    }

    override fun removeChannelRenamedListener(listener: SlackChannelRenamedListener) {
        channelRenamedListener.remove(listener)
    }

    override fun addChannelUnarchivedListener(listener: SlackChannelUnarchivedListener) {
        channelUnarchiveListener.add(listener)
    }

    override fun removeChannelUnarchivedListener(listener: SlackChannelUnarchivedListener) {
        channelUnarchiveListener.remove(listener)
    }

    override fun addMessageDeletedListener(listener: SlackMessageDeletedListener) {
        messageDeletedListener.add(listener)
    }

    override fun removeMessageDeletedListener(listener: SlackMessageDeletedListener) {
        messageDeletedListener.remove(listener)
    }

    override fun addMessagePostedListener(listener: SlackMessagePostedListener) {
        messagePostedListener.add(listener)
    }

    override fun removeMessagePostedListener(listener: SlackMessagePostedListener) {
        messagePostedListener.remove(listener)
    }

    override fun addMessageUpdatedListener(listener: SlackMessageUpdatedListener) {
        messageUpdatedListener.add(listener)
    }

    override fun removeMessageUpdatedListener(listener: SlackMessageUpdatedListener) {
        messageUpdatedListener.remove(listener)
    }

    override fun addGroupJoinedListener(listener: SlackGroupJoinedListener) {
        groupJoinedListener.add(listener)
    }

    override fun removeGroupJoinedListener(listener: SlackGroupJoinedListener) {
        groupJoinedListener.remove(listener)
    }

    fun addSlackReplyListener(listener: SlackReplyListener) {
        slackReplyListener.add(listener)
    }

    fun removeSlackReplyListener(listener: SlackReplyListener) {
        slackReplyListener.remove(listener)
    }

    companion object {

        val DEFAULT_CONFIGURATION = SlackChatConfiguration.getConfiguration().asUser()
    }

}
