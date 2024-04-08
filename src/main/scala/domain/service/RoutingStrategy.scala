package com.ts
package domain.service

import common.enums.{HealthStatus, SlownessStatus}
import domain.model.Node

import org.slf4j.LoggerFactory

import java.util.concurrent.atomic.AtomicInteger

trait RoutingStrategy {
  def selectNextNode(nodes: Seq[Node]): Option[Node]
}

class RoundRobinStrategy extends RoutingStrategy {
  private val logger = LoggerFactory.getLogger(getClass)
  private val index  = new AtomicInteger(0)

  override def selectNextNode(nodes: Seq[Node]): Option[Node] = {
    val healthyNodes = nodes
      .filter(_.healthStatus == HealthStatus.Healthy)
      .filter(_.slownessStatus == SlownessStatus.Normal)
    if (healthyNodes.isEmpty) {
      None
    } else {
      val nextIndex   = index.getAndUpdate(i => (i + 1) % healthyNodes.length)
      val healthyNode = Some(healthyNodes(nextIndex))
      logger.info(s"Healthy Node + ${healthyNode.value.toString}")
      healthyNode
    }
  }
}
