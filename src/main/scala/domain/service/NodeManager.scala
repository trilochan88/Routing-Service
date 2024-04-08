package com.ts
package domain.service

import common.enums.{HealthStatus, SlownessStatus}
import domain.model.Node

import org.slf4j.LoggerFactory

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock

trait NodeStatusSubscriber {
  def updateHealth(node: Option[Node], healthStatus: HealthStatus): Unit

  def updateSlowness(node: Option[Node], slownessStatus: SlownessStatus): Unit
}

class NodeManager(initialNodes: Seq[Node]) {
  private val logger = LoggerFactory.getLogger(getClass)
  private val lock   = new ReentrantLock()
  private val nodes: AtomicReference[Map[String, Node]] = new AtomicReference(
    initialNodes.map(node => node.url -> node).toMap
  )
  private[service] var subscribers: List[NodeStatusSubscriber] = List()

  def getNodes: Seq[Node] = nodes.get().values.toSeq

  def attach(subscriber: NodeStatusSubscriber): Unit = {
    lock.lock()
    try {
      subscribers = subscriber :: subscribers
    } finally {
      lock.unlock()
    }
  }

  def detach(subscriber: NodeStatusSubscriber): Unit = {
    lock.lock()
    try {
      subscribers = subscribers.filterNot(_ == subscriber)
    } finally {
      lock.unlock()
    }
  }
  def updateNodeHealth(url: String, healthStatus: HealthStatus): Unit = {
    logger.info(s"Attempting to update node health $url: $healthStatus")
    updateNodeConditionally(url, _.copy(healthStatus = healthStatus))
    logger.info(s"Updated node health ${nodes.get().get(url)}")
  }

  def updateNodeSlowness(url: String, slownessStatus: SlownessStatus): Unit = {
    logger.info(s"Attempting to update node slowness $url: $slownessStatus")
    updateNodeConditionally(url, _.copy(slownessStatus = slownessStatus))
    logger.info(s"Updated node slowness ${nodes.get().get(url)}")
  }

  private def updateNodeConditionally(
    url: String,
    update: Node => Node
  ): Unit = {
    val currentNodes = nodes.get()
    currentNodes.get(url) match {
      case Some(node) =>
        val updatedNode  = update(node)
        val updatedNodes = currentNodes.updated(url, updatedNode)
        if (!nodes.compareAndSet(currentNodes, updatedNodes)) {
          updateNodeConditionally(url, update)
        } else {
          subscribers.foreach(_.updateHealth(Some(node), node.healthStatus))
        }

      case None => logger.warn(s"No node found with URL $url to update")
    }
  }

  /*
  private[service] var nodes = initialNodes
  private[service] var subscribers: List[NodeStatusSubscriber] = List()

  def attach(subscriber: NodeStatusSubscriber): Unit = {
    lock.lock()
    try {
      subscribers = subscriber :: subscribers
    } finally {
      lock.unlock()
    }
  }

  def detach(subscriber: NodeStatusSubscriber): Unit = {
    lock.lock()
    try {
      subscribers = subscribers.filterNot(_ == subscriber)
    } finally {
      lock.unlock()
    }
  }

  def updateHealth(url: String, status: HealthStatus): Unit = {
    lock.lock()
    try {
      nodes = nodes.map(node => if (node.url == url) node.copy(healthStatus = status) else node)
      subscribers.foreach(_.updateHealth(nodes.find(_.url == url), status))
    } finally {
      lock.unlock()
    }
  }

  def updateSlowness(url: String, status: SlownessStatus): Unit = {
    lock.lock()
    try {
      nodes = nodes.map(node => if (node.url == url) node.copy(slownessStatus = status) else node)
      subscribers.foreach(_.updateSlowness(nodes.find(_.url == url), status))
    } finally {
      lock.unlock()
    }
  }*/
}
