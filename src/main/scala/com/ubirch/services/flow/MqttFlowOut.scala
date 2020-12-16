package com.ubirch.services.flow

import java.nio.file.Paths
import java.util.UUID

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import com.ubirch.ConfPaths.MqttConf
import com.ubirch.models.FlowOutPayload
import javax.inject.{ Inject, Singleton }
import org.eclipse.paho.client.mqttv3.{ IMqttDeliveryToken, MqttMessage }

trait MqttFlowOut {
  def process(deviceId: UUID, requestId: UUID, flowOutPayload: FlowOutPayload): IMqttDeliveryToken
}

@Singleton
class DefaultMqttFlowOut @Inject() (config: Config, mqttClients: MqttClients) extends MqttFlowOut with LazyLogging {

  private def topic(deviceId: UUID): String = Paths.get(config.getString(MqttConf.OUT_QUEUE_PREFIX), deviceId.toString).toString
  private val qos = config.getInt(MqttConf.QOS)

  override def process(deviceId: UUID, requestId: UUID, flowOutPayload: FlowOutPayload): IMqttDeliveryToken = {

    val deviceTopic = topic(deviceId)

    val message = new MqttMessage(flowOutPayload.toByteArray)
    message.setQos(qos)
    message.setRetained(false)

    mqttClients.async.publish(
      topic(deviceId),
      message,
      null,
      MqttClients.listener(_ => logger.info(s"published_to=$deviceTopic"), (_, e) => logger.error(s"error_publishing_to=$deviceTopic", e))
    )

  }

}
