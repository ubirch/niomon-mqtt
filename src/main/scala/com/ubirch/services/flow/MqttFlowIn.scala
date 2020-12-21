package com.ubirch.services.flow

import java.nio.file.Paths
import java.util.UUID

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import com.ubirch.ConfPaths.MqttConf
import com.ubirch.models.FlowInPayload
import com.ubirch.util.DateUtil
import io.prometheus.client.Counter
import javax.inject.{ Inject, Singleton }
import monix.eval.Task
import monix.execution.Scheduler
import net.logstash.logback.argument.StructuredArguments.v
import org.apache.kafka.clients.producer.RecordMetadata
import org.eclipse.paho.client.mqttv3.MqttMessage

import scala.concurrent.duration._
import scala.language.postfixOps

trait MqttFlowIn {
  def process(topic: String, message: MqttMessage): Task[(RecordMetadata, MqttMessage)]
}

@Singleton
class DefaultMqttFlowIn @Inject() (config: Config, mqttSubscriber: MqttSubscriber, kafkaFlowIn: KafkaFlowIn)(implicit scheduler: Scheduler) extends MqttFlowIn with LazyLogging {

  private val topic = Paths.get(config.getString(MqttConf.IN_QUEUE_PREFIX), "+").toString
  private val qos = config.getInt(MqttConf.QOS)

  private val flowInCounter: Counter = Counter.build()
    .name("mqtt_fi")
    .help("Represents the number of incoming mqtt flow-ins")
    .labelNames("service")
    .register()

  override def process(topic: String, message: MqttMessage): Task[(RecordMetadata, MqttMessage)] = {
    flowInCounter.labels("mqtt").inc()

    (for {
      _ <- Task.unit
      requestId = UUID.randomUUID()
      payload <- Task.delay(FlowInPayload.parseFrom(message.getPayload))
      uuid <- Task.delay(UUID.fromString(payload.hardwareId))
      _ = logger.info("mqtt_fi_message_uuid=" + uuid.toString, v("requestId", requestId.toString))
      rm <- kafkaFlowIn.publish(requestId, uuid, payload.password, payload.upp.toByteArray, 10 seconds, DateUtil.nowUTC)
      _ = logger.info("mqtt_fi_published=" + uuid.toString, v("requestId", requestId.toString))
    } yield {
      (rm._1, message)
    }).doOnFinish {
      case Some(e) => Task.delay(logger.error("mqtt_fi_message_error -> ", e))
      case _ => Task.unit
    }

  }

  mqttSubscriber.subscribe(topic, qos)((t, mqm) => process(t, mqm))

}
