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
import net.logstash.logback.argument.StructuredArguments.v

trait MqttFlowIn {
  def process(topic: String, message: MqttMessage): Task[(RecordMetadata, MqttMessage)]
}

@Singleton
class DefaultMqttFlowIn @Inject() (config: Config, mqttSubscriber: MqttSubscriber, kafkaFlowIn: KafkaFlowIn)(implicit scheduler: Scheduler) extends MqttFlowIn with LazyLogging {

  private val topic = Paths.get(config.getString(MqttConf.IN_QUEUE_PREFIX), "+").toString
  private val qos = config.getInt(MqttConf.QOS)

  override def process(topic: String, message: MqttMessage): Task[(RecordMetadata, MqttMessage)] = {
    (for {
      _ <- Task.unit
      requestId = UUID.randomUUID()
      payload <- Task.delay(FlowInPayload.parseFrom(message.getPayload))
      uuid <- Task.delay(UUID.fromString(payload.hardwareId))
      _ = logger.info("mqtt_fi_message_uuid=" + uuid.toString, v("requestId", requestId.toString))
      rm <- kafkaFlowIn.publish(requestId, uuid, payload.password, payload.upp.toByteArray)
      _ = logger.info("mqtt_fi_published=" + uuid.toString, v("requestId", requestId.toString))
    } yield {
      (rm, message)
    }).doOnFinish {
      case Some(e) => Task.delay(logger.error("mqtt_fi_message_error -> ", e))
      case _ => Task.unit
    }

  }

  mqttSubscriber.subscribe(topic, qos)((t, mqm) => process(t, mqm))

}
