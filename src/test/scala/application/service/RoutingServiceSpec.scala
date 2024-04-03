package com.ts
package application.service

import common.exceptions.NoHealthyNodeException
import domain.model.Node
import domain.service.RoutingStrategy

import com.ts.common.enums.HealthStatus
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

class RoutingServiceSpec extends AnyFlatSpec with Matchers with MockitoSugar {
  "RoutingService" should "return a healthy server when available" in {
    val mockRoutingStrategy = mock[RoutingStrategy]
    val healthyNode = Node("http://localhost:8080", HealthStatus.Healthy)
    val servers = Seq(healthyNode)

    when(mockRoutingStrategy.selectServer(any[Seq[Node]]())).thenReturn(Some(healthyNode))

    val routingService = new RoutingService(mockRoutingStrategy, servers)
    val result = routingService.getNextServer

    result shouldEqual Right(healthyNode)
  }

  it should "throw NoHealthyNodeException when no healthy nodes are available" in {
    val mockRoutingStrategy = mock[RoutingStrategy]
    val servers = Seq(Node("http://localhost:8081", HealthStatus.NotHealthy)) // All nodes are unhealthy

    when(mockRoutingStrategy.selectServer(any[Seq[Node]])).thenReturn(None)

    val routingService = new RoutingService(mockRoutingStrategy, servers)
    val result = routingService.getNextServer

    result shouldBe a[Left[_, _]]
    result.left.get shouldBe a[NoHealthyNodeException]
  }

  it should "updateHealth server health status based on notifications" in {
    val server1 = Node("http://localhost:8081", HealthStatus.Healthy)
    val server2 = Node("http://localhost:8082", HealthStatus.NotHealthy)
    val servers = Seq(server1, server2)
    val mockRoutingStrategy = mock[RoutingStrategy]

    val routingService = new RoutingService(mockRoutingStrategy, servers)

    server1.getHealthStatus() shouldBe HealthStatus.Healthy
    server2.getHealthStatus() shouldBe HealthStatus.NotHealthy

    routingService.updateHealth(server2, HealthStatus.Healthy)
    server2.getHealthStatus() shouldBe HealthStatus.Healthy
  }
}
