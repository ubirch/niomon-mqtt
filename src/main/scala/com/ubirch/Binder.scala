package com.ubirch

import com.google.inject.binder.ScopedBindingBuilder
import com.google.inject.{ AbstractModule, Module }
import com.typesafe.config.Config
import com.ubirch.services.config.ConfigProvider
import com.ubirch.services.execution.{ ExecutionProvider, SchedulerProvider }
import com.ubirch.services.flow._
import com.ubirch.services.formats.{ DefaultJsonConverterService, JsonConverterService, JsonFormatsProvider }
import com.ubirch.services.lifeCycle.{ DefaultJVMHook, DefaultLifecycle, JVMHook, Lifecycle }
import com.ubirch.services.rest.SwaggerProvider
import monix.execution.Scheduler
import org.json4s.Formats
import org.scalatra.swagger.Swagger

import scala.concurrent.ExecutionContext

/**
  * Represents the default binder for the system components
  */
class Binder
  extends AbstractModule {

  def Config: ScopedBindingBuilder = bind(classOf[Config]).toProvider(classOf[ConfigProvider])
  def ExecutionContext: ScopedBindingBuilder = bind(classOf[ExecutionContext]).toProvider(classOf[ExecutionProvider])
  def Scheduler: ScopedBindingBuilder = bind(classOf[Scheduler]).toProvider(classOf[SchedulerProvider])
  def Swagger: ScopedBindingBuilder = bind(classOf[Swagger]).toProvider(classOf[SwaggerProvider])
  def Formats: ScopedBindingBuilder = bind(classOf[Formats]).toProvider(classOf[JsonFormatsProvider])
  def Lifecycle: ScopedBindingBuilder = bind(classOf[Lifecycle]).to(classOf[DefaultLifecycle])
  def JVMHook: ScopedBindingBuilder = bind(classOf[JVMHook]).to(classOf[DefaultJVMHook])
  def JsonConverterService: ScopedBindingBuilder = bind(classOf[JsonConverterService]).to(classOf[DefaultJsonConverterService])
  def KafkaFlowOut: ScopedBindingBuilder = bind(classOf[KafkaFlowOut]).to(classOf[DefaultKafkaFlowOut])
  def KafkaFlowIn: ScopedBindingBuilder = bind(classOf[KafkaFlowIn]).to(classOf[DefaultKafkaFlowIn])
  def MqttFlowIn: ScopedBindingBuilder = bind(classOf[MqttFlowIn]).to(classOf[DefaultMqttFlowIn])
  def MqttFlowOut: ScopedBindingBuilder = bind(classOf[MqttFlowOut]).to(classOf[DefaultMqttFlowOut])
  def MqttClients: ScopedBindingBuilder = bind(classOf[MqttClients]).to(classOf[DefaultMqttClients])

  def configure(): Unit = {
    Config
    ExecutionContext
    Scheduler
    Swagger
    Formats
    Lifecycle
    JVMHook
    JsonConverterService
    MqttClients
    KafkaFlowIn
    KafkaFlowOut
    MqttFlowIn
    MqttFlowOut
  }

}

object Binder {
  def modules: List[Module] = List(new Binder)
}
