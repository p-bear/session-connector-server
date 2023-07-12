package com.pbear.sessionconnectorserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SessionConnectorServerApplication

fun main(args: Array<String>) {
    runApplication<SessionConnectorServerApplication>(*args)
}
