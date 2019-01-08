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

class WashingMachineFSM(name: String) extends FSM[State, Data] with ActorLogging {
  val r = new scala.util.Random

  startWith(Idle, Uninitialized)

  val commonHandlers: StateFunction = {
    case Event(WashingMachineFSM.GetCurrentState, _) => {
      returnCurrentState()
      stay
    }
    case Event(WashingMachineFSM.GetTotalPowerConsumption, _) => {
      returnTotalPowerConsumption()
      stay
    }
  }

  val idealStateHandlers: StateFunction = {
    case Event(WashingMachineFSM.StartNewLoad(power, _), Uninitialized) ⇒ {
      if (power == PowerLevel.FLUCTUATING) {
        goto(Stopped)
      } else {
        goto(Running) using PowerConsumption(currentState = DeviceState.ON, powerLevel = power, powerConsumptionMap = Map.empty)
      }
    }

  }

  val runningStateHandlers: StateFunction = {
    case Event(WashingMachineFSM.CapturePowerConsumption(consumption, time), PowerConsumption(currentState, power, consumptionMap)) =>
      if (consumption > 50) {
        log.error(s"${consumption} exceeds the maximum allowed consumption of 50")
        throw new IllegalArgumentException(s"${consumption} exceeds the maximum allowed consumption of 50")
      } else {
        stay using PowerConsumption(currentState = currentState, powerLevel = power, powerConsumptionMap = consumptionMap + (time -> consumption))
      }

    case Event(WashingMachineFSM.FinishCurrentLoad, _) => goto(Idle)

  }

  when(Idle)(commonHandlers orElse idealStateHandlers)

  when(Running)(commonHandlers orElse runningStateHandlers)

  when(Stopped)(commonHandlers)

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
          self ! WashingMachineFSM.CapturePowerConsumption(powerConsumption, timeNow)
        }
      }

    case _ -> Stopped => context.stop(self)

  }

  initialize()

  def returnCurrentState(): Unit = {
    sender ! stateData.currentState
  }

  def returnTotalPowerConsumption(): Unit = {
    sender ! (0 /: stateData.powerConsumptionMap.values)(_ + _)
  }

}

object WashingMachineFSM {

  def props(name: String): Props = Props(new WashingMachineFSM(name = name))

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
