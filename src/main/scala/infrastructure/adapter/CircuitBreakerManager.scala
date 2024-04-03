package com.ts
package infrastructure.adapter

import common.enums.SlownessStatus.{Normal, Slow}
import domain.model.Node
import infrastructure.config.CircuitBreakerConfig

import akka.actor.ActorSystem
import akka.pattern.CircuitBreaker
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContextExecutor

class CircuitBreakerManager(circuitBreakerConfig: CircuitBreakerConfig)(implicit
    system: ActorSystem
) {
  private val logger = LoggerFactory.getLogger(getClass)
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  private val nodeCircuitBreakers: scala.collection.mutable.Map[String, CircuitBreaker] =
    scala.collection.mutable.Map()

  def getBreakerForNode(node: Node): CircuitBreaker = {
    if (nodeCircuitBreakers.contains(node.url)) {
      logger.info(s"Existing circuitBreaker ${node.url}")
    }
    val circuitBreaker = nodeCircuitBreakers.getOrElseUpdate(node.url, createNewBreaker(node))
    logger.info(
      s"Circuit Breaker config - ${circuitBreaker.toString} - is open = ${circuitBreaker.isOpen}"
    )
    circuitBreaker
  }

  private def createNewBreaker(node: Node): CircuitBreaker = {
    new CircuitBreaker(
      system.scheduler,
      maxFailures = circuitBreakerConfig.maxFailures,
      callTimeout = circuitBreakerConfig.callTimeout,
      resetTimeout = circuitBreakerConfig.resetTimeout
    ).onOpen(notifyWhenOpened(node))
      .onClose(notifyWhenClosed(node))
      .onHalfOpen(notifyWhenHalfOpen(node))
    
  }

  private def notifyWhenOpened(node: Node): Unit = {
    logger.error("CircuitBreaker notifyWhenOpened")
    node.setSlowStatus(Slow)
  }

  private def notifyWhenClosed(node: Node): Unit = {
    logger.info("CircuitBreaker NotifyWhenClosed")
    node.setSlowStatus(Normal)
  }

  private def notifyWhenHalfOpen(node: Node): Unit =
    logger.warn(s"Circuit for ${node.url} half-open")

}
