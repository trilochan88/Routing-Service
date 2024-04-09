package com.ts
package infrastructure.validators

import common.exceptions.InvalidRequestBodyException

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.testkit.TestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Await
import scala.concurrent.duration.*

class JsonValidatorTest
    extends TestKit(ActorSystem("Validator"))
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "JsonValidator on call of isValidJson" should {
    "should return true for valid JSON" in {
      val validJson =  """
                         |{
                         |	"game":"Mobile",
                         |	"gamerID": "GYUTDTE",
                         |	"points":20
                         |}
                         |""".stripMargin
      val entity    = HttpEntity(ContentTypes.`application/json`, validJson)
      val result    = Await.result(JsonValidator.isValidJson(entity), 3.seconds)
      result should be(true)
    }

    " throw InvalidRequestBodyException for invalid JSON" in {
      val invalidJson =  """
                           |{
                           |	"game":"Mobile",
                           |	"gamerID": "GYUTDTE",
                           |	"points":20
                           |
                           |""".stripMargin
      val entity      = HttpEntity(ContentTypes.`application/json`, invalidJson)

      an[InvalidRequestBodyException] should be thrownBy {
        Await.result(JsonValidator.isValidJson(entity), 3.seconds)
      }
    }
  }

}
