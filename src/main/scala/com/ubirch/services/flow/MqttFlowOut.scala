package com.ubirch.services.flow

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import com.ubirch.ConfPaths.MqttConf
import com.ubirch.models.FlowOutPayload
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken

import java.nio.file.Paths
import java.util.UUID
import javax.inject.{ Inject, Singleton }

trait MqttFlowOut {
  def process(requestId: UUID, deviceId: UUID, flowOutPayload: FlowOutPayload): IMqttDeliveryToken
}

@Singleton
class DefaultMqttFlowOut @Inject() (config: Config, mqttPublisher: MqttPublisher) extends MqttFlowOut with LazyLogging {

  private def topic(deviceId: UUID): String = Paths.get(config.getString(MqttConf.OUT_QUEUE_PREFIX), deviceId.toString).toString
  private val qos = config.getInt(MqttConf.QOS)

  override def process(requestId: UUID, deviceId: UUID, flowOutPayload: FlowOutPayload): IMqttDeliveryToken = {
    val message = mqttPublisher.toMqttMessage(qos, retained = false, flowOutPayload.toByteArray)
    mqttPublisher.publish(topic(deviceId), requestId, deviceId, message)
  }

}
