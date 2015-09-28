package com.ullink.slack.simpleslackapi.impl

import com.ullink.slack.simpleslackapi.SlackChannel
import com.ullink.slack.simpleslackapi.SlackUser
import com.ullink.slack.simpleslackapi.events.SlackChannelUnarchived
import com.ullink.slack.simpleslackapi.events.SlackEventType

class SlackChannelUnarchivedImpl(private val slackChannel: SlackChannel, private val slackuser: SlackUser) : SlackChannelUnarchived {

    override fun getSlackChannel(): SlackChannel {
        return slackChannel
    }

    override fun getUser(): SlackUser {
        return slackuser
    }

    override fun getEventType(): SlackEventType {
        return SlackEventType.SLACK_CHANNEL_UNARCHIVED
    }

}
