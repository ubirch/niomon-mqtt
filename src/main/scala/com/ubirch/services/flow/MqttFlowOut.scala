package com.ubirch.services.flow

import java.nio.file.Paths
import java.util.UUID

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import com.ubirch.ConfPaths.MqttConf
import com.ubirch.models.FlowOutPayload
import com.ubirch.util.DateUtil
import net.logstash.logback.argument.StructuredArguments.v
import io.prometheus.client.Counter
import javax.inject.{ Inject, Singleton }
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.joda.time.DateTime

import scala.util.Try

trait MqttFlowOut {
  def process(requestId: UUID, deviceId: UUID, flowOutPayload: FlowOutPayload, entryTime: Try[DateTime]): IMqttDeliveryToken
}

@Singleton
class DefaultMqttFlowOut @Inject() (config: Config, mqttPublisher: MqttPublisher) extends MqttFlowOut with LazyLogging {

  private def topic(deviceId: UUID): String = Paths.get(config.getString(MqttConf.OUT_QUEUE_PREFIX), deviceId.toString).toString
  private val qos = config.getInt(MqttConf.QOS)

  private val flowOutCounter: Counter = Counter.build()
    .name("mqtt_fo")
    .help("Represents the number of incoming mqtt flow-outs")
    .labelNames("service")
    .register()

  override def process(requestId: UUID, deviceId: UUID, flowOutPayload: FlowOutPayload, entryTime: Try[DateTime]): IMqttDeliveryToken = {
    flowOutCounter.labels("mqtt").inc()
    val duration = entryTime.map(DateUtil.duration)
    val destination = topic(deviceId)
    val message = mqttPublisher.toMqttMessage(qos, retained = false, flowOutPayload.toByteArray)
    mqttPublisher.publish(destination, requestId, deviceId, message, MqttClients.listener(
      _ => logger.info(s"mqtt_fo_published=$destination duration=${duration.map(_.toString).toOption.getOrElse("N/A")}", v("requestId", requestId.toString)),
      (_, e) => logger.error(s"mqtt_fo_publish_error=$destination", e)
    ))
  }

}
