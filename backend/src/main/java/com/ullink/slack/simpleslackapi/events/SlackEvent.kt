package com.ullink.slack.simpleslackapi.events

public interface SlackEvent {

    public fun getEventType(): SlackEventType

    companion object {
        public val UNKNOWN_EVENT: SlackEvent = object : SlackEvent {

            override fun getEventType(): SlackEventType {
                return SlackEventType.UNKNOWN
            }
        }
    }
}
