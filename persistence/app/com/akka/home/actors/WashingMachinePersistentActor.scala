package com.akka.home.actors

import com.akka.home.{DeviceState, PowerLevel}
import akka.actor.{ActorLogging, Props}
import akka.persistence.PersistentActor
import org.joda.time.DateTime
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class WashingMachinePersistentActor(name: String) extends PersistentActor with ActorLogging {
  import WashingMachinePersistentActor._

  case class WashingMachineState(
      currentState: DeviceState.Value,
      powerConsumptionMap: Map[DateTime, Int]
  )

  var state: WashingMachineState = WashingMachineState(currentState = DeviceState.OFF, powerConsumptionMap = Map.empty)

  val r = new scala.util.Random

  def startMachine(evt: StartMachineEvt): Unit = {
    log.info(s"Started washing machine ${name}")
    state = state.copy(currentState = DeviceState.ON)

    Future {
      while (state.currentState == DeviceState.ON) {
        Thread.sleep(5000)
        val powerConsumption = r.nextInt(50)
        val timeNow = DateTime.now()
        log.info(s"startMachine - recording power consumption of ${powerConsumption} at ${timeNow}")
        state = state.copy(powerConsumptionMap = state.powerConsumptionMap + (timeNow -> powerConsumption))
      }
    }

  }

  def stopMachine(cmd: StopMachineEvt): Unit = {
    state = state.copy(currentState = DeviceState.OFF)
  }

  def capturePowerConsumption(evt: CapturePowerConsumptionEvt): Unit = {
    log.info(s"capturePowerConsumption - recording power consumption of ${evt.consumption} at ${evt.time}")
    state = state.copy(powerConsumptionMap = state.powerConsumptionMap + (evt.time -> evt.consumption))
  }

  def returnCurrentState(): Unit = {
    sender ! state.currentState
  }

  def returnTotalPowerConsumption(): Unit = {
    sender ! (0 /: state.powerConsumptionMap.values)(_ + _)
  }

  def updateState: Receive = {
    case evt: StartMachineEvt => startMachine(evt)
    case evt: StopMachineEvt => stopMachine(evt)
    case evt: CapturePowerConsumptionEvt => capturePowerConsumption(evt)
    case msg => log.error("Unknown message received {}", msg)
  }

  def persistAndUpdateState(evt: Event) = persist(evt)(persistedEvt => updateState(persistedEvt))

  override def receiveRecover: Receive = updateState

  override def receiveCommand: Receive = {
    case cmd: StartMachineCmd =>
      if (state.currentState == DeviceState.ON)
        throw new IllegalArgumentException(s"Cannot reissue start. Washing machine: ${name} already in ON state")

      if (cmd.level == PowerLevel.FLUCTUATING)
        context.stop(self)

      persistAndUpdateState(StartMachineEvt(level = cmd.level, time = cmd.time))

    case cmd: StopMachineCmd => persistAndUpdateState(StopMachineEvt(time = cmd.time))

    case cmd: CapturePowerConsumptionCmd =>
      if (cmd.consumption > 50) {
        throw new IllegalArgumentException(s"${cmd.consumption} exceeds the maximum allowed consumption of 50")
      } else {
        persistAndUpdateState(CapturePowerConsumptionEvt(consumption = cmd.consumption, time = cmd.time))
      }

    case _: GetCurrentStateCmd.type => returnCurrentState()

    case _: GetTotalPowerConsumptionCmd.type => returnTotalPowerConsumption()
  }

  override def persistenceId: String = s"washing-machine-${name}"
}

object WashingMachinePersistentActor {

  def props(name: String): Props = Props(new WashingMachinePersistentActor(name = name))

  /******************************** Commands ******************************************************/

  trait Command

  case class StartMachineCmd(
      level: PowerLevel.Value,
      time: DateTime = DateTime.now()
  ) extends Command

  case class StopMachineCmd(
      time: DateTime = DateTime.now()
  ) extends Command

  case class CapturePowerConsumptionCmd(
      consumption: Int,
      time: DateTime = DateTime.now()
  ) extends Command

  case object GetCurrentStateCmd extends Command

  case object GetTotalPowerConsumptionCmd extends Command

  case class GetConsumptionSinceCmd(time: DateTime) extends Command

 /******************************** Events ******************************************************/
  trait Event

  case class StartMachineEvt(
      level: PowerLevel.Value,
      time: DateTime = DateTime.now()
  ) extends Event

  case class StopMachineEvt(
      time: DateTime = DateTime.now()
  ) extends Event

  case class CapturePowerConsumptionEvt(
      consumption: Int,
      time: DateTime = DateTime.now()
  ) extends Event
}
