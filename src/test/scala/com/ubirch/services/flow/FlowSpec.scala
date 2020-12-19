package com.ubirch
package services.flow

import com.google.protobuf.ByteString
import com.typesafe.config.Config
import com.ubirch.ConfPaths.MqttConf
import com.ubirch.kafka.util.Implicits.enrichedConsumerRecord
import com.ubirch.kafka.util.PortGiver
import com.ubirch.models.{ FlowInPayload, FlowOutPayload }
import monix.eval.Task
import net.manub.embeddedkafka.Codecs.{ nullDeserializer, nullSerializer, stringDeserializer }
import net.manub.embeddedkafka.{ EmbeddedKafka, EmbeddedKafkaConfig, ExtendedEmbeddedKafkaHelpers }
import org.apache.kafka.clients.producer.{ ProducerRecord, RecordMetadata }
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.header.Header
import org.apache.kafka.common.header.internals.{ RecordHeader, RecordHeaders }
import org.scalatest.Tag
import java.lang.{ Iterable => JIterable }
import java.nio.charset.StandardCharsets
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Paths
import java.util
import java.util.concurrent.atomic.{ AtomicBoolean, AtomicReference }
import java.util.{ Date, UUID }

import io.prometheus.client.CollectorRegistry

import scala.collection.JavaConverters._
import scala.language.implicitConversions

class FlowSpec extends TestBase with ExecutionContextsTests with EmbeddedMqtt with EmbeddedKafka with ExtendedEmbeddedKafkaHelpers[EmbeddedKafkaConfig] {

  def pr[V](topic: String, value: V, headers: (String, String)*) = {
    val headersIterable: JIterable[Header] = headers
      .map(p => new RecordHeader(p._1, p._2.getBytes(UTF_8)): Header).asJava
    val pm: ProducerRecord[String, V] = new ProducerRecord(topic, null, null, null.asInstanceOf[String], value, new RecordHeaders(headersIterable))
    pm
  }

  val mqttBroker = new MqttTest

  "Flow -Niomon MQTT-" must {

    "do loop OK" taggedAs Tag("plum") in {

      implicit val kafkaConfig: EmbeddedKafkaConfig = EmbeddedKafkaConfig(kafkaPort = PortGiver.giveMeKafkaPort, zooKeeperPort = PortGiver.giveMeZookeeperPort)

      val Injector = new InjectorHelperImpl("localhost:" + kafkaConfig.kafkaPort) {}
      val config: Config = Injector.get[Config]

      withRunningKafka {

        def inTopic(deviceId: UUID): String = Paths.get(config.getString(MqttConf.IN_QUEUE_PREFIX), deviceId.toString).toString
        def outTopic(deviceId: UUID): String = Paths.get(config.getString(MqttConf.OUT_QUEUE_PREFIX), deviceId.toString).toString

        val kafkaFlowOut: KafkaFlowOut = Injector.get[KafkaFlowOut]
        kafkaFlowOut.start()

        val mqttClients: MqttClients = Injector.get[MqttClients]
        Injector.get[MqttFlowIn]

        val mqttPublisher: MqttPublisher = Injector.get[MqttPublisher]
        val mqttSubscriber: MqttSubscriber = Injector.get[MqttSubscriber]

        val uuid = UUID.randomUUID()

        val c = new AtomicBoolean(false)
        val flowOut = new AtomicReference[FlowOutPayload](null)

        mqttSubscriber.subscribe(outTopic(uuid), 1)((_, m) => {
          try {
            val payload = FlowOutPayload.parseFrom(m.getPayload)
            flowOut.set(payload)
            c.set(true)
          } catch {
            case e: Exception => e.printStackTrace()
          }

          Task {
            (new RecordMetadata(
              new TopicPartition("topic", 1),
              1,
              1,
              new Date().getTime,
              1L,
              1,
              1
            ), m)
          }
        })

        val requestId = UUID.randomUUID()
        val inPayload = FlowInPayload(uuid.toString, "password", ByteString.copyFrom("hola", StandardCharsets.UTF_8))
        mqttPublisher.publish(
          inTopic(uuid),
          requestId,
          uuid,
          mqttPublisher.toMqttMessage(
            1,
            retained = false,
            inPayload.toByteArray
          )
        )

        Thread.sleep(5000)

        val inPayloadFromKafka = consume[String, Array[Byte]](Set("ubirch-niomon-req-bin"), 1).getOrElse("ubirch-niomon-req-bin", Nil).head

        assert(util.Arrays.equals(inPayloadFromKafka.value(), inPayload.upp.toByteArray))
        assert(inPayloadFromKafka.findHeader(REQUEST_ID).isDefined)
        assert(inPayloadFromKafka.findHeader(X_UBIRCH_GATEWAY_TYPE).contains(MQTT))
        assert(inPayloadFromKafka.findHeader(X_UBIRCH_HARDWARE_ID).contains(uuid.toString))
        assert(inPayloadFromKafka.findHeader(X_UBIRCH_AUTH_TYPE).contains(UBIRCH))
        assert(inPayloadFromKafka.findHeader(X_UBIRCH_CREDENTIAL).contains("password"))
        assert(mqttClients.async.isConnected)

        publishToKafka(pr(
          "ubirch-common-rsp-signed-bin",
          inPayloadFromKafka.value(),
          REQUEST_ID -> requestId.toString,
          X_UBIRCH_HARDWARE_ID -> uuid.toString,
          HTTP_STATUS_CODE -> "200"
        ))

        Thread.sleep(2000)
        assert(flowOut.get().status == "200")
        assert(c.get())

      }

    }

  }

  override protected def beforeEach(): Unit = {
    CollectorRegistry.defaultRegistry.clear()
  }

  override protected def beforeAll(): Unit = {
    mqttBroker.start()
  }

  override protected def afterAll(): Unit = {
    mqttBroker.stop()
  }

}
