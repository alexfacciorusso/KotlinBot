package com.ullink.slack.simpleslackapi.events

import com.ullink.slack.simpleslackapi.SlackChannel

public interface SlackMessageUpdated : SlackMessageEvent {
    public fun getChannel(): SlackChannel
    public fun getMessageTimestamp(): String
    public fun getNewMessage(): String
}
