package com.ullink.slack.simpleslackapi.events

import com.ullink.slack.simpleslackapi.SlackChannel

public interface SlackChannelEvent : SlackEvent {
    public fun getSlackChannel(): SlackChannel
}
