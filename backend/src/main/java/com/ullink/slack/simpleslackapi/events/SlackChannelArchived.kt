package com.ullink.slack.simpleslackapi.events

import com.ullink.slack.simpleslackapi.SlackUser

public interface SlackChannelArchived : SlackChannelEvent {
    public fun getUser(): SlackUser
}
