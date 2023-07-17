package com.pbear.sessionconnectorserver

import com.fasterxml.jackson.annotation.JsonIgnore
import com.google.gson.Gson
import mu.KotlinLogging
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.ConcurrentHashMap

@Component
class SessionManager {
    private val log = KotlinLogging.logger {  }
    private val sessionMap = ConcurrentHashMap<String, SessionWrapper>()

    fun sendMessage(referenceTags: Set<String>, message: String) {
        this.getSessionsByReferenceTags(referenceTags)
            .forEach { it.webSocketSession.sendMessage(TextMessage(message)) }
    }


    fun getSessions(): Map<String, SessionWrapper> {
        return this.sessionMap
    }

    fun getSession(sessionId: String): SessionWrapper? = this.sessionMap[sessionId]

    fun getSessionsByReferenceTags(target: Set<String>): Set<SessionWrapper> {
        return this.sessionMap.entries
            .map { it.value }
            .filter { target.intersect(it.referenceTagSet).isNotEmpty() }
            .toSet()
    }

    fun addSession(sessionWrapper: SessionWrapper) {
        log.info("add Session, id: ${sessionWrapper.id}, referenceTagSet: ${sessionWrapper.referenceTagSet}")
        this.sessionMap[sessionWrapper.id] = sessionWrapper
    }

    fun removeSession(id: String): Int {
        if (this.sessionMap.containsKey(id)) {
            log.info("remove Session, id: $id")
            this.sessionMap[id]!!.webSocketSession.close()
            this.sessionMap.remove(id)
            return 1
        }
        return 0
    }

    fun addReferenceTag(sessionId: String, referenceTag: String) =
        this.sessionMap[sessionId]?.referenceTagSet?.add(referenceTag)

    fun removeReferenceTag(sessionId: String, referenceTag: String) =
        this.sessionMap[sessionId]?.referenceTagSet?.remove(referenceTag)
}

class SessionWrapper(
    val id: String,
    var referenceTagSet: MutableSet<String> = HashSet(),
    @JsonIgnore
    val webSocketSession: WebSocketSession)

@Component
class StringWebsocketHandler(val sessionManager: SessionManager): TextWebSocketHandler() {
    private val gson = Gson()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val sessionWrapper = SessionWrapper(id = session.id, webSocketSession = session)
        session.uri?.rawQuery?.let { addReferenceTags(it, sessionWrapper) }
        this.sessionManager.addSession(sessionWrapper)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        this.sessionManager.removeSession(session.id)
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val payload = this.gson.fromJson(message.payload, CommonPayload::class.java)
        when(payload.type) {
            "addReference" -> this.sessionManager.addReferenceTag(session.id, payload.data["referenceTag"] as String)
            "removeReference" -> this.sessionManager.removeReferenceTag(session.id, payload.data["referenceTag"] as String)
        }

    }


    private fun addReferenceTags(rawQuery: String, target: SessionWrapper) {
        rawQuery
            .split('&')
            .map {
                val param = it.split('=')
                Pair(param.firstOrNull() ?: "", param.drop(1).firstOrNull() ?: "")
            }
            .filter { it.first == "referenceTag" }
            .forEach { target.referenceTagSet.add(it.second) }
    }
}

data class CommonPayload(
    val type: String,
    val data: Map<String, *>)


@Configuration
@EnableWebSocket
class WebsocketConfig(val stringWebsocketHandler: StringWebsocketHandler): WebSocketConfigurer {
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry
            .addHandler(stringWebsocketHandler, "/connect").setAllowedOrigins("*")
    }
}