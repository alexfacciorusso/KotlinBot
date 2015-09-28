package com.ullink.slack.simpleslackapi.events

import java.util.HashMap

public enum class EventType(public val code: String) {
    MESSAGE("message"),
    CHANNEL_CREATED("channel_created"),
    CHANNEL_DELETED("channel_deleted"),
    CHANNEL_RENAME("channel_rename"),
    CHANNEL_ARCHIVE("channel_archive"),
    CHANNEL_UNARCHIVE("channel_unarchive"),
    GROUP_JOINED("group_joined"),
    OTHER("-");


    companion object {

        private val CODE_MAP = HashMap<String, EventType>()

        init {
            for (enumValue in EventType.values()) {
                CODE_MAP.put(enumValue.code, enumValue)
            }
        }

        public fun getByCode(code: String): EventType {
            val toReturn = CODE_MAP.get(code) ?: return OTHER
            return toReturn
        }
    }

}
