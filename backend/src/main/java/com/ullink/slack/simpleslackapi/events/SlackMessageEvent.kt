package com.ullink.slack.simpleslackapi.events

public interface SlackMessageEvent : SlackEvent {
    public fun getTimeStamp(): String
}
