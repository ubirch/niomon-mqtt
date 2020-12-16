package com.ubirch.services.flow

import java.nio.file.Paths
import java.util.UUID

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import com.ubirch.ConfPaths.MqttConf
import com.ubirch.models.FlowInPayload
import javax.inject.{ Inject, Singleton }
import org.eclipse.paho.client.mqttv3.MqttMessage

trait MqttFlowIn {
  def process(topic: String, message: MqttMessage): Unit
}

@Singleton
class DefaultMqttFlowIn @Inject() (config: Config, mqttClients: MqttClients, kafkaFlowIn: KafkaFlowIn) extends MqttFlowIn with LazyLogging {

  private val topic = Paths.get(config.getString(MqttConf.QUEUE_PREFIX), "+").toString
  private val qos = config.getInt(MqttConf.QOS)

  if (mqttClients.async.isConnected) {
    mqttClients.async.subscribe(
      topic,
      qos,
      null,
      MqttClients.listener(_ => logger.info(s"subscribed_to=$topic"), (_, e) => logger.error(s"error_subscribing_to=$topic", e)),
      (topic: String, message: MqttMessage) => process(topic, message)
    )
  } else {
    logger.warn("mqtt connection is not ready")
  }

  override def process(topic: String, message: MqttMessage): Unit = {
    val payload = FlowInPayload.parseFrom(message.getPayload)
    val uuid = UUID.fromString(payload.hardwareId)
    kafkaFlowIn.publish(uuid, payload.password, payload.upp.toByteArray)
  }

}
