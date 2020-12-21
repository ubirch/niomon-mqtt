package com.ubirch.services.flow

import java.util.UUID

import com.typesafe.scalalogging.LazyLogging
import javax.inject.{ Inject, Singleton }
import net.logstash.logback.argument.StructuredArguments.v
import org.eclipse.paho.client.mqttv3.{ IMqttDeliveryToken, MqttMessage }

trait MqttPublisher {
  def toMqttMessage(qos: Int, retained: Boolean, payload: Array[Byte]): MqttMessage
  def publish(topic: String, requestId: UUID, deviceId: UUID, message: MqttMessage): IMqttDeliveryToken
}

@Singleton
class DefaultMqttPublisher @Inject() (mqttClients: MqttClients) extends MqttPublisher with LazyLogging {

  override def toMqttMessage(qos: Int, retained: Boolean, payload: Array[Byte]): MqttMessage = {
    val message = new MqttMessage(payload)
    message.setQos(qos)
    message.setRetained(retained)
    message
  }

  override def publish(topic: String, requestId: UUID, deviceId: UUID, message: MqttMessage): IMqttDeliveryToken = {
    mqttClients.async.publish(
      topic,
      message,
      null,
      mqttClients.listener(
        _ => logger.info(s"mqtt_fo_published=$topic", v("requestId", requestId.toString)),
        (_, e) => logger.error(s"mqtt_fo_publish_error=$topic", e)
      )
    )
  }

}
