package com.ts
package application.service

import common.enums.{HealthStatus, SlownessStatus}
import common.exceptions.NoHealthyNodeException
import domain.model.{Node, NodeStatusSubscriber}
import domain.service.RoutingStrategy

import org.slf4j.LoggerFactory

import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec
import scala.collection.immutable

class RoutingService(routingStrategy: RoutingStrategy, initialNodes: Seq[Node])
    extends NodeStatusSubscriber {
  private val logger = LoggerFactory.getLogger(getClass)
  private[application] val nodes: AtomicReference[Map[String, Node]] =
    new AtomicReference(initialNodes.map(node => node.url -> node).toMap)
  def getNextNode: Either[NoHealthyNodeException, Node] = {
    val currentNodes = nodes.get()
    logger.info(s"Current nodes = ${currentNodes.toString()}")
    routingStrategy.selectNextNode(currentNodes.values.toSeq) match
      case Some(node) => Right(node)
      case None =>
        logger.error(
          s"No health nodes available from the list of size ${currentNodes.size}"
        )
        Left(
          new NoHealthyNodeException(
            "No healthy nodes available to handle this request"
          )
        )
  }

  /**
   * This code can be refactored and move to node manager and node manager can be single source of truth, during my review
   * @param maybeNode
   * @param healthStatus
   */
  override def updateHealth(
    maybeNode: Option[Node],
    healthStatus: HealthStatus
  ): Unit = {
    maybeNode match
      case Some(node) ⇒ {
        logger.info(
          s"Attempting to update health status for maybeNode with URL ${node.url}"
        )
        updateNodeConditionally(
          nodes,
          node.url,
          _.copy(healthStatus = healthStatus)
        )
      }
      case _ ⇒ logger.error("No maybeNode found")

  }

  override def updateSlowness(
    maybeNode: Option[Node],
    slownessStatus: SlownessStatus
  ): Unit = {
    maybeNode match
      case Some(node) ⇒ {
        logger.info(
          s"Attempting to update slowness status for maybeNode with URL ${node.url} to $slownessStatus"
        )
        updateNodeConditionally(
          nodes,
          node.url,
          _.copy(slownessStatus = slownessStatus)
        )
      }
      case None ⇒ logger.error("No maybeNode found")
  }

  private def updateNodeConditionally(
    nodes: AtomicReference[Map[String, Node]],
    url: String,
    update: Node => Node
  ): Unit = {
    @tailrec
    def attemptUpdate(): Unit = {
      val currentNodes = nodes.get()
      currentNodes.get(url) match {
        case Some(node) =>
          val updatedNode  = update(node)
          val updatedNodes = currentNodes.updated(url, updatedNode)

          if (!nodes.compareAndSet(currentNodes, updatedNodes)) {
            logger.info(
              "Failed to update due to concurrent modification, retrying..."
            )
            attemptUpdate()
          } else {
            logger.info(s"Node updated successfully: $updatedNode")
          }

        case None =>
          logger.warn(s"No maybeNode found with URL $url to update")
      }
    }

    attemptUpdate()
  }
}
