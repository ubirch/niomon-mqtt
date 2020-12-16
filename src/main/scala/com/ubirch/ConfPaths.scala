package com.ubirch

/**
  * Object that contains configuration keys
  */
object ConfPaths {

  trait GenericConfPaths {
    final val NAME = "niomon-mqtt.name"
  }

  trait HttpServerConfPaths {
    final val PORT = "niomon-mqtt.server.port"
    final val SWAGGER_PATH = "niomon-mqtt.server.swaggerPath"
  }

  trait ExecutionContextConfPaths {
    final val THREAD_POOL_SIZE = "niomon-mqtt.executionContext.threadPoolSize"
  }

  trait FlowOutConsumerConfPaths {
    final val BOOTSTRAP_SERVERS = "niomon-mqtt.flow-out.kafkaConsumer.bootstrapServers"
    final val IMPORT_TOPIC_PATH = "niomon-mqtt.flow-out.kafkaConsumer.import"
    final val ACTIVATION_TOPIC_PATH = "niomon-mqtt.flow-out.kafkaConsumer.activation"
    final val MAX_POLL_RECORDS = "niomon-mqtt.flow-out.kafkaConsumer.maxPollRecords"
    final val GROUP_ID_PATH = "niomon-mqtt.flow-out.kafkaConsumer.groupId"
    final val GRACEFUL_TIMEOUT_PATH = "niomon-mqtt.flow-out.kafkaConsumer.gracefulTimeout"
    final val METRICS_SUB_NAMESPACE = "niomon-mqtt.flow-out.kafkaConsumer.metricsSubNamespace"
    final val FETCH_MAX_BYTES_CONFIG = "niomon-mqtt.flow-out.kafkaConsumer.fetchMaxBytesConfig"
    final val MAX_PARTITION_FETCH_BYTES_CONFIG = "niomon-mqtt.flow-out.kafkaConsumer.maxPartitionFetchBytesConfig"
    final val RECONNECT_BACKOFF_MS_CONFIG = "niomon-mqtt.flow-out.kafkaConsumer.reconnectBackoffMsConfig"
    final val RECONNECT_BACKOFF_MAX_MS_CONFIG = "niomon-mqtt.flow-out.kafkaConsumer.reconnectBackoffMaxMsConfig"
  }

  trait FlowOutProducerConfPaths {
    final val LINGER_MS = "niomon-mqtt.flow-out.kafkaProducer.lingerMS"
    final val BOOTSTRAP_SERVERS = "niomon-mqtt.flow-out.kafkaProducer.bootstrapServers"
    final val ERROR_TOPIC_PATH = "niomon-mqtt.flow-out.kafkaProducer.errorTopic"
  }

  trait FlowInProducerConfPaths {
    final val LINGER_MS = "niomon-mqtt.flow-in.kafkaProducer.lingerMS"
    final val BOOTSTRAP_SERVERS = "niomon-mqtt.flow-in.kafkaProducer.bootstrapServers"
    final val TOPIC_PATH = "niomon-mqtt.flow-in.kafkaProducer.topic"
    final val ERROR_TOPIC_PATH = "niomon-mqtt.flow-in.kafkaProducer.errorTopic"
  }

  trait PrometheusConfPaths {
    final val PORT = "niomon-mqtt.metrics.prometheus.port"
  }

  trait MqttConf {
    final val BROKER_URL = "niomon-mqtt.mqtt.brokerUrl"
    final val CLIENT_ID = "niomon-mqtt.mqtt.clientId"
    final val QOS = "niomon-mqtt.mqtt.qualityOfService"
    final val USER_NAME = "niomon-mqtt.mqtt.userName"
    final val PASSWORD = "niomon-mqtt.mqtt.password"
    final val QUEUE_PREFIX = "niomon-mqtt.mqtt.queuePrefix"
  }

  object GenericConfPaths extends GenericConfPaths
  object FlowOutConsumerConfPaths extends FlowOutConsumerConfPaths
  object FlowOutProducerConfPaths extends FlowOutProducerConfPaths
  object HttpServerConfPaths extends HttpServerConfPaths
  object MqttConf extends MqttConf
  object FlowInProducerConfPaths extends FlowInProducerConfPaths

}
