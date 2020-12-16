package com.ubirch

import com.typesafe.scalalogging.LazyLogging
import com.ubirch.services.flow.{ KafkaFlowOut, MqttFlowIn, MqttFlowOut }
import com.ubirch.services.rest.RestService

import java.util.concurrent.CountDownLatch
import javax.inject.{ Inject, Singleton }
import scala.util.Try

/**
  * Represents a bootable service object that starts the system
  */
@Singleton
class Service @Inject() (restService: RestService, mqttFlowIn: MqttFlowIn, mqttFlowOut: MqttFlowOut, kafkaFlowOut: KafkaFlowOut) extends LazyLogging {

  def start(): Unit = {

    for {
      _ <- Try(mqttFlowIn)
      _ <- Try(mqttFlowOut)
      _ <- Try(kafkaFlowOut.start())
      _ <- Try(restService.start())
    } yield true

    val cd = new CountDownLatch(1)
    cd.await()
  }

}

object Service extends Boot(List(new Binder)) {
  def main(args: Array[String]): Unit = * {
    get[Service].start()
  }
}
