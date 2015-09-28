package com.ullink.slack.simpleslackapi.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.ullink.slack.simpleslackapi.SlackAttachment
import com.ullink.slack.simpleslackapi.SlackField
import java.io.UnsupportedEncodingException
import java.util.ArrayList


object SlackJSONAttachmentFormatter {
    private val _mapper = ObjectMapper()

    public fun encodeAttachments(vararg attachments: SlackAttachment): List<ObjectNode> {
        val toReturn = ArrayList<ObjectNode>()
        for (attachment in attachments) {
            val attachmentJSON = _mapper.createObjectNode()
            toReturn.add(attachmentJSON)
            if (attachment.title != null) {
                attachmentJSON.put("title", attachment.title)
            }
            if (attachment.thumb_url != null) {
                attachmentJSON.put("thumb_url", attachment.thumb_url)
            }
            if (attachment.titleLink != null) {
                attachmentJSON.put("title_link", attachment.titleLink)
            }
            if (attachment.text != null) {
                attachmentJSON.put("text", attachment.text)
            }
            if (attachment.color != null) {
                attachmentJSON.put("color", attachment.color)
            }
            if (attachment.pretext != null) {
                attachmentJSON.put("pretext", attachment.pretext)
            }
            if (attachment.fallback != null) {
                attachmentJSON.put("fallback", attachment.fallback)
            }
            if (attachment.miscRootFields != null) {
                for (entry in attachment.miscRootFields.entrySet()) {
                    attachmentJSON.put(entry.getKey(), entry.getValue())
                }
            }
            if (attachment.markdown_in != null && !attachment.markdown_in.isEmpty()) {
                val array = attachmentJSON.putArray("mrkdwn_in")
                for(el in attachment.markdown_in) {
                    array.add(el)
                }
            }
            if (attachment.fields != null && !attachment.fields.isEmpty()) {
                attachmentJSON.put("fields", encodeAttachmentFields(attachment.fields))
            }

        }
        return toReturn
    }

    private fun encodeAttachmentFields(fields: List<SlackField>): ArrayNode? {
        val toReturn = _mapper.createArrayNode()
        for (field in fields) {
            val fieldJSON = _mapper.createObjectNode()
            if (field.title != null) {
                fieldJSON.put("title", field.title)
            }
            if (field.value != null) {
                fieldJSON.put("value", field.value)
            }
            fieldJSON.put("short", field.isShort)
            toReturn.add(fieldJSON)
        }
        return toReturn
    }

    @Throws(UnsupportedEncodingException::class)
    @JvmStatic public fun main(args: Array<String>) {
        println(String("Loïc Herve".getBytes(), "UTF-8"))
    }
}
