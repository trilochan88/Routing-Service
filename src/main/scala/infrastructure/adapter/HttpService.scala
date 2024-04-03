package com.ts
package infrastructure.adapter

import common.exceptions.ExternalHttpFailedException
import domain.model.Node

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

trait HttpService {
  def sendRequest(node: Node, request: HttpRequest): Future[HttpResponse]
}

class AkkaHttpService(circuitBreakerManager: CircuitBreakerManager)(implicit
  val system: ActorSystem,
  executionContext: ExecutionContext
) extends HttpService {

  private val logger = LoggerFactory.getLogger(getClass)
  override def sendRequest(
    node: Node,
    request: HttpRequest
  ): Future[HttpResponse] = {
    val circuitBreaker = circuitBreakerManager.getBreakerForNode(node)
    if (IsNotHealthyEndpoint(request)) {
      logger.info(s"routing request to ${request.getUri} HealthStatus = ${node
          .healthStatus} slowness = ${node.slownessStatus}")
    }
    val responseFuture =
      circuitBreaker.withCircuitBreaker(
        Http()(system.classicSystem).singleRequest(request)
      )
    responseFuture
      .recoverWith { case ex =>
        logger.error(s"Endpoint failed with ${ex.getMessage}")
        Future.failed(
          new ExternalHttpFailedException(s"Request failed for ${node.url}", ex)
        )
      }
  }

  private def IsNotHealthyEndpoint(request: HttpRequest) = {
    !request.getUri.path().contains("health")
  }
}
