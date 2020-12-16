package com.ubirch

import com.google.inject.binder.ScopedBindingBuilder
import com.typesafe.config.{ Config, ConfigValueFactory }
import com.ubirch.ConfPaths.{ FlowInProducerConfPaths, FlowOutConsumerConfPaths, FlowOutProducerConfPaths }
import com.ubirch.services.config.ConfigProvider

class InjectorHelperImpl(bootstrapServers: String) extends InjectorHelper(List(new Binder {

  override def Config: ScopedBindingBuilder = bind(classOf[Config]).toProvider(new ConfigProvider {
    override def conf: Config = super.conf
      .withValue(FlowOutConsumerConfPaths.BOOTSTRAP_SERVERS, ConfigValueFactory.fromAnyRef(bootstrapServers))
      .withValue(FlowOutProducerConfPaths.BOOTSTRAP_SERVERS, ConfigValueFactory.fromAnyRef(bootstrapServers))
      .withValue(FlowInProducerConfPaths.BOOTSTRAP_SERVERS, ConfigValueFactory.fromAnyRef(bootstrapServers))
  })

}))
