package com.ts
package domain.service

import common.enums.{HealthStatus, SlownessStatus}
import domain.model.{Node, NodeStatusSubscriber}

import java.util.concurrent.locks.ReentrantLock

class NodeManager(initialNodes: Seq[Node]) {
  private val lock = new ReentrantLock()
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
  }
}
