package com.ts
package domain.model

import com.ts.common.enums.HealthStatus
import org.mockito.Mockito.{times, verify}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

class NodeSpec extends AnyFlatSpec with Matchers with MockitoSugar {

  "A Node" should "correctly updateHealth its health status and notify subscribers" in {
    val mockSubscriber = mock[NodeStatusSubscriber]
    val node = new Node("http://localhost:8080")
    node.attach(mockSubscriber)

    node.getHealthStatus() shouldBe HealthStatus.Healthy
    node.setHealth(HealthStatus.NotHealthy)

    node.getHealthStatus() shouldBe HealthStatus.NotHealthy

    verify(mockSubscriber, times(1)).updateHealth(node, HealthStatus.NotHealthy)
  }

  it should "allow attaching and detaching subscribers" in {
    val subscriber1 = mock[NodeStatusSubscriber]
    val subscriber2 = mock[NodeStatusSubscriber]
    val node = new Node("http://localhost:8080")

    node.attach(subscriber1)
    node.attach(subscriber2)

    node.setHealth(HealthStatus.NotHealthy)
    verify(subscriber1, times(1)).updateHealth(node, HealthStatus.NotHealthy)
    verify(subscriber2, times(1)).updateHealth(node, HealthStatus.NotHealthy)

    node.detach(subscriber1)
    node.setHealth(HealthStatus.Healthy)

    verify(subscriber1, times(1)).updateHealth(node, HealthStatus.NotHealthy)
    verify(subscriber2, times(1)).updateHealth(node, HealthStatus.Healthy)
  }
}
