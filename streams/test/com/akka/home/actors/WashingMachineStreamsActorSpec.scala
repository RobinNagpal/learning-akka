package com.akka.home.actors

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.Timeout
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration.DurationLong

class WashingMachineStreamsActorSpec extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll{

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  implicit val timeout: Timeout = Timeout(5 seconds)

  private case class StartedMachineSUT(){
    val washingMachine = system.actorOf(WashingMachineStreamsActor.props("Samsung"))

    washingMachine ! WashingMachineStreamsActor.StartMachine(
      level = PowerLevel.HIGH
    )
  }

  "A Washing machine actor" must {

    "start with proper state" in {
      val washingMachineSUT = StartedMachineSUT()
      washingMachineSUT.washingMachine ! WashingMachineStreamsActor.GetCurrentState
      expectMsg(DeviceState.ON)
    }

    "return correct power consumption" in {
      val washingMachine = StartedMachineSUT().washingMachine
      washingMachine ! WashingMachineStreamsActor.GetTotalPowerConsumption
      expectMsg(0)


      washingMachine ! WashingMachineStreamsActor.CapturePowerConsumption(consumption = 25)
      washingMachine ! WashingMachineStreamsActor.GetTotalPowerConsumption
      expectMsg(25)
    }

    "actor should restart if consumption is too high" in {
      val washingMachine = StartedMachineSUT().washingMachine

      washingMachine ! WashingMachineStreamsActor.GetTotalPowerConsumption
      expectMsg(0)


      washingMachine ! WashingMachineStreamsActor.CapturePowerConsumption(consumption = 25)
      washingMachine ! WashingMachineStreamsActor.GetTotalPowerConsumption
      expectMsg(25)

      washingMachine ! WashingMachineStreamsActor.CapturePowerConsumption(consumption = 500)

      washingMachine ! WashingMachineStreamsActor.GetCurrentState
      expectMsgPF(){
        case DeviceState.OFF => ()
        case _ => fail
      }

      washingMachine ! WashingMachineStreamsActor.GetTotalPowerConsumption
      expectMsg(0)
    }

    "actor should stop id the power is fluctuating" in {
      val probe = TestProbe()

      val washingMachine = system.actorOf(WashingMachineStreamsActor.props("Samsung"))


      probe watch washingMachine
      washingMachine ! WashingMachineStreamsActor.StartMachine(
        level = PowerLevel.FLUCTUATING
      )

      probe.expectTerminated(washingMachine)
    }

  }

}
