package com.pbear.sessionconnectorserver

import com.google.gson.Gson
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*
import javax.annotation.PostConstruct
import kotlin.collections.HashMap

@Component
class KafkaConsumeManager(val sessionManager: SessionManager) {
    private lateinit var receiverOptions: ReceiverOptions<String, String>
    private lateinit var connectorMessageReceiver: KafkaReceiver<String, String>
    private val gson = Gson()

    @Value("\${kafka.bootstrap.server.address}")
    val bootstrapServer: String = ""

    @PostConstruct
    fun init() {
        val consumerConfig = HashMap<String, Any>();
        consumerConfig[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServer
        consumerConfig[ConsumerConfig.GROUP_ID_CONFIG] = UUID.randomUUID().toString()
        consumerConfig[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        consumerConfig[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        consumerConfig[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] =
            OffsetResetStrategy.LATEST.name.lowercase(Locale.getDefault())
        consumerConfig[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = 10

        this.receiverOptions = ReceiverOptions.create(consumerConfig)
        this.connectorMessageReceiver = KafkaReceiver
            .create(
                this.receiverOptions
                    .commitInterval(Duration.of(10, ChronoUnit.MILLIS))
                    .commitBatchSize(1)
                    .subscription(listOf("connector.message"))
            )

        this.consumeConnectorMessage()
    }

    fun consumeConnectorMessage() {
        this.connectorMessageReceiver
            .receiveAutoAck()
            .concatMap { it }
            .onErrorContinue { t, _ -> t.printStackTrace() }
            .subscribe { record ->
                try {
                    val commonConnectorMessage = this.gson.fromJson(record.value(), CommonConnectorMessage::class.java)
                    this.sessionManager.sendMessage(
                        commonConnectorMessage.referenceTags.toSet(),
                        this.gson.toJson(commonConnectorMessage.message)
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
    }
}