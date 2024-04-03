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
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import scala.concurrent.duration._
import scala.concurrent.Await

class RoutingServiceSpec
    extends AnyFlatSpec
    with Matchers
    with MockitoSugar
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
}
