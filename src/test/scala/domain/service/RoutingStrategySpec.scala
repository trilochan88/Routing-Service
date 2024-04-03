package com.ts
package domain.service

import common.enums.{HealthStatus, SlownessStatus}
import domain.model.Node

import org.mockito.Mockito.when
import org.scalatest.OptionValues
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

class RoutingStrategySpec
    extends AsyncFlatSpec
    with Matchers
      with OptionValues
    with MockitoSugar {
  "RoundRobinStrategy" should "select nodes in a round-robin manner when all nodes are healthy" in {
    val nodes = Seq(
      Node("Node1", HealthStatus.Healthy, SlownessStatus.Normal),
      Node("Node2", HealthStatus.Healthy, SlownessStatus.Normal),
      Node("Node3", HealthStatus.Healthy, SlownessStatus.Normal)
    )

    val strategy = new RoundRobinStrategy()

    strategy.selectNextNode(nodes).value shouldEqual nodes(0)
    strategy.selectNextNode(nodes).value shouldEqual nodes(1)
    strategy.selectNextNode(nodes).value shouldEqual nodes(2)
  }

it should "skip unhealthy and slow nodes" in {
  val nodes = Seq(
    Node("Node1", HealthStatus.NotHealthy, SlownessStatus.Normal),
    Node("Node2", HealthStatus.Healthy, SlownessStatus.Slow),
    Node("Node3", HealthStatus.Healthy, SlownessStatus.Normal)
  )

  val strategy = new RoundRobinStrategy()
  strategy.selectNextNode(nodes).value shouldEqual nodes(2)
  strategy.selectNextNode(nodes).value shouldEqual nodes(2)
}
  it should "return None when no nodes are healthy" in {
    val nodes = Seq(
      Node("Node1", HealthStatus.NotHealthy, SlownessStatus.Normal),
      Node("Node2", HealthStatus.NotHealthy, SlownessStatus.Normal)
    )
    val strategy = new RoundRobinStrategy()

    strategy.selectNextNode(nodes) shouldBe None
  }

  it should "return None when all nodes are slow" in {
    val nodes = Seq(
      Node("Node1", HealthStatus.Healthy, SlownessStatus.Slow),
      Node("Node2", HealthStatus.Healthy, SlownessStatus.Slow)
    )
    val strategy = new RoundRobinStrategy()

    strategy.selectNextNode(nodes) shouldBe None
  }


}
