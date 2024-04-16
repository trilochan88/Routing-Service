package com.ts
package domain.service

import common.enums.{HealthStatus, SlownessStatus}
import domain.model.{Node, NodeStatusSubscriber}

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.{ReentrantLock, ReentrantReadWriteLock, StampedLock}
import scala.jdk.CollectionConverters.*

class NodeManager(initialNodes: Seq[Node]) {
  private val lock = new StampedLock();
  private val nodeMap: ConcurrentHashMap[String, Node] =
    new ConcurrentHashMap[String, Node]()
  private[service] var subscribers: List[NodeStatusSubscriber] = List()
  initialNodes.foreach(node ⇒ nodeMap.putIfAbsent(node.url, node))
  def attach(subscriber: NodeStatusSubscriber): Unit = {
    val stamp = lock.writeLock()
    try {
      subscribers = subscriber :: subscribers
    } finally {
      lock.unlockWrite(stamp)
    }
  }

  def getNodes(): Seq[Node] = {
    var stamp    = lock.tryOptimisticRead()
    var nodeList = nodeMap.values().asScala.toList.map(_.copy())
    if (!lock.validate(stamp)) {
      stamp = lock.readLock()
      try {
        nodeList = nodeMap.values().asScala.toList.map(_.copy())
      } finally {
        lock.unlockRead(stamp)
      }
    }
    nodeList
  }

  def detach(subscriber: NodeStatusSubscriber): Unit = {
    val stamp = lock.writeLock()
    try {
      subscribers = subscribers.filterNot(_ == subscriber)
    } finally {
      lock.unlockWrite(stamp)
    }
  }

  def updateHealth(url: String, status: HealthStatus): Unit = {
    val stamp = lock.writeLock()
    try {
      nodeMap.computeIfPresent(
        url,
        (_, current) ⇒ current.copy(healthStatus = status)
      )
      subscribers.foreach(_.updateHealth(Option(nodeMap.get(url)), status))
    } finally {
      lock.unlockWrite(stamp)
    }
  }

  def updateSlowness(url: String, status: SlownessStatus): Unit = {
    val stamp = lock.writeLock()
    try {
      nodeMap.computeIfPresent(
        url,
        (_, current) ⇒ current.copy(slownessStatus = status)
      )
      subscribers.foreach(_.updateSlowness(Option(nodeMap.get(url)), status))
    } finally {
      lock.unlockWrite(stamp)
    }
  }

}
