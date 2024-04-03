package com.ts
package domain.model

import common.enums.{HealthStatus, SlownessStatus}

import com.ts.common.enums.HealthStatus.NotHealthy
import org.mockito.Mockito.{times, verify}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MockSubscriber extends NodeStatusSubscriber{
  var healthUpdates: List[(Node, HealthStatus)] = List.empty
  var slownessUpdates: List[(Node, SlownessStatus)] = List.empty

  override def updateHealth(node: Node, healthStatus: HealthStatus): Future[Unit] = {
    healthUpdates = (node, healthStatus) :: healthUpdates
    Future.successful(())
  }

  override def updateSlowness(node: Node, slownessStatus: SlownessStatus): Future[Unit] = {
    slownessUpdates = (node, slownessStatus) :: slownessUpdates
    Future.successful(())
  }
}

class NodeSpec extends AnyFlatSpec with Matchers with MockitoSugar {

  "Node" should "correctly updateHealth its health status and notify subscribers" in {
    val subscriber = new MockSubscriber()
    val node = Node("http://example.com").attach(subscriber)
    node.setHealth(HealthStatus.NotHealthy).map { updatedNode =>
      updatedNode.healthStatus shouldEqual HealthStatus.NotHealthy
      subscriber.healthUpdates should have size 1
      subscriber.healthUpdates.head._2 shouldEqual HealthStatus.NotHealthy
    }
  }

  it should "allow detaching a subscriber and not receive updates" in {
    val subscriber = new MockSubscriber()
    val node = Node("node1").attach(subscriber).detach(subscriber)
    node.setHealth(HealthStatus.NotHealthy).map { updatedNode =>
      subscriber.healthUpdates shouldBe empty
    }
  }

  it should "update slowness status and notify the subscriber" in {
    val subscriber = new MockSubscriber()
    val node = Node("Node1").attach(subscriber)
    val updatedNode = node.copy(slownessStatus = SlownessStatus.Slow)
    Future.sequence(node.subscribers.map(_.updateSlowness(updatedNode, SlownessStatus.Slow))).map { _ =>
      subscriber.slownessUpdates should have size 1
      subscriber.slownessUpdates.head._2 shouldEqual SlownessStatus.Slow
    }
  }

  it should "handle no subscribers without errors when updating statuses" in {
    val node = Node("Node1")
    node.setHealth(HealthStatus.NotHealthy).flatMap { updatedNode =>
      updatedNode.setSlowStatus(SlownessStatus.Slow)
      Future.successful(succeed)
    }
  }
}
