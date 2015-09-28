package com.ullink.slack.simpleslackapi.impl

import com.ullink.slack.simpleslackapi.SlackBot

@Deprecated("")
class SlackBotImpl(id: String, userName: String, realName: String, userMail: String, deleted: Boolean, admin: Boolean, owner: Boolean, primaryOwner: Boolean, restricted: Boolean, ultraRestricted: Boolean) : SlackPersonaImpl(id, userName, realName, userMail, deleted, admin, owner, primaryOwner, restricted, ultraRestricted, true, null, null, 0), SlackBot
