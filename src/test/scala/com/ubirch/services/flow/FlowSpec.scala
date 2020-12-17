package com.ubirch.services.flow

import com.google.protobuf.ByteString
import com.typesafe.config.Config
import com.ubirch.ConfPaths.MqttConf
import com.ubirch.kafka.util.Implicits.enrichedConsumerRecord
import com.ubirch.kafka.util.PortGiver
import com.ubirch.models.FlowInPayload
import com.ubirch.{ EmbeddedMqtt, ExecutionContextsTests, InjectorHelperImpl, TestBase }
import net.manub.embeddedkafka.Codecs.{ nullDeserializer, stringDeserializer }
import net.manub.embeddedkafka.{ EmbeddedKafka, EmbeddedKafkaConfig, ExtendedEmbeddedKafkaHelpers }
import org.scalatest.Tag

import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util
import java.util.UUID

class FlowSpec extends TestBase with ExecutionContextsTests with EmbeddedMqtt with EmbeddedKafka with ExtendedEmbeddedKafkaHelpers[EmbeddedKafkaConfig] {

  val mqttBroker = new MqttTest

  "Flow -Niomon MQTT-" must {

    "do loop OK" taggedAs Tag("plum") in {

      implicit val kafkaConfig: EmbeddedKafkaConfig = EmbeddedKafkaConfig(kafkaPort = PortGiver.giveMeKafkaPort, zooKeeperPort = PortGiver.giveMeZookeeperPort)

      val Injector = new InjectorHelperImpl("localhost:" + kafkaConfig.kafkaPort) {}
      val config: Config = Injector.get[Config]

      withRunningKafka {

        val kafkaFlowOut: KafkaFlowOut = Injector.get[KafkaFlowOut]
        kafkaFlowOut.start()

        val mqttClients: MqttClients = Injector.get[MqttClients]
        Injector.get[MqttFlowIn]

        val mqttPublisher: MqttPublisher = Injector.get[MqttPublisher]
        val uuid = UUID.randomUUID()

        def topic(deviceId: UUID): String = Paths.get(config.getString(MqttConf.IN_QUEUE_PREFIX), deviceId.toString).toString
        val inPayload = FlowInPayload(uuid.toString, "password", ByteString.copyFrom("hola", StandardCharsets.UTF_8))
        mqttPublisher.publish(topic(uuid), uuid,
          mqttPublisher.toMqttMessage(
            1,
            retained = false,
            inPayload.toByteArray
          ))

        Thread.sleep(5000)

        val inPayloadFromKafka = consume[String, Array[Byte]](Set("ubirch-niomon-req-bin"), 1).getOrElse("ubirch-niomon-req-bin", Nil).head

        assert(util.Arrays.equals(inPayloadFromKafka.value(), inPayload.upp.toByteArray))
        assert(inPayloadFromKafka.findHeader("request-id").isDefined)
        assert(inPayloadFromKafka.findHeader("X-Ubirch-Gateway-Type").contains("mqtt"))
        assert(inPayloadFromKafka.findHeader("X-Ubirch-Hardware-Id").contains(uuid.toString))
        assert(inPayloadFromKafka.findHeader("X-Ubirch-Auth-Type").contains("ubirch"))
        assert(inPayloadFromKafka.findHeader("X-Ubirch-Credential").contains("password"))
        assert(mqttClients.async.isConnected)

      }

    }

  }

  override protected def beforeAll(): Unit = {
    mqttBroker.start()
  }

  override protected def afterAll(): Unit = {
    mqttBroker.stop()
  }

}
