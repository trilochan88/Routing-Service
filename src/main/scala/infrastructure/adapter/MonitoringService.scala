package com.ts
package infrastructure.adapter

import common.enums.{HealthStatus, SlownessStatus}
import domain.model.{Node, NodeStatusSubscriber}

import org.slf4j.LoggerFactory

class MonitoringService extends NodeStatusSubscriber {
  private val logger = LoggerFactory.getLogger(getClass)
  override def updateHealth(node: Node, healthStatus: HealthStatus): Unit = {
    (node.getHealthStatus(), healthStatus) match {
      case (HealthStatus.Healthy, HealthStatus.NotHealthy) =>  logger.error(s"Node ${node.url} is not healthy")
      case (HealthStatus.NotHealthy, HealthStatus.Healthy) => logger.info(s"Node ${node.url} is healthy")
      case _ =>
    }
  }

  override def updateSlowNess(node: Node, slownessStatus: SlownessStatus): Unit = {
    (node.getSlownessStatus(), slownessStatus) match
      case (SlownessStatus.Slow, SlownessStatus.Normal) ⇒ logger.info(s"Node ${node.url} is normal now")
      case (SlownessStatus.Normal, SlownessStatus.Slow) ⇒ logger.error(s"Node ${node.url} is slow")
      case _ ⇒
  }
}
