package com.ullink.slack.simpleslackapi.impl

import com.ullink.slack.simpleslackapi.SlackChannel
import com.ullink.slack.simpleslackapi.SlackUser
import com.ullink.slack.simpleslackapi.events.SlackChannelArchived
import com.ullink.slack.simpleslackapi.events.SlackEventType

class SlackChannelArchivedImpl(private val slackChannel: SlackChannel, private val slackuser: SlackUser) : SlackChannelArchived {

    override fun getSlackChannel(): SlackChannel {
        return slackChannel
    }

    override fun getUser(): SlackUser {
        return slackuser
    }

    override fun getEventType(): SlackEventType {
        return SlackEventType.SLACK_CHANNEL_ARCHIVED
    }

}
