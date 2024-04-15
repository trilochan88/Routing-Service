package com.ts
package infrastructure.adapter

import common.enums.HealthStatus.{Healthy, NotHealthy}
import domain.model.Node
import infrastructure.config.HealthConfig

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.settings.ConnectionPoolSettings
import com.ts.domain.service.NodeManager
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration.*

/**
 * Background job to check health of all api server periodically
 * @param healthConfig - have interval configuration 
 * @param nodes - list of configured servers
 * @param system
 */
class HealthService(healthConfig: HealthConfig, nodeManager: NodeManager)(implicit
  system: ActorSystem
) {
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  import system.dispatcher

  def startChecking(): Unit = {
    system.scheduler.scheduleAtFixedRate(
      initialDelay = 0.seconds,
      healthConfig.interval
    ) { () =>
      nodeManager.getNodes().foreach(checkHealth)
    }
  }

  private[adapter] def checkHealth(node: Node): Unit = {
    val healthCheckUri = Uri(node.url).withPath(Uri.Path("/health"))
    val httpPoolSettings = ConnectionPoolSettings(system)
      .withMaxConnections(16)
      .withMaxRetries(3)
      .withMaxOpenRequests(64)
      Http().singleRequest(HttpRequest(uri = healthCheckUri), settings = httpPoolSettings)
      .map {
        case HttpResponse(StatusCodes.OK, _, _, _) =>
       //   logger.info(s"Node is healthy ${node.url}")
          nodeManager.updateHealth(node.url, Healthy)
        case _ =>
       //   logger.error(s"Node is not healthy ${node.url}")
          nodeManager.updateHealth(node.url,NotHealthy)
      }
      .recover { case ex =>
        logger.error(s"Node ${node.url} is not healthy due to ${ex.getMessage}")
        nodeManager.updateHealth(node.url,NotHealthy)
      }
  }
}
