package com.ullink.slack.simpleslackapi.impl

import com.ullink.slack.simpleslackapi.SlackPersona
import com.ullink.slack.simpleslackapi.events.SlackConnected
import com.ullink.slack.simpleslackapi.events.SlackEventType

class SlackConnectedImpl(private val slackConnectedPersona: SlackPersona) : SlackConnected {

    override fun getConnectedPersona(): SlackPersona {
        return slackConnectedPersona
    }

    override fun getEventType(): SlackEventType {
        return SlackEventType.SLACK_CONNECTED
    }
}
