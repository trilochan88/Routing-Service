package com.ts
package application.service

import common.enums.HealthStatus
import common.enums.HealthStatus.{Healthy, NotHealthy}
import common.enums.SlownessStatus.{Normal, Slow}
import common.exceptions.NoHealthyNodeException
import domain.model.Node
import domain.service.{NodeManager, RoutingStrategy}

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
    val nodeManager = new NodeManager(nodes)
    when(mockRoutingStrategy.selectNextNode(any[Seq[Node]]()))
      .thenReturn(Some(healthyNode))

    val routingService = new RoutingService(mockRoutingStrategy, nodeManager)
    val result         = routingService.getNextNode

    result shouldEqual Right(healthyNode)
  }

  it should "return NoHealthyNodeException when no healthy maybeNode is available" in {
    val mockRoutingStrategy = mock[RoutingStrategy]
    val nodes =
      Seq(
        Node("http://localhost:8081", HealthStatus.NotHealthy,Slow)
      )
    val nodeManager = new NodeManager(nodes)
    when(mockRoutingStrategy.selectNextNode(any[Seq[Node]])).thenReturn(None)

    val routingService = new RoutingService(mockRoutingStrategy, nodeManager)
    val result         = routingService.getNextNode

    result.left.value shouldBe a[NoHealthyNodeException]
  }
  
}
