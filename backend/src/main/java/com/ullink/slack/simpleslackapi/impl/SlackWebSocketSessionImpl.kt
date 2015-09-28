package com.ullink.slack.simpleslackapi.impl

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.ullink.slack.simpleslackapi.*
import com.ullink.slack.simpleslackapi.events.*
import com.ullink.slack.simpleslackapi.impl.SlackChatConfiguration.Avatar
import com.ullink.slack.simpleslackapi.listeners.SlackEventListener
import java.io.IOException
import java.io.InputStreamReader
import java.net.ConnectException
import java.net.Proxy
import java.net.URI
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class SlackWebSocketSessionImpl : AbstractSlackSessionImpl, SlackSession, MessageHandler.Whole<String> {

    public inner class EventDispatcher {

        fun dispatch(event: SlackEvent) {
            when (event.getEventType()) {
                SlackEventType.SLACK_CHANNEL_ARCHIVED -> dispatchImpl(event as SlackChannelArchived, channelArchiveListener)
                SlackEventType.SLACK_CHANNEL_CREATED -> dispatchImpl(event as SlackChannelCreated, channelCreateListener)
                SlackEventType.SLACK_CHANNEL_DELETED -> dispatchImpl(event as SlackChannelDeleted, channelDeleteListener)
                SlackEventType.SLACK_CHANNEL_RENAMED -> dispatchImpl(event as SlackChannelRenamed, channelRenamedListener)
                SlackEventType.SLACK_CHANNEL_UNARCHIVED -> dispatchImpl(event as SlackChannelUnarchived, channelUnarchiveListener)
                SlackEventType.SLACK_GROUP_JOINED -> dispatchImpl(event as SlackGroupJoined, groupJoinedListener)
                SlackEventType.SLACK_MESSAGE_DELETED -> dispatchImpl(event as SlackMessageDeleted, messageDeletedListener)
                SlackEventType.SLACK_MESSAGE_POSTED -> dispatchImpl(event as SlackMessagePosted, messagePostedListener)
                SlackEventType.SLACK_MESSAGE_UPDATED -> dispatchImpl(event as SlackMessageUpdated, messageUpdatedListener)
                SlackEventType.SLACK_REPLY -> dispatchImpl(event as SlackReplyEvent, slackReplyListener)
                SlackEventType.SLACK_CONNECTED -> dispatchImpl(event as SlackConnected, slackConnectedLinster)
                SlackEventType.UNKNOWN -> throw IllegalArgumentException("event not handled " + event)
            }
        }

        private fun <E : SlackEvent, L : SlackEventListener<E>> dispatchImpl(event: E, listeners: List<L>) {
            for (listener in listeners) {
                listener.onEvent(event, this@SlackWebSocketSessionImpl)
            }
        }
    }

    private var websocketSession: Session? = null
    private var authToken: String? = null
    private val proxyAddress: String?
    private val proxyPort = -1
    var proxyHost: HttpHost? = null
    private var lastPingSent: Long = 0
    Volatile private var lastPingAck: Long = 0

    private var messageId: Long = 0

    private val lastConnectionTime = -1

    private var reconnectOnDisconnection: Boolean = false
    private var wantDisconnect = false

    private val pendingMessageMap = ConcurrentHashMap<Long, SlackMessageHandleImpl>()

    private var connectionMonitoringThread: Thread? = null
    private val dispatcher = EventDispatcher()

    constructor(authToken: String, proxyType: Proxy.Type, proxyAddress: String, proxyPort: Int, reconnectOnDisconnection: Boolean) {
        this.authToken = authToken
        this.proxyAddress = proxyAddress
        this.proxyPort = proxyPort
        this.proxyHost = HttpHost(proxyAddress, proxyPort)
        this.reconnectOnDisconnection = reconnectOnDisconnection
    }

    constructor(authToken: String, reconnectOnDisconnection: Boolean) {
        this.authToken = authToken
        this.reconnectOnDisconnection = reconnectOnDisconnection
    }

    Throws(IOException::class)
    override fun connect() {
        wantDisconnect = false
        connectImpl()
        print("starting connection monitoring")
        startConnectionMonitoring()
    }

    override fun disconnect() {
        wantDisconnect = true
        print("Disconnecting from the Slack server")
        disconnectImpl()
        stopConnectionMonitoring()
    }

    private fun connectImpl() {
        print("connecting to slack")
        lastPingSent = 0
        lastPingAck = 0
        val httpClient = getHttpClient()
        val request = HttpGet(SLACK_HTTPS_AUTH_URL + authToken!!)
        val response: HttpResponse
        response = httpClient.execute(request)
        print(response.getStatusLine().toString())
        val jsonResponse = CharStreams.toString(InputStreamReader(response.getEntity().getContent()))
        val sessionParser = SlackJSONSessionStatusParser(jsonResponse)
        try {
            sessionParser.parse()
        } catch (e1: ParseException) {
            LOGGER.error(e1.toString())
        }

        if (sessionParser.error != null) {
            LOGGER.error("Error during authentication : " + sessionParser.error!!)
            throw ConnectException(sessionParser.error)
        }
        users = sessionParser.getUsers()
        channels = sessionParser.getChannels()
        sessionPersona = sessionParser.sessionPersona
        LOGGER.info(getUsers().size() + " users found on this session")
        LOGGER.info(getChannels().size() + " channels found on this session")
        val wssurl = sessionParser.webSocketURL

        print("retrieved websocket URL : " + wssurl)
        val client = ClientManager.createClient()
        if (proxyAddress != null) {
            client.getProperties().put(ClientProperties.PROXY_URI, "http://$proxyAddress:$proxyPort")
        }
        val handler = this
        print("initiating connection to websocket")
        try {
            websocketSession = client.connectToServer(object : Endpoint() {
                public fun onOpen(session: Session, config: EndpointConfig) {
                    session.addMessageHandler(handler)
                }

            }, URI.create(wssurl))
        } catch (e: DeploymentException) {
            LOGGER.error(e.toString())
        }

        if (websocketSession != null) {
            val slackConnectedImpl = SlackConnectedImpl(sessionPersona)
            dispatcher.dispatch(slackConnectedImpl)
            print("websocket connection established")
            LOGGER.info("slack session ready")
        }
    }

    private fun disconnectImpl() {
        if (websocketSession != null) {
            try {
                websocketSession!!.close()
            } catch (ex: IOException) {
                // ignored.
            } finally {
                websocketSession = null
            }
        }
    }

    private fun startConnectionMonitoring() {
        connectionMonitoringThread = object : Thread() {
            override fun run() {
                print("monitoring thread started")
                while (true) {
                    try {
                        // heart beat of 30s (should be configurable in the future)
                        Thread.sleep(30000)

                        // disconnect() was called.
                        if (wantDisconnect)
                            this.interrupt()

                        if (lastPingSent != lastPingAck || websocketSession == null) {
                            // disconnection happened
                            LOGGER.warn("Connection lost...")
                            try {
                                if (websocketSession != null) {
                                    websocketSession!!.close()
                                }
                            } catch (e: IOException) {
                                LOGGER.error("exception while trying to close the websocket ", e)
                            }

                            websocketSession = null
                            if (reconnectOnDisconnection) {
                                connectImpl()
                                continue
                            } else {
                                this.interrupt()
                            }
                        } else {
                            lastPingSent = getNextMessageId()
                            print("sending ping " + lastPingSent)
                            try {
                                if (websocketSession!!.isOpen()) {
                                    websocketSession!!.getBasicRemote().sendText("{\"type\":\"ping\",\"id\":$lastPingSent}")
                                } else if (reconnectOnDisconnection) {
                                    connectImpl()
                                }
                            } catch (e: IllegalStateException) {
                                // websocketSession might be closed in this case
                                if (reconnectOnDisconnection) {
                                    connectImpl()
                                }
                            }

                        }
                    } catch (e: InterruptedException) {
                        break
                    } catch (e: IOException) {
                        LOGGER.error("unexpected exception on monitoring thread ", e)
                    }

                }
                print("monitoring thread stopped")
            }
        }

        if (!wantDisconnect)
            connectionMonitoringThread!!.start()
    }

    private fun stopConnectionMonitoring() {
        if (connectionMonitoringThread != null) {
            while (true) {
                try {
                    connectionMonitoringThread!!.interrupt()
                    connectionMonitoringThread!!.join()
                    break
                } catch (ex: InterruptedException) {
                    // ouch - let's try again!
                }

            }
        }
    }

    override fun sendMessage(channel: SlackChannel, message: String, attachment: SlackAttachment?, chatConfiguration: SlackChatConfiguration): SlackMessageHandle {
        val handle = SlackMessageHandleImpl(getNextMessageId())
        val arguments = HashMap<String, String>()
        arguments.put("token", authToken)
        arguments.put("channel", channel.id)
        arguments.put("text", message)
        if (chatConfiguration.asUser) {
            arguments.put("as_user", "true")
        }
        if (chatConfiguration.avatar === Avatar.ICON_URL) {
            arguments.put("icon_url", chatConfiguration.avatarDescription)
        }
        if (chatConfiguration.avatar === Avatar.EMOJI) {
            arguments.put("icon_emoji", chatConfiguration.avatarDescription)
        }
        if (chatConfiguration.userName != null) {
            arguments.put("username", chatConfiguration.userName)
        }
        if (attachment != null) {
            arguments.put("attachments", SlackJSONAttachmentFormatter.encodeAttachments(attachment).toString())
        }

        postSlackCommand(arguments, CHAT_POST_MESSAGE_COMMAND, handle)
        return handle
    }

    override fun deleteMessage(timeStamp: String, channel: SlackChannel): SlackMessageHandle {
        val handle = SlackMessageHandleImpl(getNextMessageId())
        val arguments = HashMap<String, String>()
        arguments.put("token", authToken)
        arguments.put("channel", channel.id)
        arguments.put("ts", timeStamp)
        postSlackCommand(arguments, CHAT_DELETE_COMMAND, handle)
        return handle
    }

    override fun updateMessage(timeStamp: String, channel: SlackChannel, message: String): SlackMessageHandle {
        val handle = SlackMessageHandleImpl(getNextMessageId())
        val arguments = HashMap<String, String>()
        arguments.put("token", authToken)
        arguments.put("ts", timeStamp)
        arguments.put("channel", channel.id)
        arguments.put("text", message)
        postSlackCommand(arguments, CHAT_UPDATE_COMMAND, handle)
        return handle
    }

    override fun addReactionToMessage(channel: SlackChannel, messageTimeStamp: String, emojiCode: String): SlackMessageHandle {
        val handle = SlackMessageHandleImpl(getNextMessageId())
        val arguments = HashMap<String, String>()
        arguments.put("token", authToken)
        arguments.put("channel", channel.id)
        arguments.put("timestamp", messageTimeStamp)
        arguments.put("name", emojiCode)
        postSlackCommand(arguments, REACTIONS_ADD_COMMAND, handle)
        return handle
    }

    override fun joinChannel(channelName: String): SlackMessageHandle {
        val handle = SlackMessageHandleImpl(getNextMessageId())
        val arguments = HashMap<String, String>()
        arguments.put("token", authToken)
        arguments.put("name", channelName)
        postSlackCommand(arguments, CHANNELS_JOIN_COMMAND, handle)
        return handle
    }

    override fun leaveChannel(channel: SlackChannel): SlackMessageHandle {
        val handle = SlackMessageHandleImpl(getNextMessageId())
        val arguments = HashMap<String, String>()
        arguments.put("token", authToken)
        arguments.put("channel", channel.id)
        postSlackCommand(arguments, CHANNELS_LEAVE_COMMAND, handle)
        return handle
    }

    private fun postSlackCommand(params: Map<String, String>, command: String, handle: SlackMessageHandleImpl) {
        val client = getHttpClient()
        val request = HttpPost(SLACK_API_HTTPS_ROOT + command)
        val nameValuePairList = ArrayList<NameValuePair>()
        for (arg in params.entrySet()) {
            nameValuePairList.add(BasicNameValuePair(arg.getKey(), arg.getValue()))
        }
        try {
            request.setEntity(UrlEncodedFormEntity(nameValuePairList, "UTF-8"))
            val response = client.execute(request)
            val jsonResponse = CharStreams.toString(InputStreamReader(response.getEntity().getContent()))
            print("PostMessage return: " + jsonResponse)
            val reply = SlackJSONReplyParser.decode(parseObject(jsonResponse))
            handle.slackReply = reply
        } catch (e: Exception) {
            // TODO : improve exception handling
            e.printStackTrace()
        }

    }

    private fun getHttpClient(): HttpClient {
        var client: HttpClient? = null
        if (proxyHost != null) {
            client = HttpClientBuilder.create().setRoutePlanner(DefaultProxyRoutePlanner(proxyHost)).build()
        } else {
            client = HttpClientBuilder.create().build()
        }
        return client
    }

    override fun sendMessageOverWebSocket(channel: SlackChannel, message: String, attachment: SlackAttachment?): SlackMessageHandle {
        val handle = SlackMessageHandleImpl(getNextMessageId())
        try {
            val messageJSON = JSONObject()
            messageJSON.put("type", "message")
            messageJSON.put("channel", channel.id)
            messageJSON.put("text", message)
            if (attachment != null) {
                messageJSON.put("attachments", SlackJSONAttachmentFormatter.encodeAttachments(attachment))
            }
            websocketSession!!.getBasicRemote().sendText(messageJSON.toJSONString())
        } catch (e: Exception) {
            // TODO : improve exception handling
            e.printStackTrace()
        }

        return handle
    }

    override fun getPresence(persona: SlackPersona): SlackPersona.SlackPresence {
        val client = getHttpClient()
        val request = HttpPost("https://slack.com/api/users.getPresence")
        val nameValuePairList = ArrayList<NameValuePair>()
        nameValuePairList.add(BasicNameValuePair("token", authToken))
        nameValuePairList.add(BasicNameValuePair("user", persona.id))
        try {
            request.setEntity(UrlEncodedFormEntity(nameValuePairList, "UTF-8"))
            val response = client.execute(request)
            val jsonResponse = CharStreams.toString(InputStreamReader(response.getEntity().getContent()))
            print("PostMessage return: " + jsonResponse)
            val resultObject = parseObject(jsonResponse)

            val reply = SlackJSONReplyParser.decode(resultObject)
            if (!reply.isOk()) {
                return SlackPersona.SlackPresence.UNKNOWN
            }

            if ("active" == resultObject.get("presence")) {
                return SlackPersona.SlackPresence.ACTIVE
            }
            if ("away" == resultObject.get("presence")) {
                return SlackPersona.SlackPresence.AWAY
            }
        } catch (e: Exception) {
            // TODO : improve exception handling
            e.printStackTrace()
        }

        return SlackPersona.SlackPresence.UNKNOWN
    }

    @Synchronized
    private fun getNextMessageId(): Long {
        return messageId++
    }

    public fun onMessage(message: String) {
        print("receiving from websocket " + message)
        if (message.contains("{\"type\":\"pong\",\"reply_to\"")) {
            val rightBracketIdx = message.indexOf('}')
            val toParse = message.substring(26, rightBracketIdx)
            lastPingAck = Integer.parseInt(toParse).toLong()
            print("pong received " + lastPingAck)
        } else {
            val `object` = parseObject(message)
            val slackEvent = SlackJSONMessageParser.decode(this, `object`)
            if (slackEvent is SlackChannelCreated) {
                getChannels().put(slackEvent.getSlackChannel().id, slackEvent.getSlackChannel())
            }
            if (slackEvent is SlackGroupJoined) {
                getChannels().put(slackEvent.getSlackChannel().id, slackEvent.getSlackChannel())
            }
            dispatcher.dispatch(slackEvent)
        }
    }

    private fun parseObject(json: String): JsonNode? {
        val parser = ObjectMapper()
        try {
            val obj = parser.readTree(json)
            return obj
        } catch (e: JsonParseException) {
            e.printStackTrace()
            return null
        }

    }

    override fun inviteUser(email: String, firstName: String, setActive: Boolean): SlackMessageHandle {

        val handle = SlackMessageHandleImpl(getNextMessageId())
        val arguments = HashMap<String, String>()
        authToken?.let { arguments.put("token", it) }
        arguments.put("email", email)
        arguments.put("first_name", firstName)
        arguments.put("set_active", "" + setActive)
        postSlackCommand(arguments, INVITE_USER_COMMAND, handle)
        return handle
    }

    companion object {
        private val SLACK_API_HTTPS_ROOT = "https://slack.com/api/"

        private val CHANNELS_LEAVE_COMMAND = "channels.leave"

        private val CHANNELS_JOIN_COMMAND = "channels.join"

        private val CHAT_POST_MESSAGE_COMMAND = "chat.postMessage"

        private val CHAT_DELETE_COMMAND = "chat.delete"

        private val CHAT_UPDATE_COMMAND = "chat.update"

        private val REACTIONS_ADD_COMMAND = "reactions.add"

        private val INVITE_USER_COMMAND = "users.admin.invite"


        private val SLACK_HTTPS_AUTH_URL = "https://slack.com/api/rtm.start?token="
    }

}
