package com.ts
package domain.service

import domain.model.Node

import com.ts.common.enums.HealthStatus
import org.mockito.Mockito.when
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

class RoutingStrategySpec extends AsyncFlatSpec with Matchers with MockitoSugar {
  "RoundRobinStrategy" should "cycle through healthyServers in a round-robin manner" in {
    val nodes = Seq(
      mock[Node],
      mock[Node],
      mock[Node]
    )
    when(nodes(0).getHealthStatus()).thenReturn(HealthStatus.Healthy)
    when(nodes(1).getHealthStatus()).thenReturn(HealthStatus.NotHealthy)
    when(nodes(2).getHealthStatus()).thenReturn(HealthStatus.Healthy)

    val strategy = new RoundRobinStrategy()

    strategy.selectServer(nodes) shouldEqual Some(nodes(0))
    strategy.selectServer(nodes) shouldEqual Some(nodes(2))
    strategy.selectServer(nodes) shouldEqual Some(nodes(0))
  }

  it should "return None if there are no healthy nodes" in {
    val unhealthyServers = Seq(mock[Node])
    when(unhealthyServers.head.getHealthStatus()).thenReturn(HealthStatus.NotHealthy)

    val strategy = new RoundRobinStrategy()

    strategy.selectServer(unhealthyServers) shouldEqual None
  }
}
