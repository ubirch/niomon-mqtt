package com.ubirch
package services.flow

import java.util.UUID

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import com.ubirch.ConfPaths.FlowInProducerConfPaths
import com.ubirch.kafka.express.ExpressProducer
import com.ubirch.kafka.producer.{ ProducerRunner, WithProducerShutdownHook }
import com.ubirch.services.lifeCycle.Lifecycle
import javax.inject._
import monix.eval.Task
import net.logstash.logback.argument.StructuredArguments.v
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.{ ByteArraySerializer, Serializer, StringSerializer }
import org.joda.time.DateTime
import org.json4s.{ DefaultFormats, Formats }

import scala.concurrent.TimeoutException
import scala.concurrent.duration.FiniteDuration
import scala.language.postfixOps

trait KafkaFlowIn extends LazyLogging {

  def publish(requestId: UUID, deviceId: UUID, password: String, upp: Array[Byte], time: DateTime): Task[RecordMetadata]

  def publishAsOpt(requestId: UUID, deviceId: UUID, password: String, upp: Array[Byte], time: DateTime): Task[Option[RecordMetadata]] = {
    publish(requestId, deviceId, password, upp, time)
      .map(x => Option(x))
      .onErrorHandle {
        e =>
          logger.error("Error publishing to kafka, deviceId={} exception={} error_message", deviceId.toString, e.getClass.getName, e.getMessage, v("requestId", requestId.toString))
          None
      }
  }

  def publish(requestId: UUID, deviceId: UUID, password: String, upp: Array[Byte], timeout: FiniteDuration, time: DateTime): Task[(RecordMetadata, UUID)] = {
    for {
      maybeRM <- publishAsOpt(requestId, deviceId, password, upp, time)
        .timeoutTo(timeout, Task.raiseError(FailedKafkaPublish(deviceId, Option(new TimeoutException(s"failed_publish_timeout=${timeout.toString()}")))))
        .onErrorHandleWith(e => Task.raiseError(FailedKafkaPublish(deviceId, Option(e))))
      _ = if (maybeRM.isEmpty) logger.error(s"failed_publish=$deviceId", v("requestId", requestId.toString))
      _ = if (maybeRM.isDefined) logger.debug(s"publish_succeeded_for=$deviceId", v("requestId", requestId.toString))
      _ <- earlyResponseIf(maybeRM.isEmpty)(FailedKafkaPublish(deviceId, None))
    } yield {
      (maybeRM.get, deviceId)
    }
  }

  protected def earlyResponseIf(condition: Boolean)(response: Exception): Task[Unit] =
    if (condition) Task.raiseError(response) else Task.unit

}

abstract class KafkaFlowInImpl(config: Config, lifecycle: Lifecycle)
  extends KafkaFlowIn
  with ExpressProducer[String, Array[Byte]]
  with WithProducerShutdownHook
  with LazyLogging {

  override val producerBootstrapServers: String = config.getString(FlowInProducerConfPaths.BOOTSTRAP_SERVERS)
  override val lingerMs: Int = config.getInt(FlowInProducerConfPaths.LINGER_MS)
  override val keySerializer: Serializer[String] = new StringSerializer
  override val valueSerializer: Serializer[Array[Byte]] = new ByteArraySerializer

  val producerTopic: String = config.getString(FlowInProducerConfPaths.TOPIC_PATH)

  override def publish(requestId: UUID, deviceId: UUID, password: String, upp: Array[Byte], time: DateTime): Task[RecordMetadata] = Task.defer {
    Task.fromFuture {
      send(producerTopic, upp,
        REQUEST_ID -> requestId.toString,
        X_UBIRCH_GATEWAY_TYPE -> MQTT,
        X_UBIRCH_HARDWARE_ID -> deviceId.toString,
        X_UBIRCH_AUTH_TYPE -> UBIRCH,
        X_UBIRCH_CREDENTIAL -> password,
        X_ENTRY_TIME -> time.toString())
    }
  }

  lifecycle.addStopHook(hookFunc(production))

}

@Singleton
class DefaultKafkaFlowIn @Inject() (config: Config, lifecycle: Lifecycle)
  extends KafkaFlowInImpl(config, lifecycle) {

  implicit val formats: Formats = DefaultFormats

  override lazy val production: ProducerRunner[String, Array[Byte]] = ProducerRunner(producerConfigs, Some(keySerializer), Some(valueSerializer))

}
