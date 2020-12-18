package com.ubirch.services.flow

import com.typesafe.scalalogging.LazyLogging
import org.eclipse.paho.client.mqttv3.MqttMessage

import javax.inject.{ Inject, Singleton }
import monix.eval.Task
import monix.execution.Scheduler
import org.apache.kafka.clients.producer.RecordMetadata

trait MqttSubscriber {
  def subscribe(topic: String, qos: Int)(process: (String, MqttMessage) => Task[(RecordMetadata, MqttMessage)]): Unit
}

@Singleton
class DefaultMqttSubscriber @Inject() (mqttClients: MqttClients)(implicit scheduler: Scheduler) extends MqttSubscriber with LazyLogging {

  override def subscribe(topic: String, qos: Int)(process: (String, MqttMessage) => Task[(RecordMetadata, MqttMessage)]): Unit = {
    if (mqttClients.async.isConnected) {
      mqttClients.async.subscribe(
        topic,
        qos,
        null,
        mqttClients.listener(_ => logger.info(s"subscribed_to=$topic"), (_, e) => logger.error(s"error_subscribing_to=$topic", e)),
        (topic: String, message: MqttMessage) => process(topic, message).runToFuture
      )
    } else {
      logger.warn("mqtt connection is not ready")
    }
  }

}
