package com.ts
package domain.model

import common.enums.{HealthStatus, SlownessStatus}

import com.ts.common.enums.SlownessStatus.Normal

trait NodeStatusSubscriber {
  def updateHealth(node: Node, healthStatus: HealthStatus): Unit
  def updateSlowNess(node: Node,slownessStatus: SlownessStatus): Unit
}

class Node(
            val url: String,
            private var healthStatus: HealthStatus = HealthStatus.Healthy,
            private var slownessStatus: SlownessStatus = Normal
) {
  private var subscribers: List[NodeStatusSubscriber] = Nil

  def getHealthStatus(): HealthStatus = healthStatus

  def getSlownessStatus(): SlownessStatus = slownessStatus


  def attach(subscriber: NodeStatusSubscriber): Unit = {
    subscribers = subscriber :: subscribers
  }

  def detach(subscriber: NodeStatusSubscriber): Unit = {
    subscribers = subscribers.filterNot(_ == subscriber)
  }

  def setHealth(status: HealthStatus): Unit = this.synchronized {
    healthStatus = status
    publishHealthStatusToSubscribers()
  }

  def setSlowStatus(status: SlownessStatus): Unit = this.synchronized {
    slownessStatus = status
    publishSlownessStatusToSubscribers()
  }

  private def publishHealthStatusToSubscribers(): Unit = {
    subscribers.foreach(_.updateHealth(this, getHealthStatus()))
  }

  private def publishSlownessStatusToSubscribers():Unit = {
    subscribers.foreach(_.updateSlowNess(this,getSlownessStatus()))
  }


}
