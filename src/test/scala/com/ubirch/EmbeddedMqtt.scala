package com.ubirch

import java.util.Properties

import com.typesafe.scalalogging.LazyLogging
import io.moquette.broker.Server

trait EmbeddedMqtt {

  class MqttTest extends LazyLogging {

    @volatile var mqttBroker: Server = _

    def start(): Unit = {
      try {
        val props = new Properties()
        props.setProperty("port", "1883")
        props.setProperty("host", "localhost")
        props.setProperty("allow_anonymous", "true")
        mqttBroker = new Server()
        mqttBroker.startServer(props)
      } catch {
        case e: Exception =>
          logger.error("mqtt_start_error=" + e.getMessage, e)
          throw e
      }
    }

    def stop(): Unit = {
      try {
        if (mqttBroker != null) mqttBroker.stopServer()
      } catch {
        case e: Exception =>
          logger.error("mqtt_stop_error=" + e.getMessage, e)
          throw e
      }
    }

  }

}
