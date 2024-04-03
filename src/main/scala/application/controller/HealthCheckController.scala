package com.ts
package application.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Directives.{complete, path}

class HealthCheckController {
  def routes = path("health") {
    Directives.get {
      complete(StatusCodes.OK, "Up")
    }
  }
}
