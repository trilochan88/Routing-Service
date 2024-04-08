package com.ts
package infrastructure.adapter

import common.enums.{HealthStatus, SlownessStatus}
import domain.model.Node

import com.ts.domain.service.NodeStatusSubscriber
import org.slf4j.LoggerFactory
class MonitoringService extends NodeStatusSubscriber {
  private val logger = LoggerFactory.getLogger(getClass)
  override def updateHealth(
    node: Option[Node],
    healthStatus: HealthStatus
  ): Unit =  {
    logger.info(s"send health status to APM service ${node.toString} : $healthStatus")
  }

  override def updateSlowness(
    node: Option[Node],
    slownessStatus: SlownessStatus
  ): Unit =  {
    logger.info(
      s"send slowness status to APM service ${node.toString}: $SlownessStatus"
    )
  }
}
