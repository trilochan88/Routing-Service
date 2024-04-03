package com.ts
package infrastructure.config

import com.typesafe.config.Config

import scala.concurrent.duration.*
case class CircuitBreakerConfig(
    maxFailures: Int,
    callTimeout: FiniteDuration,
    resetTimeout: FiniteDuration
)

object CircuitBreakerConfig {
  def apply(config: Config): CircuitBreakerConfig = {
    new CircuitBreakerConfig(
      maxFailures = config.getInt("maxFailures"),
      callTimeout = config.getInt("callTimeout").seconds,
      resetTimeout = config.getInt("resetTimeout").minute
    )
  }
}
