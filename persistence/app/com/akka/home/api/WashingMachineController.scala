package com.akka.home.api

import akka.actor.{ActorRef, ActorSystem}
import com.akka.home.PowerLevel
import com.akka.home.actors.WashingMachinePersistentActor
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.libs.json.{JsError, Json, Reads}
import play.api.mvc.{AbstractController, ControllerComponents, Result, Results}
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
    findMachineAndDoAsync(id) { machine =>
      {
        val stateFuture: Future[DeviceState.Value] = (machine ? WashingMachinePersistentActor.GetCurrentStateCmd).mapTo[DeviceState.Value]
        stateFuture.map(state => Results.Ok(Json.toJson(Map("state" -> state.toString))))
      }
    }
  }

  def getWashingMachineConsumption(id: String) = Action.async {
    findMachineAndDoAsync(id) { machine =>
      {
        val powerFuture: Future[Int] = (machine ? WashingMachinePersistentActor.GetTotalPowerConsumptionCmd).mapTo[Int]
        powerFuture.map(power => Results.Ok(Json.toJson(Map("power" -> power))))
      }
    }
  }

  def startWashingMachine(id: String) = Action {
    findMachineAndDo(id) { machine =>
      {
        machine ! WashingMachinePersistentActor.StartMachineCmd(level = PowerLevel.HIGH, time = DateTime.now())
        Results.Ok
      }
    }
  }

  def capturePowerConsumption(id: String) = Action(validateJson[CapturePowerConsumptionCmd]) { cmd =>
    findMachineAndDo(id) { machine =>
      {
        machine ! WashingMachinePersistentActor.CapturePowerConsumptionCmd(consumption = cmd.body.consumption, time = DateTime.now())
        Results.Ok
      }
    }
  }

  def saveSnapshot(id: String) = Action {
    findMachineAndDo(id) { machine =>
      {
        machine ! WashingMachinePersistentActor.SaveSnapshotCmd
        Results.Ok
      }
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

  def findMachineAndDo(id: String)(f: ActorRef => Result): Result = {
    getWashingMachineActor(id) match {
      case Some(machine) =>
        f(machine)
      case None =>
        Results.NotFound
    }
  }

  def findMachineAndDoAsync(id: String)(f: ActorRef => Future[Result]): Future[Result] = {
    getWashingMachineActor(id) match {
      case Some(machine) =>
        f(machine)
      case None =>
        Future.successful(Results.NotFound)
    }
  }

  private def validateJson[A: Reads] = parse.json.validate(
    _.validate[A].asEither.left.map(e => BadRequest(JsError.toJson(e)))
  )

}
