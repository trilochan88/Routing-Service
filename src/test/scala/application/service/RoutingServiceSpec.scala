package com.ts
package application.service

import common.enums.HealthStatus
import common.enums.HealthStatus.{Healthy, NotHealthy}
import common.enums.SlownessStatus.{Normal, Slow}
import common.exceptions.NoHealthyNodeException
import domain.model.Node
import domain.service.RoutingStrategy

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.{EitherValues, OptionValues}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class RoutingServiceSpec
    extends AnyFlatSpec
    with Matchers
    with MockitoSugar
      with OptionValues
    with EitherValues {
  "RoutingService" should "return the next node if one is available" in {
    val mockRoutingStrategy = mock[RoutingStrategy]
    val healthyNode = Node("http://localhost:8080", HealthStatus.Healthy, Normal)
    val nodes       = Seq(healthyNode)

    when(mockRoutingStrategy.selectNextNode(any[Seq[Node]]()))
      .thenReturn(Some(healthyNode))

    val routingService = new RoutingService(mockRoutingStrategy, nodes)
    val result         = routingService.getNextNode

    result shouldEqual Right(healthyNode)
  }

  it should "return NoHealthyNodeException when no healthy node is available" in {
    val mockRoutingStrategy = mock[RoutingStrategy]
    val nodes =
      Seq(
        Node("http://localhost:8081", HealthStatus.NotHealthy,Slow)
      )

    when(mockRoutingStrategy.selectNextNode(any[Seq[Node]])).thenReturn(None)

    val routingService = new RoutingService(mockRoutingStrategy, nodes)
    val result         = routingService.getNextNode

    result.left.value shouldBe a[NoHealthyNodeException]
  }

 it should "fail to update the health status when the node does not exist" in {
   val initialNode = Node("localhost:0000",Healthy,Normal)
   val nonExistentNode = Node("localhost:9001",Healthy,Normal)
   val service = new RoutingService(mock[RoutingStrategy], Seq(initialNode))

   Await.result(service.updateHealth(nonExistentNode,NotHealthy),1.seconds)
   service.nodes.get() should contain only initialNode
 }
  it should "fail to update the slowness status when the node does not exist" in {
    val initialNode = Node("localhost:9001", Healthy, Normal)
    val nonExistentNode = Node("localhost:0000", Healthy, Normal)
    val service = new RoutingService(mock[RoutingStrategy], Seq(initialNode))

    Await.result(service.updateSlowness(nonExistentNode, Slow), 1.seconds)
    service.nodes.get() should contain only initialNode
  }

  it should "handle concurrent update without data races" in {
    val nodes = Seq(Node("test1",NotHealthy),Node("test2",NotHealthy))
    val service = new RoutingService(mock[RoutingStrategy],nodes)

    val updates = (1 to 100).map{
      _ ⇒ service.updateHealth(nodes.headOption.value, Healthy)
    }
    Await.result(Future.sequence(updates), 2.seconds)
    service.nodes.get().count(_.healthStatus == Healthy) should be (1)
  }
}
