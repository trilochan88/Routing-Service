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
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RoutingService(routingStrategy: RoutingStrategy, initialNodes: Seq[Node])
    extends NodeStatusSubscriber {
  private val logger = LoggerFactory.getLogger(getClass)
  private[application] val nodes: AtomicReference[Seq[Node]] =
    new AtomicReference[immutable.Seq[Node]](initialNodes.toVector)
  def getNextNode: Either[NoHealthyNodeException, Node] = {
    val currentNodes = nodes.get()
    routingStrategy.selectNextNode(currentNodes) match
      case Some(node) => Right(node)
      case None =>
        logger.error(
          s"No health nodes available from the list of size ${currentNodes.length}"
        )
        Left(
          new NoHealthyNodeException(
            "No healthy nodes available to handle this request"
          )
        )
  }

  override def updateHealth(
    node: Node,
    healthStatus: HealthStatus
  ): Future[Unit] =
    Future {
      updateNodeConditionally(
        nodes,
        _.url == node.url,
        _.copy(healthStatus = healthStatus)
      )
    }.recover { case e: Exception ⇒
      logger.error("Failed to update node status for health")
    }

  override def updateSlowness(
    node: Node,
    slownessStatus: SlownessStatus
  ): Future[Unit] = Future {
    updateNodeConditionally(
      nodes,
      _.url == node.url,
      _.copy(slownessStatus = slownessStatus)
    )
  }.recover { case e: Exception ⇒
    logger.error("Failed to update node status for slowness")
  }

  private def updateNodeConditionally(
    nodes: AtomicReference[Seq[Node]],
    predicate: Node ⇒ Boolean,
    update: Node ⇒ Node
  ) = {
    @tailrec
    def attemptUpdate(): Unit = {
      val currentNodes = nodes.get()
      val updateNodes =
        currentNodes.map(node ⇒ if (predicate(node)) update(node) else node)
      if (!nodes.compareAndSet(currentNodes, updateNodes)) attemptUpdate()
    }
    attemptUpdate()
  }
}
