package com.ullink.slack.simpleslackapi.events

import com.ullink.slack.simpleslackapi.SlackChannel

public interface SlackMessageDeleted : SlackMessageEvent {
    public fun getChannel(): SlackChannel
    public fun getMessageTimestamp(): String
}
