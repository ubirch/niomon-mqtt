package com

import java.util.UUID

import scala.util.control.NoStackTrace

package object ubirch {

  /**
    * Represents Generic Top Level Exception for the Service System
    * @param message Represents the error message.
    */

  abstract class ServiceException(message: String) extends Exception(message) with NoStackTrace {
    val name: String = this.getClass.getCanonicalName
  }

  case object NoEntryTimeException extends ServiceException("No Entry Time")

  case class FailedKafkaPublish(deviceId: UUID, maybeThrowable: Option[Throwable])
    extends ServiceException(maybeThrowable.map(_.getMessage).getOrElse("Failed Publish"))

  def REQUEST_ID = "request-id"
  def X_UBIRCH_GATEWAY_TYPE = "X-Ubirch-Gateway-Type"
  def X_UBIRCH_HARDWARE_ID = "X-Ubirch-Hardware-Id"
  def X_UBIRCH_AUTH_TYPE = "X-Ubirch-Auth-Type"
  def X_UBIRCH_CREDENTIAL = "X-Ubirch-Credential"
  def HTTP_STATUS_CODE = "http-status-code"
  def X_ENTRY_TIME = "X-Entry-Time"

  def MQTT = "mqtt"
  def UBIRCH = "ubirch"

}
