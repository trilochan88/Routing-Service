package com.ts
package infrastructure.adapter

import domain.model.Node
import infrastructure.config.HealthConfig

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes, Uri}
import com.ts.common.enums.HealthStatus.{Healthy, NotHealthy}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration.*

class HealthService(healthConfig: HealthConfig, nodes: Seq[Node])(implicit
    system: ActorSystem
) {
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  import system.dispatcher

  def startChecking(): Unit = {
    system.scheduler.scheduleAtFixedRate(initialDelay = 0.seconds, healthConfig.interval) { () =>
      nodes.foreach(checkHealth)
    }
  }

  private[adapter] def checkHealth(node: Node): Unit = {
    val healthCheckUri = Uri(node.url).withPath(Uri.Path("/health"))
    Http()(system.classicSystem).singleRequest(HttpRequest(uri = healthCheckUri))
      .map {
        case HttpResponse(StatusCodes.OK, _, _, _) =>
          node.setHealth(Healthy)
        case _ =>
          logger.error(s"Node is not healthy ${node.url}")
          node.setHealth(NotHealthy)
      }
      .recover { case ex =>
        logger.error(s"Node ${node.url} is not healthy due to ${ex.getMessage}")
        node.setHealth(NotHealthy)
      }
  }
}
