package com.ubirch.services.flow

import java.util.concurrent.{ CountDownLatch, TimeUnit }

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import com.ubirch.ConfPaths.MqttConf
import com.ubirch.services.lifeCycle.Lifecycle
import javax.inject._
import org.eclipse.paho.client.mqttv3._
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

import scala.concurrent.Future

trait MqttClients {
  def async: IMqttAsyncClient
  def listener(success: IMqttToken => Unit, failure: (IMqttToken, Throwable) => Unit): IMqttActionListener = MqttClients.listener(success, failure)
}

object MqttClients {
  def listener(success: IMqttToken => Unit, failure: (IMqttToken, Throwable) => Unit): IMqttActionListener = new IMqttActionListener {
    override def onSuccess(asyncActionToken: IMqttToken): Unit = {
      success(asyncActionToken)
    }
    override def onFailure(asyncActionToken: IMqttToken, exception: Throwable): Unit = {
      failure(asyncActionToken, exception)
    }
  }
}

@Singleton
class DefaultMqttClients @Inject() (config: Config, lifecycle: Lifecycle) extends MqttClients with LazyLogging {

  private val broker = config.getString(MqttConf.BROKER_URL)
  private val clientId = config.getString(MqttConf.CLIENT_ID)
  private val userName = config.getString(MqttConf.USER_NAME)
  private val password = config.getString(MqttConf.PASSWORD)
  private val maxInFlight = config.getInt(MqttConf.MAX_IN_FLIGHT)
  private val client: IMqttAsyncClient = {
    val p = new CountDownLatch(1)
    val c = try {
      val persistence = new MemoryPersistence()
      val client: IMqttAsyncClient = new MqttAsyncClient(broker, clientId, persistence)
      val connOpts = new MqttConnectOptions()
      connOpts.setUserName(userName)
      connOpts.setPassword(password.toCharArray)
      connOpts.setMaxInflight(maxInFlight)
      connOpts.setCleanSession(true)
      client.connect(connOpts, null, listener(_ => {
        logger.info(s"mqtt_connected=OK @ $broker with max_in_flight=$maxInFlight")
        p.countDown()
      }, (_, e) => {
        logger.error(s"error_connecting to $broker with max_in_flight=$maxInFlight", e)
        p.countDown()
      }))
      client
    } catch {
      case me: MqttException =>
        p.countDown()
        logger.error("retrieving MQTT client failed: ", me)
        throw me
    }

    p.await(15, TimeUnit.SECONDS)
    c

  }

  def async: IMqttAsyncClient = {
    client
  }

  lifecycle.addStopHook { () =>
    logger.info("Shutting mqtt connection...")
    Future.successful { client.disconnect(); client.close() }
  }

}

