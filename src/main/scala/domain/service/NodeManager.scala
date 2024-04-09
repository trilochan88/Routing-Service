package com.ts
package domain.service

import common.enums.{HealthStatus, SlownessStatus}
import domain.model.{Node, NodeStatusSubscriber}

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import scala.jdk.CollectionConverters._

class NodeManager(initialNodes: Seq[Node]) {
  private val lock = new ReentrantLock()
  private val nodeMap: ConcurrentHashMap[String, Node] =
    new ConcurrentHashMap[String, Node]()
  private[service] var subscribers: List[NodeStatusSubscriber] = List()
  initialNodes.foreach(node ⇒ nodeMap.putIfAbsent(node.url, node))
  def attach(subscriber: NodeStatusSubscriber): Unit = {
    lock.lock()
    try {
      subscribers = subscriber :: subscribers
    } finally {
      lock.unlock()
    }
  }
  
  def getNodes(): Seq[Node] = {
    nodeMap.values().asScala.toSeq
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
    nodeMap.computeIfPresent(
      url,
      (_, current) ⇒ current.copy(healthStatus = status)
    )
    subscribers.foreach(_.updateHealth(Option(nodeMap.get(url)), status))
  }

  def updateSlowness(url: String, status: SlownessStatus): Unit = {
    nodeMap.computeIfPresent(
      url,
      (_, current) ⇒ current.copy(slownessStatus = status)
    )
    subscribers.foreach(_.updateSlowness(Option(nodeMap.get(url)), status))
  }

}
