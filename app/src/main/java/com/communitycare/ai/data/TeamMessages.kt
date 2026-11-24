package com.communitycare.ai.data

import java.time.Instant
import java.util.UUID

data class MessageForm(val text: String)
data class TeamMessage(val id: String, val senderId: String, val senderName: String, val text: String, val sentAt: String) {
    fun validate(): TeamMessage {
        UUID.fromString(id)
        Instant.parse(sentAt)
        require(senderId.isNotBlank() && senderName.isNotBlank() && text.isNotBlank() && text.length <= 2000)
        return this
    }
}
data class TeamInbox(val orgUnit: String, val messages: List<TeamMessage>) {
    fun validate(profile: Profile): TeamInbox {
        require(orgUnit == profile.orgUnit && messages.size <= 100 && messages.map { it.id }.distinct().size == messages.size)
        messages.forEach { it.validate() }
        return this
    }
}
