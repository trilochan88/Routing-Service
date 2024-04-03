package com.ts
package infrastructure.config

import com.typesafe.config.Config

import scala.concurrent.duration.*

case class HealthConfig(interval: FiniteDuration)

object HealthConfig {
  def apply(config: Config): HealthConfig = {
    new HealthConfig(config.getInt("interval").seconds)
  }
}
