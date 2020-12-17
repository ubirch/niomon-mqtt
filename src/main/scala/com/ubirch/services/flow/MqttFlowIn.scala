package com.ubirch.services.flow

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import com.ubirch.ConfPaths.MqttConf
import com.ubirch.models.FlowInPayload
import monix.eval.Task
import monix.execution.Scheduler
import org.apache.kafka.clients.producer.RecordMetadata
import org.eclipse.paho.client.mqttv3.MqttMessage

import java.nio.file.Paths
import java.util.UUID
import javax.inject.{ Inject, Singleton }

trait MqttFlowIn {
  def process(topic: String, message: MqttMessage): Task[(RecordMetadata, MqttMessage)]
}

@Singleton
class DefaultMqttFlowIn @Inject() (config: Config, mqttSubscriber: MqttSubscriber, kafkaFlowIn: KafkaFlowIn)(implicit scheduler: Scheduler) extends MqttFlowIn with LazyLogging {

  private val topic = Paths.get(config.getString(MqttConf.IN_QUEUE_PREFIX), "+").toString
  private val qos = config.getInt(MqttConf.QOS)

  override def process(topic: String, message: MqttMessage): Task[(RecordMetadata, MqttMessage)] = {
    val payload = FlowInPayload.parseFrom(message.getPayload)
    val uuid = UUID.fromString(payload.hardwareId)
    kafkaFlowIn
      .publish(uuid, payload.password, payload.upp.toByteArray)
      .map { rm => (rm, message) }
  }

  mqttSubscriber.subscribe(topic, qos)((t, mqm) => process(t, mqm))

}
