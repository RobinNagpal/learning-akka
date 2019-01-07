package com.akka.home.actors

import com.akka.home.{DeviceState, PowerLevel}
import akka.actor.{Actor, ActorLogging, Props}
import org.joda.time.DateTime

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


class WashingMachineActor(name: String) extends Actor with ActorLogging{

  var currentState: DeviceState.Value = DeviceState.OFF
  var powerConsumptionMap: mutable.Map[DateTime, Int] = mutable.Map.empty
  val r = new scala.util.Random

  override def receive: Receive = {
    case cmd: WashingMachineActor.StartMachine => startMachine(cmd)
    case cmd: WashingMachineActor.StopMachine  => stopMachine(cmd)
    case cmd: WashingMachineActor.CapturePowerConsumption  => capturePowerConsumption(cmd)
    case _: WashingMachineActor.GetCurrentState.type  => returnCurrentState()
    case _: WashingMachineActor.GetTotalPowerConsumption.type  => returnTotalPowerConsumption()
  }

  def startMachine(cmd: WashingMachineActor.StartMachine): Unit = {
    if(currentState == DeviceState.ON)
      throw new IllegalArgumentException(s"Cannot reissue start. Washing machine: ${name} already in ON state")


    if(cmd.level == PowerLevel.FLUCTUATING)
      context.stop(self)

    log.info(s"Started washing machine ${name}")

    currentState = DeviceState.ON

    Future {
      while (currentState == DeviceState.ON) {
        Thread.sleep(5000)
        val powerConsumption = r.nextInt(50)
        val timeNow = DateTime.now()
        log.info(s"recording power consumption of ${powerConsumption} at ${timeNow}")
        powerConsumptionMap += (timeNow -> powerConsumption)
      }
    }

  }

  def stopMachine(cmd: WashingMachineActor.StopMachine): Unit = {
    currentState = DeviceState.OFF
  }

  def capturePowerConsumption(cmd: WashingMachineActor.CapturePowerConsumption): Unit = {
    if(cmd.consumption > 50)
      throw new IllegalArgumentException(s"${cmd.consumption} exceeds the maximum allowed consumption of 50")

    powerConsumptionMap += (cmd.time -> cmd.consumption)
  }

  def returnCurrentState(): Unit = {
    sender ! currentState
  }

  def returnTotalPowerConsumption(): Unit = {
   sender !  (0 /: powerConsumptionMap.values)(_ + _)
  }

}

object WashingMachineActor {

  def props(name: String): Props = Props(new WashingMachineActor(name = name))

  case class StartMachine(
      level: PowerLevel.Value,
      time: DateTime = DateTime.now()
  )

  case class StopMachine(
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
