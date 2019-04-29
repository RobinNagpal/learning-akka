package com.akka.streamsAndPersistence.api

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.akka.streamsAndPersistence.actors.{DeviceState, PowerLevel, WashingMachineStreamsActor}
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.libs.json.{JsError, Json, Reads}
import play.api.mvc.{AbstractController, ControllerComponents, Result, Results}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

@Singleton
class WashingMachineStreamController @Inject()(system: ActorSystem, cc: ControllerComponents) extends AbstractController(cc) with Serializers {

  val samsung = system.actorOf(WashingMachineStreamsActor.props("Samsung"))
  val lg = system.actorOf(WashingMachineStreamsActor.props("LG"))
  val bosh = system.actorOf(WashingMachineStreamsActor.props("Bosh"))

  implicit val timeout = Timeout(5 seconds)

  def getWashingMachines() = Action {
    Results.Ok(Json.toJson(List("Samsung", "LG", "Bosh")))
  }

  def getWashingMachineState(id: String) = Action.async {
    findMachineAndDoAsync(id) {
      machine => {
        val stateFuture: Future[DeviceState.Value] = (machine ? WashingMachineStreamsActor.GetCurrentState).mapTo[DeviceState.Value]
        stateFuture.map( state => Results.Ok(Json.toJson(Map("state" -> state.toString))))
      }
    }
  }

  def getWashingMachineConsumption(id: String) = Action.async {
    findMachineAndDoAsync(id) {
      machine => {
        val powerFuture: Future[Int] = (machine ? WashingMachineStreamsActor.GetTotalPowerConsumption).mapTo[Int]
        powerFuture.map( power => Results.Ok(Json.toJson(Map("power" -> power))))
      }
    }
  }

  def startWashingMachine(id: String) = Action {
    findMachineAndDo(id) {
      machine => {
        machine ! WashingMachineStreamsActor.StartMachine(level = PowerLevel.HIGH, time = DateTime.now())
        Results.Ok
      }
    }
  }

  def capturePowerConsumption(id: String) = Action(validateJson[CapturePowerConsumptionCmd]) { cmd =>
    findMachineAndDo(id) {
      machine => {
        machine ! WashingMachineStreamsActor.CapturePowerConsumption(consumption = cmd.body.consumption, time = DateTime.now())
        Results.Ok
      }
    }
  }

  def capturePowerConsumptionInBatches(id: String) = Action(validateJson[CapturePowerConsumptionInBatches]) { cmd =>
    findMachineAndDo(id) {
      machine => {
        machine ! WashingMachineStreamsActor.CapturePowerConsumptionInBatches(
          batchSize = cmd.body.batchSize,
          singleReadingAmount = cmd.body.singleReadingAmount,
          totalNumberOfReadings = cmd.body.totalNumberOfReadings,
          startDate = cmd.body.startDate,
          endDate = cmd.body.endDate
        )
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

  def findMachineAndDo(id:String)(f: ActorRef => Result): Result = {
    getWashingMachineActor(id) match {
      case Some(machine) =>
        f(machine)
      case None =>
        Results.NotFound
    }
  }

  def findMachineAndDoAsync(id:String)(f: ActorRef => Future[Result]): Future[Result] = {
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

