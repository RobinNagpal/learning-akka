package com.akka.home.actors

import com.akka.home.{DeviceState, PowerLevel}
import akka.actor.{ActorLogging, FSM, Props}
import org.joda.time.DateTime
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

// states
sealed trait State

case object Idle extends State

case object Running extends State

case object Stopped extends State

sealed trait Data {
  val currentState: DeviceState.Value
  val powerConsumptionMap: Map[DateTime, Int]
}

case object Uninitialized extends Data {
  override val currentState: DeviceState.Value = DeviceState.OFF
  override val powerConsumptionMap: Map[DateTime, Int] = Map.empty
}

final case class PowerConsumption(
    currentState: DeviceState.Value,
    powerLevel: PowerLevel.Value,
    powerConsumptionMap: Map[DateTime, Int]
) extends Data

class WashingMachineActor(name: String) extends FSM[State, Data] with ActorLogging {
  val r = new scala.util.Random

  startWith(Idle, Uninitialized)

  val commonHandlers: StateFunction = {
    case Event(WashingMachineActor.GetCurrentState, _) => {
      returnCurrentState()
      stay
    }
    case Event(WashingMachineActor.GetTotalPowerConsumption, _) => {
      returnTotalPowerConsumption()
      stay
    }
  }

  val idealStateHandlers: StateFunction = {
    case Event(WashingMachineActor.StartNewLoad(power, _), Uninitialized) ⇒
      goto(Running) using PowerConsumption(currentState = DeviceState.ON, powerLevel = power, powerConsumptionMap = Map.empty)
  }

  val runningStateHandlers: StateFunction = {
    case Event(WashingMachineActor.CapturePowerConsumption(consumption, time), PowerConsumption(currentState, power, consumptionMap)) =>
      if (consumption > 50) {
        log.error(s"${consumption} exceeds the maximum allowed consumption of 50")
        goto(Stopped)
      } else {
        stay using PowerConsumption(currentState = currentState, powerLevel = power, powerConsumptionMap = consumptionMap + (time -> consumption))
      }

    case Event(WashingMachineActor.FinishCurrentLoad, _) => goto(Idle)

  }

  when(Idle)(commonHandlers orElse idealStateHandlers)

  when(Running)(commonHandlers orElse runningStateHandlers)

  whenUnhandled {
    case Event(e, s) ⇒
      log.warning("received unhandled request {} in state {}/{}", e, stateName, s)
      stay
  }

  onTransition {
    case Idle -> Running =>
      Future {
        while (stateName == Running) {
          Thread.sleep(5000)
          val powerConsumption = r.nextInt(50)
          val timeNow = DateTime.now()
          log.info(s"recording power consumption of ${powerConsumption} at ${timeNow}")
          self ! WashingMachineActor.CapturePowerConsumption(powerConsumption, timeNow)
        }
      }

    case Running -> Stopped =>
      context.stop(self)
  }

  initialize()

  def returnCurrentState(): Unit = {
    sender ! stateData.currentState
  }

  def returnTotalPowerConsumption(): Unit = {
    sender ! (0 /: stateData.powerConsumptionMap.values)(_ + _)
  }

}

object WashingMachineActor {

  def props(name: String): Props = Props(new WashingMachineActor(name = name))

  case class StartNewLoad(
      level: PowerLevel.Value,
      time: DateTime = DateTime.now()
  )

  case class FinishCurrentLoad(
      time: DateTime = DateTime.now()
  )

  case class CapturePowerConsumption(
      consumption: Int,
      time: DateTime = DateTime.now()
  )

  case object GetCurrentState

  case object GetTotalPowerConsumption

  case class GetConsumptionSince(time: DateTime)

}
