package com.ts
package infrastructure.adapter

import common.enums.{HealthStatus, SlownessStatus}
import domain.model.{Node, NodeStatusSubscriber}

import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
class MonitoringService extends NodeStatusSubscriber {
  private val logger = LoggerFactory.getLogger(getClass)

  override def updateHealth(
    node: Node,
    healthStatus: HealthStatus
  ): Future[Unit] = Future {
    logger.info(s"send health status to APM service ${node.url}: $healthStatus")
  }

  override def updateSlowness(
    node: Node,
    slownessStatus: SlownessStatus
  ): Future[Unit] = Future {
    logger.info(
      s"send slowness status to APM service ${node.url}: $SlownessStatus"
    )
  }
}
