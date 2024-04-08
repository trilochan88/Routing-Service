package com.ts
package infrastructure.adapter

import common.enums.SlownessStatus.{Normal, Slow}
import domain.model.Node
import domain.service.NodeManager
import infrastructure.config.CircuitBreakerConfig

import akka.actor.ActorSystem
import akka.pattern.CircuitBreaker
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global

class CircuitBreakerManager(
  circuitBreakerConfig: CircuitBreakerConfig,
  nodeManager: NodeManager
)(implicit system: ActorSystem) {
  private val logger = LoggerFactory.getLogger(getClass)
  private val nodeCircuitBreakers
    : scala.collection.mutable.Map[String, CircuitBreaker] =
    scala.collection.mutable.Map()

  def getBreakerForNode(node: Node): CircuitBreaker = {
    val circuitBreaker =
      nodeCircuitBreakers.getOrElseUpdate(node.url, createNewBreaker(node))
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
    logger.error(
      s"CircuitBreaker notifyWhenOpened notify slow for maybeNode ${node.toString}"
    )
    nodeManager.updateNodeSlowness(node.url, Slow)
  }

  private def notifyWhenClosed(node: Node): Unit = {
    logger.info(
      s"CircuitBreaker NotifyWhenClosed notify normal for maybeNode ${node.toString}"
    )
    nodeManager.updateNodeSlowness(node.url, Normal)
  }

  private def notifyWhenHalfOpen(node: Node): Unit =
    logger.warn(s"Circuit for ${node.url} half-open")

}
