package com.ts
package infrastructure.validators

import common.exceptions.InvalidRequestBodyException

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpEntity
import akka.util.ByteString
import io.circe.parser.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
object JsonValidator {
  def isValidJson(
    entity: HttpEntity
  )(implicit system: ActorSystem): Future[Boolean] = {
    entity.dataBytes.runFold(ByteString(""))(_ ++ _).flatMap { bytes ⇒
      val jsonStr = bytes.utf8String
    validateJson(jsonStr)
    }
  }

  private def validateJson(jsonStr: String): Future[Boolean] = {
    parse(jsonStr) match
      case Left(ex) ⇒
        Future.failed(
          new InvalidRequestBodyException(
            "Request contain invalid body",
            ex.getCause
          )
        )
      case Right(value) ⇒ Future.successful(true)
  }
}
