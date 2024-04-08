package com.ts
package infrastructure.adapter

import common.enums.SlownessStatus.Slow
import domain.model.Node
import domain.service.NodeManager
import infrastructure.config.CircuitBreakerConfig

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.ts.common.enums.SlownessStatus
import org.mockito.Mockito.{times, verify}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterAll, Inspectors}
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future
import scala.concurrent.duration.*
class CircuitBreakerManagerSpec
    extends TestKit(ActorSystem("CircuitBreakerManagerSpec"))
    with ImplicitSender
    with Inspectors
    with AnyWordSpecLike
    with BeforeAndAfterAll
    with MockitoSugar
    with Matchers {

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  "getBreakerForNode" should {
    "create a new circuit breaker for a new maybeNode" in {
      val mockNodeManager = mock[NodeManager]
      val circuitBreakerConfig =
        CircuitBreakerConfig(
          maxFailures = 1,
          callTimeout = 1.seconds,
          resetTimeout = 1.minute
        )
      val circuitBreakerManager =
        new CircuitBreakerManager(circuitBreakerConfig, mockNodeManager)

      val node    = Node("http://localhost:8080")
      val breaker = circuitBreakerManager.getBreakerForNode(node)

      breaker should not be (null)
    }
    "open after 5 failures" in {
      implicit val executionContext = system.dispatcher
      val mockNodeManager           = mock[NodeManager]
      val circuitBreakerConfig = CircuitBreakerConfig(
        maxFailures = 5,
        callTimeout = 100.milliseconds, // More realistic timeout
        resetTimeout = 50.milliseconds
      )

      val circuitBreakerManager =
        new CircuitBreakerManager(circuitBreakerConfig, mockNodeManager)
      val node           = Node("test1")
      val circuitBreaker = circuitBreakerManager.getBreakerForNode(node)

      val failingOperation = () =>
        Future {
          Thread.sleep(10) // Simulate a small delay
          throw new RuntimeException("Simulated failure")
        }

      val attempts =
        (1 to 5).map(_ => circuitBreaker.withCircuitBreaker(failingOperation()))

      Future.sequence(attempts).transformWith {
        case util.Failure(_) =>
          circuitBreaker.withCircuitBreaker(failingOperation()).transformWith {
            case util.Failure(_) => // Expected as circuit should be open now
              verify(mockNodeManager)
                .updateSlowness("test1", SlownessStatus.Slow)
              Future.successful(succeed)
            case _ =>
              Future.failed(
                new Exception("Circuit breaker did not open as expected")
              )
          }
        case _ =>
          Future.failed(
            new Exception("Circuit breaker should have failed after 5 attempts")
          )
      }
    }
  }
}
