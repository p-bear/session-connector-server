package com.pbear.sessionconnectorserver

import com.google.gson.Gson
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/session")
class SessionController(val sessionManager: SessionManager) {
    private val gson = Gson()

    @GetMapping
    fun getSessionList(): Any {
        val sessions = this.sessionManager.getSessions()
        return mapOf(
            "result" to "success",
            "data" to mapOf(
                "totalCount" to sessions.size,
                "sessions" to sessions))
    }

    @GetMapping("/{sessionId}")
    fun getSession(@PathVariable sessionId: String) =
        mapOf("data" to mapOf("session" to this.sessionManager.getSession(sessionId)))

    @DeleteMapping("/{sessionId}")
    fun deleteSession(@PathVariable sessionId: String) = mapOf(
        "result" to "success",
        "count" to this.sessionManager.removeSession(sessionId))

    @PutMapping("/{sessionId}/referenceTag/{referenceTag}")
    fun putSessionReferenceTag(@PathVariable sessionId: String, @PathVariable referenceTag: String) =
        this.sessionManager.addReferenceTag(sessionId, referenceTag)

    @DeleteMapping("/{sessionId}/referenceTag/{referenceTag}")
    fun deleteSessionReferenceTag(@PathVariable sessionId: String, @PathVariable referenceTag: String) =
        this.sessionManager.removeReferenceTag(sessionId, referenceTag)

    @PostMapping("/message")
    fun sendMessage(@RequestBody body: Map<String, *>) {
        this.sessionManager.sendMessage(
            (body["ref"] as List<String>).toSet(),
            this.gson.toJson(body["message"]))
    }
}