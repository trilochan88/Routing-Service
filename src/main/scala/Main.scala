package com.ts

import application.controller.RoutingController
import application.service.RoutingService
import domain.model.Node
import domain.service.{PostRequestHandler, RequestHandler, RoundRobinStrategy, RoutingStrategy}
import infrastructure.adapter.*
import infrastructure.config.{CircuitBreakerConfig, HealthConfig}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

object Main extends App {
  implicit val system: ActorSystem = ActorSystem("routing-service")
  implicit val executionContext: ExecutionContextExecutor = system.getDispatcher
  private val rootConfigs = ConfigFactory.load()
  /*val nodes =
    Seq(Node("http://localhost:9000"), Node("http://localhost:9001"), Node("http://localhost:9002"))*/
  private val circuitBreakerConfig: CircuitBreakerConfig = CircuitBreakerConfig(
    rootConfigs.getConfig("routing-service.circuit-breaker")
  )
  private val healthConfig: HealthConfig = HealthConfig(
    rootConfigs.getConfig("routing-service.health-service")
  )
  val nodes: Seq[Node] = rootConfigs
    .getStringList("routing-service.application-instances")
    .toArray()
    .toSeq
    .map(_.toString)
    .map(url ⇒ Node(url))
  private val circuitBreakerManager: CircuitBreakerManager = CircuitBreakerManager(
    circuitBreakerConfig
  )
  private val httpServiceAdapter: HttpService = AkkaHttpService(circuitBreakerManager)
  private val requestHandler: RequestHandler = PostRequestHandler(httpServiceAdapter)
  private val routingStrategy: RoutingStrategy = RoundRobinStrategy()
  private val routingService: RoutingService = RoutingService(routingStrategy, nodes)
  private val monitoringService = MonitoringService()
  nodes.foreach(_.attach(routingService))
  nodes.foreach(_.attach(monitoringService))
  private val healthService = new HealthService(healthConfig, nodes)
  healthService.startChecking()
  private val routes: Route =
    RoutingController(routingService, requestHandler)(system, executionContext).routes
  private val bindingFuture = Http().newServerAt("localhost", 8080).bind(routes)

  println(s"Server is up at http://localhost:8080/\n")
  StdIn.readLine()
}
