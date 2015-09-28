package com.ullink.slack.simpleslackapi.events

import com.ullink.slack.simpleslackapi.SlackBot
import com.ullink.slack.simpleslackapi.SlackChannel
import com.ullink.slack.simpleslackapi.SlackUser

public interface SlackMessagePosted : SlackMessageEvent {
    public fun getMessageContent(): String

    public fun getSender(): SlackUser

    public fun getBot(): SlackBot

    public fun getChannel(): SlackChannel

}
