package com.ts
package domain.model

import common.enums.SlownessStatus.Normal
import common.enums.{HealthStatus, SlownessStatus}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
trait NodeStatusSubscriber {
  def updateHealth(node: Node, healthStatus: HealthStatus): Future[Unit]
  def updateSlowness(node: Node, slownessStatus: SlownessStatus): Future[Unit]
}
/**
 * Contain
 */
case class Node(
   url: String,
   healthStatus: HealthStatus = HealthStatus.Healthy,
   slownessStatus: SlownessStatus = Normal,
   subscribers: List[NodeStatusSubscriber] = List.empty[NodeStatusSubscriber]
) {

  def attach(subscriber: NodeStatusSubscriber): Node = {
    this.copy(subscribers =  subscriber :: subscribers)
  }

  def detach(subscriber: NodeStatusSubscriber): Node = {
    this.copy(subscribers = subscribers.filterNot(_ == subscriber))
  }

  def setHealth(status: HealthStatus): Future[Node] = {
    val updatedNode = this.copy(healthStatus = status)
    Future.sequence(subscribers.map(_.updateHealth(updatedNode,status)))
      .map(_ ⇒ updatedNode)
  }

  def setSlowStatus(status: SlownessStatus): Unit =  {
    val updatedNode = this.copy(slownessStatus = status)
    Future.sequence(subscribers.map(_.updateSlowness(updatedNode, status)))
      .map(_ ⇒ updatedNode)
  }

}
