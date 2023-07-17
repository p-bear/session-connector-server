package com.pbear.sessionconnectorserver

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.web.socket.WebSocketSession
import java.util.*
import kotlin.collections.HashSet

class SessionWrapper(
    val id: String,
    var referenceTagSet: MutableSet<String> = HashSet(),
    val timestamp: Long = Date().time,
    @JsonIgnore
    val webSocketSession: WebSocketSession
)

data class CommonWebsocketPayload(
    val type: String,
    val data: Map<String, *>)

data class CommonConnectorMessage(
    val referenceTags: List<String>,
    val message: Any
)