package com.ts
package application.controller

import application.service.RoutingService
import common.exceptions.NoHealthyNodeException
import domain.service.RequestHandler
import infrastructure.adapter.HttpService

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

class RoutingControllerSpec
    extends AnyWordSpec
    with Matchers
    with ScalatestRouteTest
    with MockitoSugar {

  "RoutingController" should {
    "reject non-POST requests with MethodNotAllowed" in {
      val mockRoutingService                = mock[RoutingService]
      val requestHandler                    = mock[RequestHandler]
      implicit val httpService: HttpService = mock[HttpService]
      val controller = new RoutingController(mockRoutingService, requestHandler)

      Get("/api") ~> controller.routes ~> check {
        status shouldBe StatusCodes.MethodNotAllowed
      }
    }

    "respond with BadGateway when no healthy nodes are available" in {
      val mockRoutingService                = mock[RoutingService]
      implicit val httpService: HttpService = mock[HttpService]
      val requestHandler                    = mock[RequestHandler]
      when(mockRoutingService.getNextNode).thenReturn(
        Left(new NoHealthyNodeException("No healthy nodes available"))
      )
      val controller = new RoutingController(mockRoutingService, requestHandler)

      Post(
        "/api",
        HttpEntity(ContentTypes.`application/json`, """{"data":"test"}""")
      ) ~> controller.routes ~> check {
        status shouldBe StatusCodes.ServiceUnavailable
        responseAs[String] should include("No healthy nodes available")
      }
    }
  }
}
