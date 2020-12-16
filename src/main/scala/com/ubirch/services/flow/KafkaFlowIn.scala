package com.ubirch.services.flow

import java.util.UUID

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import com.ubirch.ConfPaths.FlowInProducerConfPaths
import com.ubirch.FailedKafkaPublish
import com.ubirch.kafka.express.ExpressProducer
import com.ubirch.kafka.producer.{ ProducerRunner, WithProducerShutdownHook }
import com.ubirch.services.lifeCycle.Lifecycle
import javax.inject._
import monix.eval.Task
import monix.execution.{ CancelableFuture, Scheduler }
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.{ ByteArraySerializer, Serializer, StringSerializer }
import org.json4s.{ DefaultFormats, Formats }

import scala.concurrent.TimeoutException
import scala.concurrent.duration.{ FiniteDuration, _ }
import scala.language.postfixOps

trait KafkaFlowIn extends LazyLogging {

  def publish(deviceId: UUID, password: String, upp: Array[Byte]): Task[RecordMetadata]

  def publish_!(deviceId: UUID, password: String, upp: Array[Byte])(implicit scheduler: Scheduler): CancelableFuture[RecordMetadata] = publish(deviceId, password, upp).runToFuture

  def publishAsOpt(deviceId: UUID, password: String, upp: Array[Byte]): Task[Option[RecordMetadata]] = {
    publish(deviceId, password, upp)
      .map(x => Option(x))
      .onErrorHandle {
        e =>
          logger.error("Error publishing to kafka, deviceId={} exception={} error_message", deviceId.toString, e.getClass.getName, e.getMessage)
          None
      }
  }

  def publish(deviceId: UUID, password: String, upp: Array[Byte], timeout: FiniteDuration = 10 seconds): Task[(RecordMetadata, UUID)] = {
    for {
      maybeRM <- publishAsOpt(deviceId, password, upp)
        .timeoutTo(timeout, Task.raiseError(FailedKafkaPublish(deviceId, Option(new TimeoutException(s"failed_publish_timeout=${timeout.toString()}")))))
        .onErrorHandleWith(e => Task.raiseError(FailedKafkaPublish(deviceId, Option(e))))
      _ = if (maybeRM.isEmpty) logger.error("failed_publish={}", deviceId.toString)
      _ = if (maybeRM.isDefined) logger.info("publish_succeeded_for={}", deviceId)
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

  override def publish(deviceId: UUID, password: String, upp: Array[Byte]): Task[RecordMetadata] = Task.defer {
    Task.fromFuture {
      send(producerTopic, upp,
        "X-Ubirch-Hardware-Id" -> deviceId.toString,
        "X-Ubirch-Auth-Type" -> "ubirch",
        "X-Ubirch-Credential" -> password)
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
