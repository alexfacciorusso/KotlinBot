package com.ullink.slack.simpleslackapi.impl

import java.util.ArrayList
import java.util.HashSet
import com.ullink.slack.simpleslackapi.SlackChannel
import com.ullink.slack.simpleslackapi.SlackUser

class SlackChannelImpl(private val id: String, private val name: String?, private val topic: String?, private val purpose: String?, private val direct: Boolean) : SlackChannel {
    private val members = HashSet<SlackUser>()

    fun addUser(user: SlackUser) {
        members.add(user)
    }

    fun removeUser(user: SlackUser) {
        members.remove(user)
    }

    override fun getId(): String {
        return id
    }

    override fun getName(): String? {
        return name
    }

    override fun getMembers(): Collection<SlackUser> {
        return ArrayList(members)
    }

    override fun getTopic(): String? {
        return topic
    }

    override fun toString(): String {
        return "SlackChannelImpl{topic='$topic', purpose='$purpose', id='$id', name='$name'}"
    }

    override fun getPurpose(): String? {
        return purpose
    }

    override fun isDirect(): Boolean {
        return direct
    }
}
