package com.ts
package infrastructure.adapter

import domain.model.Node
import infrastructure.config.HealthConfig

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.testkit.{ImplicitSender, TestKit}
import com.ts.common.enums.HealthStatus
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterAll, Inspectors}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock

import scala.concurrent.Future
import scala.concurrent.duration.*

class HealthServiceSpec
    extends TestKit(ActorSystem("HealthServiceSpec"))
    with ImplicitSender
    with Inspectors
    with AnyWordSpecLike
    with BeforeAndAfterAll
    with MockitoSugar
    with Matchers {

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  "Health Service" should {
    "mark server as healthy if health check returns 200 OK" in {
      //arrange
      val server = new Node("http://localhost:8081")
      val mockHttpService = mock[HttpService]
      val mockHttpConfig = mock[HealthConfig]
      when(mockHttpConfig.interval).thenReturn(1.seconds)
      val responseFuture = Future.successful(HttpResponse(StatusCodes.OK))
      when(mockHttpService.sendRequest(any[Node](), any[HttpRequest]())).thenReturn(responseFuture)
      server.setHealth(HealthStatus.NotHealthy)
      val servers = List(server)
      val healthService = new HealthService(mockHttpConfig, mockHttpService, servers)(system)
      healthService.checkHealth(server)
      Thread.sleep(200)
      server.getHealthStatus() shouldBe (HealthStatus.NotHealthy)
    }
    "mark unhealthy if health check respond other than 200 Ok " in {
      val node = new Node("http://localhost:8081")
      val mockHttpService = mock[HttpService]
      val mockHttpConfig = mock[HealthConfig]
      when(mockHttpConfig.interval).thenReturn(1.seconds)
      val responseFuture = Future.successful(HttpResponse(StatusCodes.InternalServerError))
      when(mockHttpService.sendRequest(any[Node](), any[HttpRequest]())).thenReturn(responseFuture)
      val servers = List(node)
      val healthService = new HealthService(mockHttpConfig, mockHttpService, servers)(system)
      healthService.checkHealth(node)
      Thread.sleep(200)
      node.getHealthStatus() shouldBe (HealthStatus.Healthy)
    }
  }

}
