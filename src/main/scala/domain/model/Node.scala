package com.ts
package domain.model

import common.enums.SlownessStatus.Normal
import common.enums.{HealthStatus, SlownessStatus}

import org.slf4j.LoggerFactory

import java.util.concurrent.locks.ReentrantLock
trait NodeStatusSubscriber {
  def updateHealth(node: Option[Node], healthStatus: HealthStatus): Unit
  def updateSlowness(node: Option[Node], slownessStatus: SlownessStatus): Unit
}

/** Contain
  */
case class Node(
  url: String,
  healthStatus: HealthStatus = HealthStatus.Healthy,
  slownessStatus: SlownessStatus = Normal,
)
