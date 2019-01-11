package com.akka.home.api

import akka.actor.{ActorRef, ActorSystem}
import com.akka.home.PowerLevel
import com.akka.home.actors.WashingMachinePersistentActor
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents, Results}
import akka.pattern.ask
import akka.util.Timeout
import com.akka.home._
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class WashingMachineController @Inject()(system: ActorSystem, cc: ControllerComponents) extends AbstractController(cc) with Serializers {

  val samsung = system.actorOf(WashingMachinePersistentActor.props("Samsung"))
  val lg = system.actorOf(WashingMachinePersistentActor.props("LG"))
  val bosh = system.actorOf(WashingMachinePersistentActor.props("Bosh"))

  implicit val timeout = Timeout(5 seconds)

  def getWashingMachines() = Action {
    Results.Ok(Json.toJson(List("Samsung", "LG", "Bosh")))
  }

  def getWashingMachineState(id: String) = Action.async {
    getWashingMachineActor(id) match {

      case Some(machine) =>
        val stateFuture: Future[DeviceState.Value] = (machine ? WashingMachinePersistentActor.GetCurrentStateCmd).mapTo[DeviceState.Value]
        stateFuture.map( state => Results.Ok(Json.toJson(Map("state" -> state.toString))))

      case None =>
        Future.successful(Results.NotFound)
    }
  }

  def startWashingMachine(id: String) = Action {
    getWashingMachineActor(id) match {

      case Some(machine) =>
        machine ! WashingMachinePersistentActor.StartMachineCmd(level = PowerLevel.HIGH, time = DateTime.now())
        Results.Ok

      case None =>
        Results.NotFound
    }
  }

  private def getWashingMachineActor(id: String): Option[ActorRef] = {
    if ("Samsung".equalsIgnoreCase(id)) {
      Some(samsung)
    } else if ("LG".equalsIgnoreCase(id)) {
      Some(lg)
    } else if ("Bosh".equalsIgnoreCase(id)) {
      Some(bosh)
    } else {
      None
    }
  }

}
