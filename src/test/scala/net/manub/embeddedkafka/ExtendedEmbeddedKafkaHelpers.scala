package net.manub.embeddedkafka

import org.apache.kafka.clients.consumer.{ ConsumerConfig, ConsumerRecord, KafkaConsumer, OffsetAndMetadata }
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.{ KafkaException, TopicPartition }

import scala.collection.mutable.ListBuffer
import scala.concurrent.TimeoutException
import scala.concurrent.duration._
import scala.util.Try
import scala.collection.JavaConverters._

trait ExtendedEmbeddedKafkaHelpers[C <: EmbeddedKafkaConfig] {

  _: EmbeddedKafka =>

  def consume[K, V](
      topics: Set[String],
      number: Int,
      autoCommit: Boolean = false,
      timeout: Duration = 5.seconds,
      resetTimeoutOnEachMessage: Boolean = true
  )(
      implicit
      config: C,
      keyDeserializer: Deserializer[K],
      valueDeserializer: Deserializer[V]
  ): Map[String, List[ConsumerRecord[K, V]]] = {

    val consumerProperties = baseConsumerConfig ++ Map[String, Object](
      ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG -> autoCommit.toString
    )

    var timeoutNanoTime = System.nanoTime + timeout.toNanos
    val consumer = new KafkaConsumer[K, V](
      consumerProperties.asJava,
      keyDeserializer,
      valueDeserializer
    )

    val messages = Try {
      val messagesBuffers = topics.map(_ -> ListBuffer.empty[ConsumerRecord[K, V]]).toMap
      var messagesRead = 0
      consumer.subscribe(topics.asJava)
      topics.foreach(consumer.partitionsFor)

      while (messagesRead < number && System.nanoTime < timeoutNanoTime) {
        val recordIter =
          consumer.poll(duration2JavaDuration(consumerPollingTimeout)).iterator
        if (resetTimeoutOnEachMessage && recordIter.hasNext) {
          timeoutNanoTime = System.nanoTime + timeout.toNanos
        }
        while (recordIter.hasNext && messagesRead < number) {
          val record = recordIter.next
          messagesBuffers(record.topic) += record
          val tp = new TopicPartition(record.topic, record.partition)
          val om = new OffsetAndMetadata(record.offset + 1)
          consumer.commitSync(Map(tp -> om).asJava)
          messagesRead += 1
        }
      }
      if (messagesRead < number) {
        throw new TimeoutException(
          s"Unable to retrieve $number message(s) from Kafka in $timeout"
        )
      }
      messagesBuffers.mapValues(_.toList)
    }

    consumer.close()
    messages.recover {
      case ex: KafkaException => throw new KafkaUnavailableException(ex)
    }.get
  }

}
