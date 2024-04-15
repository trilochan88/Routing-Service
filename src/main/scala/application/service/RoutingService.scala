package com.ts
package application.service

import common.exceptions.NoHealthyNodeException
import domain.model.Node
import domain.service.{NodeManager, RoutingStrategy}

import org.slf4j.LoggerFactory

class RoutingService(
  routingStrategy: RoutingStrategy,
  nodeManager: NodeManager
) {
  private val logger = LoggerFactory.getLogger(getClass)
  def getNextNode: Either[NoHealthyNodeException, Node] = {
    val currentNodes = nodeManager.getNodes()
    logger.info(s"Current nodes = ${currentNodes.toString()}")
    routingStrategy.selectNextNode(currentNodes) match
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
}
