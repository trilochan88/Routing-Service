package com.ts
package domain.service

import common.enums.{HealthStatus, SlownessStatus}
import domain.model.{Node, NodeStatusSubscriber}

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.{ReentrantLock, ReentrantReadWriteLock}
import scala.jdk.CollectionConverters.*

class NodeManager(initialNodes: Seq[Node]) {
  private val rwLock = new ReentrantReadWriteLock();
  private val readLock = rwLock.readLock()
  private val writeLock = rwLock.writeLock()
  private val nodeMap: ConcurrentHashMap[String, Node] =
    new ConcurrentHashMap[String, Node]()
  private[service] var subscribers: List[NodeStatusSubscriber] = List()
  initialNodes.foreach(node ⇒ nodeMap.putIfAbsent(node.url, node))
  def attach(subscriber: NodeStatusSubscriber): Unit = {
    writeLock.lock()
    try {
      subscribers = subscriber :: subscribers
    } finally {
      writeLock.unlock()
    }
  }

  def getNodes(): Seq[Node] = {
    readLock.lock();
    try {
      nodeMap.values().asScala.toList.map(_.copy())
    } finally {
      readLock.unlock();
    }
  }

  def detach(subscriber: NodeStatusSubscriber): Unit = {
    writeLock.lock()
    try {
      subscribers = subscribers.filterNot(_ == subscriber)
    } finally {
      writeLock.unlock()
    }
  }

  def updateHealth(url: String, status: HealthStatus): Unit = {
    writeLock.lock();
    try {
      nodeMap.computeIfPresent(
        url,
        (_, current) ⇒ current.copy(healthStatus = status)
      )
      subscribers.foreach(_.updateHealth(Option(nodeMap.get(url)), status))
    } finally {
      writeLock.unlock();
    }
  }

  def updateSlowness(url: String, status: SlownessStatus): Unit = {
    writeLock.lock();
    try {
      nodeMap.computeIfPresent(
        url,
        (_, current) ⇒ current.copy(slownessStatus = status)
      )
      subscribers.foreach(_.updateSlowness(Option(nodeMap.get(url)), status))
    } finally {
      writeLock.unlock();
    }
  }

}
