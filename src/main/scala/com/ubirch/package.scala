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

  case class FailedKafkaPublish(deviceId: UUID, maybeThrowable: Option[Throwable])
    extends ServiceException(maybeThrowable.map(_.getMessage).getOrElse("Failed Publish"))

}
