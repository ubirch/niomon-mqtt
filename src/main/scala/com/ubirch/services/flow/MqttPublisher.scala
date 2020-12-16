package com.ubirch.services.flow

import com.typesafe.scalalogging.LazyLogging
import org.eclipse.paho.client.mqttv3.{ IMqttDeliveryToken, MqttMessage }

import java.util.UUID
import javax.inject.{ Inject, Singleton }

trait MqttPublisher {
  def toMqttMessage(qos: Int, retained: Boolean, payload: Array[Byte]): MqttMessage
  def publish(topic: String, deviceId: UUID, message: MqttMessage): IMqttDeliveryToken
}

@Singleton
class DefaultMqttPublisher @Inject() (mqttClients: MqttClients) extends MqttPublisher with LazyLogging {

  override def toMqttMessage(qos: Int, retained: Boolean, payload: Array[Byte]): MqttMessage = {
    val message = new MqttMessage(payload)
    message.setQos(qos)
    message.setRetained(retained)
    message
  }

  override def publish(topic: String, deviceId: UUID, message: MqttMessage): IMqttDeliveryToken = {
    mqttClients.async.publish(
      topic,
      message,
      null,
      MqttClients.listener(_ => logger.info(s"published_to=$topic"), (_, e) => logger.error(s"error_publishing_to=$topic", e))
    )
  }

}
