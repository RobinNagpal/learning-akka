package com.akka.home.api


import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents, Results}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class WashingMachineController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Serializers {

  def getWashingMachines() = Action.async {
    Future(Results.Ok(Json.toJson((List("Samsung", "LG", "Bosh")))))
  }

}
