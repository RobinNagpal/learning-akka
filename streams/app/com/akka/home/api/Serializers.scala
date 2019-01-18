package com.akka.home.api

import play.api.libs.json.Json

trait Serializers {

  implicit val capturePowerConsumptionReads = Json.reads[CapturePowerConsumptionCmd]

}
