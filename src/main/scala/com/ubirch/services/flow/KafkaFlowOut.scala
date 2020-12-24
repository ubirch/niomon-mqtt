package com.ubirch
package services.flow

import java.util.UUID

import com.google.protobuf.ByteString
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import com.ubirch.ConfPaths.{ FlowOutConsumerConfPaths, FlowOutProducerConfPaths }
import com.ubirch.kafka.consumer.WithConsumerShutdownHook
import com.ubirch.kafka.express.ExpressKafka
import com.ubirch.kafka.producer.WithProducerShutdownHook
import com.ubirch.kafka.util.Implicits.enrichedConsumerRecord
import com.ubirch.models.FlowOutPayload
import com.ubirch.services.lifeCycle.Lifecycle
import com.ubirch.util.DateUtil
import javax.inject._
import monix.eval.Task
import monix.execution.Scheduler
import net.logstash.logback.argument.StructuredArguments.v
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization._
import org.joda.time.DateTime

import scala.util.{ Failure, Try }

abstract class KafkaFlowOut(val config: Config, lifecycle: Lifecycle)
  extends ExpressKafka[String, Array[Byte], Unit]
  with WithConsumerShutdownHook
  with WithProducerShutdownHook
  with LazyLogging {

  override val keyDeserializer: Deserializer[String] = new StringDeserializer
  override val valueDeserializer: Deserializer[Array[Byte]] = new ByteArrayDeserializer
  override val consumerTopics: Set[String] = Set(config.getString(FlowOutConsumerConfPaths.IMPORT_TOPIC_PATH))
  override val keySerializer: Serializer[String] = new StringSerializer
  override val valueSerializer: Serializer[Array[Byte]] = new ByteArraySerializer
  override val consumerBootstrapServers: String = config.getString(FlowOutConsumerConfPaths.BOOTSTRAP_SERVERS)
  override val consumerGroupId: String = config.getString(FlowOutConsumerConfPaths.GROUP_ID_PATH)
  override val consumerMaxPollRecords: Int = config.getInt(FlowOutConsumerConfPaths.MAX_POLL_RECORDS)
  override val consumerGracefulTimeout: Int = config.getInt(FlowOutConsumerConfPaths.GRACEFUL_TIMEOUT_PATH)
  override val metricsSubNamespace: String = config.getString(FlowOutConsumerConfPaths.METRICS_SUB_NAMESPACE)
  override val consumerReconnectBackoffMsConfig: Long = config.getLong(FlowOutConsumerConfPaths.RECONNECT_BACKOFF_MS_CONFIG)
  override val consumerReconnectBackoffMaxMsConfig: Long = config.getLong(FlowOutConsumerConfPaths.RECONNECT_BACKOFF_MAX_MS_CONFIG)
  override val maxTimeAggregationSeconds: Long = 120
  override val producerBootstrapServers: String = config.getString(FlowOutProducerConfPaths.BOOTSTRAP_SERVERS)
  override val lingerMs: Int = config.getInt(FlowOutProducerConfPaths.LINGER_MS)

  lifecycle.addStopHooks(hookFunc(consumerGracefulTimeout, consumption), hookFunc(production))

}

@Singleton
class DefaultKafkaFlowOut @Inject() (
    mqttFlowOut: MqttFlowOut,
    config: Config,
    lifecycle: Lifecycle
)(implicit val scheduler: Scheduler) extends KafkaFlowOut(config, lifecycle) {

  def logic(cr: ConsumerRecord[String, Array[Byte]]): Task[Unit] = Task.delay {
    val requestId = cr.findHeader(REQUEST_ID)
    val hwId = cr.findHeader(X_UBIRCH_HARDWARE_ID)
    val sts = cr.findHeader(HTTP_STATUS_CODE).orElse(Option("200"))
    val entryTime: Try[DateTime] = cr.findHeader(X_ENTRY_TIME).map(DateUtil.parseToUTC).getOrElse(Failure(NoEntryTimeException))

    (for {
      requestId <- requestId.flatMap(x => Try(UUID.fromString(x)).toOption)
      deviceId <- hwId.flatMap(x => Try(UUID.fromString(x)).toOption)
      status <- sts
      _ = logger.debug("kafka_fo_message_uuid=" + deviceId.toString, v("requestId", requestId.toString))
    } yield {
      mqttFlowOut.process(requestId, deviceId, FlowOutPayload(status, ByteString.copyFrom(cr.value())), entryTime)
    }).getOrElse {
      logger.warn("kafka_fo_message_incomplete", v("requestId", requestId.getOrElse("no-request-id")))
    }
  }

  override val process: Process = Process.async { crs =>
    Task.sequence { crs.map { cr => logic(cr) } }
      .flatMap(_ => Task.unit)
      .runToFuture
  }

  override def prefix: String = "Ubirch"

}
