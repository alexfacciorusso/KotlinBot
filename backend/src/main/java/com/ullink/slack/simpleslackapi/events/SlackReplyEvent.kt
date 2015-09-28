package com.ullink.slack.simpleslackapi.events

public interface SlackReplyEvent : SlackEvent {
    public fun isOk(): Boolean
    public fun getReplyTo(): Long
    public fun getTimestamp(): String
}
