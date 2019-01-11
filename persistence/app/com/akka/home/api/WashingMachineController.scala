package com.akka.home.api


import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents, Results}

@Singleton
class WashingMachineController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Serializers {

  def getWashingMachines() = Action {
    Results.Ok(Json.toJson(List("Samsung", "LG", "Bosh")))
  }

  def startWashingMachine(id: String) = Action {
    if ("Samsung".equalsIgnoreCase(id)) {
      Results.Ok
    } else {
      Results.NotFound
    }

  }

}
