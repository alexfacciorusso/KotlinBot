package com.ullink.slack.simpleslackapi.impl

import com.ullink.slack.simpleslackapi.SlackChannel
import com.ullink.slack.simpleslackapi.events.SlackChannelDeleted
import com.ullink.slack.simpleslackapi.events.SlackEventType

class SlackChannelDeletedImpl(private val slackChannel: SlackChannel) : SlackChannelDeleted {

    override fun getSlackChannel(): SlackChannel {
        return slackChannel
    }

    override fun getEventType(): SlackEventType {
        return SlackEventType.SLACK_CHANNEL_DELETED
    }
}
