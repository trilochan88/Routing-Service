package com.ts
package domain.service

import domain.model.Node
import infrastructure.adapter.HttpService

import akka.actor.ActorSystem
import akka.http.scaladsl.model.*

import scala.concurrent.Future

trait RequestHandler {
  def handle(
    request: HttpRequest,
    entity: RequestEntity,
    node: Node
  ): Future[HttpResponse]
}

/** Post Request Handler - Creating and routing to valid node
  * @param httpService
  * @param system
  */
class PostRequestHandler(httpService: HttpService)(implicit
  val system: ActorSystem
) extends RequestHandler {
  override def handle(
    request: HttpRequest,
    entity: RequestEntity,
    node: Node
  ): Future[HttpResponse] = {
    val forwardUri = Uri(node.url).withPath(Uri.Path(request.getUri.path()))
    httpService.sendRequest(
      node,
      HttpRequest(
        method = HttpMethods.POST,
        uri = forwardUri,
        entity = entity,
        headers = request.headers,
        protocol = request.protocol
      )
    )
  }
}
