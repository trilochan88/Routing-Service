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
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{EitherValues, OptionValues}
import org.scalatestplus.mockito.MockitoSugar

class RoutingServiceSpec
    extends AnyFlatSpec
    with Matchers
    with MockitoSugar
      with OptionValues
    with EitherValues {
  "RoutingService" should "return the next maybeNode if one is available" in {
    val mockRoutingStrategy = mock[RoutingStrategy]
    val healthyNode = Node("http://localhost:8080", HealthStatus.Healthy, Normal)
    val nodes       = Seq(healthyNode)

    when(mockRoutingStrategy.selectNextNode(any[Seq[Node]]()))
      .thenReturn(Some(healthyNode))

    val routingService = new RoutingService(mockRoutingStrategy, nodes)
    val result         = routingService.getNextNode

    result shouldEqual Right(healthyNode)
  }

  it should "return NoHealthyNodeException when no healthy maybeNode is available" in {
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

 it should "fail to update the health status when the maybeNode does not exist" in {
   val initialNode = Node("localhost:0000",Healthy,Normal)
   val nonExistentNode = Node("localhost:9001",Healthy,Normal)
   val service = new RoutingService(mock[RoutingStrategy], Seq(initialNode))

   service.updateHealth(Some(nonExistentNode), NotHealthy)
   service.nodes.get().values.toSeq should contain only initialNode
 }
  it should "fail to update the slowness status when the maybeNode does not exist" in {
    val initialNode = Node("localhost:9001", Healthy, Normal)
    val nonExistentNode = Node("localhost:0000", Healthy, Normal)
    val service = new RoutingService(mock[RoutingStrategy], Seq(initialNode))

    service.updateSlowness(Some(nonExistentNode), Slow)
    service.nodes.get().values.toSeq should contain only initialNode
  }

  it should "handle concurrent update without data races" in {
    val nodes = Seq(Node("test1",NotHealthy),Node("test2",NotHealthy))
    val service = new RoutingService(mock[RoutingStrategy],nodes)

    val updates = (1 to 100).map{
      _ ⇒ service.updateHealth(nodes.headOption, Healthy)
    }
    service.nodes.get().get("test1").count(_.healthStatus == Healthy) should be (1)
  }
}
