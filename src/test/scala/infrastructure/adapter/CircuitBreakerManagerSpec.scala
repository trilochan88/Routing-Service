package com.ts
package infrastructure.adapter

import domain.model.Node
import infrastructure.config.CircuitBreakerConfig

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.ts.common.enums.SlownessStatus.Slow
import org.mockito.Mockito.verify
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
    "create a new circuit breaker for a new node" in {
      val circuitBreakerConfig =
        CircuitBreakerConfig(maxFailures = 1, callTimeout = 1.seconds, resetTimeout = 1.minute)
      val circuitBreakerManager = new CircuitBreakerManager(circuitBreakerConfig)

      val node = Node("http://localhost:8080")
      val breaker = circuitBreakerManager.getBreakerForNode(node)

      breaker should not be (null)
    }
    "open after 5 failures" in {
      implicit val executionContext = system.dispatcher
      val circuitBreakerConfig =
        CircuitBreakerConfig(
          maxFailures = 1,
          callTimeout = 1.nanoseconds,
          resetTimeout = 50.millisecond
        )
      val circuitBreakerManager = new CircuitBreakerManager(circuitBreakerConfig)

      val node = mock[Node]
      val circuitBreaker = circuitBreakerManager.getBreakerForNode(node)

      val failingOperation = () =>
        Future {
          throw new RuntimeException("Simulated failure")
        }

      val attempts = (1 to 5).map(_ => circuitBreaker.withCircuitBreaker(failingOperation()))

      Future
        .sequence(attempts)
        .recoverWith { case _ =>
          circuitBreaker
            .withCircuitBreaker(failingOperation())
            .map(_ => fail("Should not reach this point"))
        }
        .recover { case _: RuntimeException =>
          succeed
        }

      verify(node).setSlowStatus(Slow)
    }
  }
}
