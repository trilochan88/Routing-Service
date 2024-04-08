package com.ts
package domain.service

import common.enums.HealthStatus.NotHealthy
import common.enums.{HealthStatus, SlownessStatus}
import domain.model.{Node}

import org.mockito.Mockito
import org.mockito.Mockito.{atLeastOnce, never, verify}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar


class NodeManagerTest extends AnyFlatSpec with Matchers with MockitoSugar {
  val url1 = "test1"
  val url2 = "test2"
  val initialNodes = Seq(Node(url1), Node(url2))

  "NodeManager" should "attach subscribers correctly" in {
    val manager = new NodeManager(Seq(Node("http://localhost:8080")))
    val subscriber = mock[NodeStatusSubscriber]
    manager.attach(subscriber)

    manager.subscribers should contain(subscriber)
  }

  it should "detach subscribers correctly" in {
    val manager = new NodeManager(Seq(Node("http://localhost:8080")))
    val subscriber = mock[NodeStatusSubscriber]
    manager.attach(subscriber)
    manager.detach(subscriber)

    manager.subscribers should not contain subscriber
  }

  it should "not update health status if node does not exist" in {
    val manager = new NodeManager(Seq(Node("http://localhost:8080")))
    val subscriber = mock[NodeStatusSubscriber]
    manager.attach(subscriber)

    manager.updateNodeHealth("http://localhost:8084", HealthStatus.NotHealthy)
    verify(subscriber, never()).updateHealth(Some(Node("http://localhost:8084")), HealthStatus.NotHealthy)
  }

  it should "handle concurrent updates safely" in {
    val manager = new NodeManager(Seq(Node("http://localhost:8080")))
    val subscriber = mock[NodeStatusSubscriber]
    manager.attach(subscriber)

    // Simulate concurrent updates
    val threads = (1 to 10).map(_ => new Thread(() => {
      manager.updateNodeHealth("http://localhost:8080", HealthStatus.NotHealthy)
    }))
    threads.foreach(_.start())
    threads.foreach(_.join())

    // Verifying final state consistency
    manager.getNodes.head.healthStatus should be(HealthStatus.NotHealthy)
    verify(subscriber, atLeastOnce()).updateHealth(Some(Node("http://localhost:8080", HealthStatus.NotHealthy)), HealthStatus.NotHealthy)
  }

  it should "not update slowness status if node URL does not match" in {
    val manager = new NodeManager(Seq(Node("http://localhost:8080")))
    manager.updateNodeSlowness("http://localhost:8084", SlownessStatus.Slow)

    manager.getNodes.head.slownessStatus should be(SlownessStatus.Normal)
  }
}
