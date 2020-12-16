package com.ubirch

import com.typesafe.scalalogging.LazyLogging
import io.moquette.broker.Server

import java.util.Properties

trait EmbeddedMqtt {

  class MqttTest extends LazyLogging {

    @volatile var mqttBroker: Server = _

    def start(): Unit = {
      val props = new Properties()
      props.setProperty("port", "1883")
      props.setProperty("host", "localhost")
      props.setProperty("allow_anonymous", "true")
      mqttBroker = new Server()
      mqttBroker.startServer(props)
    }

    def stop(): Unit = {
      if (mqttBroker != null) mqttBroker.stopServer()
    }

  }

}
