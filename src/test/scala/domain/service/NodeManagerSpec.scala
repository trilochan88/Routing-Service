package com.ts
package domain.service

import common.enums.HealthStatus.NotHealthy
import common.enums.{HealthStatus, SlownessStatus}
import domain.model.{Node, NodeStatusSubscriber}

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{never, verify}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar


class NodeManagerTest extends AnyFlatSpec with Matchers with MockitoSugar {
  val url1 = "test1"
  val url2 = "test2"
  val initialNodes = Seq(Node(url1), Node(url2))

  "NodeManager" should "correctly attach a subscriber" in {
    val manager = new NodeManager(initialNodes)
    val subscriber = mock[NodeStatusSubscriber]

    manager.attach(subscriber)
    manager.subscribers should contain(subscriber)
  }

  it should "correctly detach a subscriber" in {
    val manager = new NodeManager(initialNodes)
    val subscriber = mock[NodeStatusSubscriber]
    manager.attach(subscriber)

    manager.detach(subscriber)
    manager.subscribers should not contain subscriber
  }

  it should "update health status correctly and notify subscribers" in {
    val manager = new NodeManager(initialNodes)
    val subscriber = mock[NodeStatusSubscriber]
    manager.attach(subscriber)

    manager.updateHealth(url1, HealthStatus.NotHealthy)
    manager.nodes.find(_.url == url1).get.healthStatus shouldEqual  HealthStatus.NotHealthy
  }

  it should "not update health status if URL does not match any maybeNode" in {
    val manager = new NodeManager(initialNodes)
    val subscriber = mock[NodeStatusSubscriber]
    manager.attach(subscriber)

    manager.updateHealth("test3", HealthStatus.NotHealthy)
    manager.nodes.foreach(_.healthStatus shouldEqual HealthStatus.Healthy) // No change
    verify(subscriber, never()).updateHealth(any[Option[Node]], any[HealthStatus])
  }

  it should "update slowness status correctly and notify subscribers" in {
    val manager = new NodeManager(initialNodes)
    val subscriber = mock[NodeStatusSubscriber]
    manager.attach(subscriber)

    manager.updateSlowness(url2, SlownessStatus.Slow)
    manager.nodes.find(_.url == url2).get.slownessStatus shouldEqual SlownessStatus.Slow
  }

  it should "not update slowness status if URL does not match any maybeNode" in {
    val manager = new NodeManager(initialNodes)
    val subscriber = mock[NodeStatusSubscriber]
    manager.attach(subscriber)

    manager.updateSlowness("test", SlownessStatus.Slow)
    manager.nodes.foreach(_.slownessStatus shouldEqual SlownessStatus.Normal) // No change
    verify(subscriber, never()).updateSlowness(any[Option[Node]], any[SlownessStatus])
  }
}
