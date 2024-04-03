package com.ts
package application.controller

import application.service.RoutingService
import domain.service.RequestHandler

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpMethods, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, extractRequest, extractRequestEntity, onComplete}
import akka.http.scaladsl.server.Route

import scala.concurrent.ExecutionContext

class RoutingController(routingService: RoutingService, requestHandler: RequestHandler)(implicit val system: ActorSystem, executionContext: ExecutionContext) {
  def routes: Route = {
    extractRequest {
      request =>
        request.method match {
          case HttpMethods.GET => complete(StatusCodes.MethodNotAllowed)
          case HttpMethods.POST => extractRequestEntity {
            entity => {
              routingService.getNextServer match
                case Left(ex) => complete(StatusCodes.BadGateway, ex.getMessage)
                case Right(node) => {
                  onComplete(requestHandler.handle(request, entity, node)) {
                    case util.Success(response) => complete(response)
                    case util.Failure(ex) => complete(StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}")
                  }
                }

            }
          }
          case _ => complete(StatusCodes.MethodNotAllowed)
        }
    }
  }
}