package com.ts
package domain.service

import common.enums.{HealthStatus, SlownessStatus}
import domain.model.{Node, NodeStatusSubscriber}

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import scala.jdk.CollectionConverters.*

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
    lock.lock();
    try {
      nodeMap.values().asScala.toList.map(_.copy())
    } finally {
      lock.unlock();
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
    lock.lock();
    try {
      nodeMap.computeIfPresent(
        url,
        (_, current) ⇒ current.copy(healthStatus = status)
      )
      subscribers.foreach(_.updateHealth(Option(nodeMap.get(url)), status))
    } finally {
      lock.unlock();
    }
  }

  def updateSlowness(url: String, status: SlownessStatus): Unit = {
    lock.lock();
    try {
      nodeMap.computeIfPresent(
        url,
        (_, current) ⇒ current.copy(slownessStatus = status)
      )
      subscribers.foreach(_.updateSlowness(Option(nodeMap.get(url)), status))
    } finally {
      lock.unlock();
    }
  }

}
