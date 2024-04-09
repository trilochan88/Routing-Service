package com.ts
package domain.service

import common.enums.HealthStatus.NotHealthy
import common.enums.{HealthStatus, SlownessStatus}
import domain.model.{Node, NodeStatusSubscriber}

import org.scalatest.{AsyncTestSuite, OptionValues}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class NodeManagerTest extends AnyFlatSpec with Matchers with MockitoSugar with OptionValues {
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
    manager.getNodes().find(_.url == url1).get.healthStatus shouldEqual  HealthStatus.NotHealthy
  }

  it should "not update health status if URL does not match any maybeNode" in {
    val manager = new NodeManager(initialNodes)
    val subscriber = mock[NodeStatusSubscriber]
    manager.attach(subscriber)

    manager.updateHealth("test3", HealthStatus.NotHealthy)
    manager.getNodes().foreach(_.healthStatus shouldEqual HealthStatus.Healthy)
  }

  it should "update slowness status correctly and notify subscribers" in {
    val manager = new NodeManager(initialNodes)
    val subscriber = mock[NodeStatusSubscriber]
    manager.attach(subscriber)

    manager.updateSlowness(url2, SlownessStatus.Slow)
    manager.getNodes().find(_.url == url2).get.slownessStatus shouldEqual SlownessStatus.Slow
  }

  it should "not update slowness status if URL does not match any maybeNode" in {
    val manager = new NodeManager(initialNodes)
    val subscriber = mock[NodeStatusSubscriber]
    manager.attach(subscriber)

    manager.updateSlowness("test", SlownessStatus.Slow)
    manager.getNodes().foreach(_.slownessStatus shouldEqual SlownessStatus.Normal)
  }

  it should "Concurrent updates to node health should be handled correctly" in {
    val initialNodes = Seq(Node("test1"), Node("test2"))
    val nodeManager = new NodeManager(initialNodes)
    val updates = (1 to 1000).map { _ =>
      Future {
        nodeManager.updateHealth("test1", HealthStatus.NotHealthy)
        nodeManager.updateHealth("test1", HealthStatus.Healthy)
      }
    }
    
    val results = Future.sequence(updates)
    Await.result(results,Duration.Inf)
    
   val updatedNode =  nodeManager.getNodes().find(_.url == "test1").value 
    updatedNode.healthStatus should (be(HealthStatus.Healthy) or be(HealthStatus.NotHealthy))
  }

}
