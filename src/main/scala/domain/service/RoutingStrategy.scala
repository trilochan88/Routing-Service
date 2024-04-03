package com.ts
package domain.service

import common.enums.{HealthStatus, SlownessStatus}
import domain.model.Node

import java.util.concurrent.atomic.AtomicInteger

trait RoutingStrategy {
  def selectServer(servers: Seq[Node]): Option[Node]
}

class RoundRobinStrategy extends RoutingStrategy {
  private val index = new AtomicInteger(0)

  override def selectServer(servers: Seq[Node]): Option[Node] = {
    val healthyServers = servers
      .filter(_.getHealthStatus() == HealthStatus.Healthy)
      .filter(_.getSlownessStatus() == SlownessStatus.Normal)
    if (healthyServers.isEmpty) {
      None
    } else {
      val nextIndex = index.getAndUpdate(i => (i + 1) % healthyServers.length)
      Some(healthyServers(nextIndex))
    }
  }
}
