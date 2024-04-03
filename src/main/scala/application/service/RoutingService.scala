package com.ts
package application.service

import common.enums.{HealthStatus, SlownessStatus}
import common.exceptions.NoHealthyNodeException
import domain.model.{Node, NodeStatusSubscriber}
import domain.service.RoutingStrategy

import org.slf4j.LoggerFactory

class RoutingService(routingStrategy: RoutingStrategy, nodes: Seq[Node]) extends NodeStatusSubscriber {
  private val logger = LoggerFactory.getLogger(getClass)
  def getNextServer: Either[NoHealthyNodeException, Node] = {
    routingStrategy.selectServer(nodes) match
      case Some(server) => Right(server)
      case None => 
        logger.error(s"No health nodes available from the list ${nodes.map(_.url)}")
        Left(new NoHealthyNodeException("No healthy nodes available to handle this request"))
  }

  private def checkAndSetHealthyStatusOfNodes(node: Node, healthStatus: HealthStatus): Unit = {
    nodes.find(_.url == node.url).foreach { node =>
      (node.getHealthStatus(), healthStatus) match {
        case (HealthStatus.Healthy, HealthStatus.NotHealthy) => node.setHealth(healthStatus)
        case (HealthStatus.NotHealthy, HealthStatus.Healthy) => node.setHealth(healthStatus)
        case _ =>
      }
    }
  }
  private def checkAndSetSlowNessStatusOfNodes(node: Node, slownessStatus: SlownessStatus):Unit = {
    nodes.find(_.url == node.url).foreach{
      node ⇒ (node.getSlownessStatus(),slownessStatus) match
        case (SlownessStatus.Slow,SlownessStatus.Normal)⇒ node.setSlowStatus(slownessStatus)
        case (SlownessStatus.Normal,SlownessStatus.Slow)⇒ node.setSlowStatus(slownessStatus)
        case _ ⇒
    }
  }

  override def updateHealth(node: Node, healthStatus: HealthStatus): Unit =  checkAndSetHealthyStatusOfNodes(node, healthStatus)

  override def updateSlowNess(node: Node, slownessStatus: SlownessStatus): Unit = checkAndSetSlowNessStatusOfNodes(node, slownessStatus)
}
