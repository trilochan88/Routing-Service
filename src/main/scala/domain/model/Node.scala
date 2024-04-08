package com.ts
package domain.model

import common.enums.SlownessStatus.Normal
import common.enums.{HealthStatus, SlownessStatus}


/** Contain
  */
case class Node(
  url: String,
  healthStatus: HealthStatus = HealthStatus.Healthy,
  slownessStatus: SlownessStatus = Normal,
)
