package com.ullink.slack.simpleslackapi.impl

public class SlackChatConfiguration private constructor() {
    enum class Avatar {
        DEFAULT, EMOJI, ICON_URL
    }

    protected var asUser: Boolean = false
    protected var avatar: Avatar = Avatar.DEFAULT
    protected var userName: String? = null
    protected var avatarDescription: String? = null

    public fun asUser(): SlackChatConfiguration {
        asUser = true
        avatar = Avatar.DEFAULT
        avatarDescription = null
        return this
    }

    public fun withIcon(iconURL: String): SlackChatConfiguration {
        asUser = false
        avatar = Avatar.ICON_URL
        avatarDescription = iconURL
        return this
    }

    public fun withName(name: String): SlackChatConfiguration {
        asUser = false
        userName = name
        return this
    }

    public fun withEmoji(emoji: String): SlackChatConfiguration {
        asUser = false
        avatar = Avatar.EMOJI
        avatarDescription = emoji
        return this
    }

    companion object {

        public fun getConfiguration(): SlackChatConfiguration {
            return SlackChatConfiguration()
        }
    }

}
