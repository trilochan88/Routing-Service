package com.ts
package domain.service

import common.enums.{HealthStatus, SlownessStatus}
import domain.model.Node

import java.util.concurrent.atomic.AtomicInteger

trait RoutingStrategy {
  def selectNextNode(nodes: Seq[Node]): Option[Node]
}

class RoundRobinStrategy extends RoutingStrategy {
  private val index = new AtomicInteger(0)

  override def selectNextNode(nodes: Seq[Node]): Option[Node] = {
    val healthyNode = nodes
      .filter(_.healthStatus == HealthStatus.Healthy)
      .filter(_.slownessStatus == SlownessStatus.Normal)
    if (healthyNode.isEmpty) {
      None
    } else {
      val nextIndex = index.getAndUpdate(i => (i + 1) % healthyNode.length)
      Some(healthyNode(nextIndex))
    }
  }
}
