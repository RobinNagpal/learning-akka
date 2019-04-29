package com.akka.home.actors

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import akka.util.Timeout
import com.akka.home.{DeviceState, PowerLevel}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import scala.concurrent.duration.DurationLong

class WashingMachinePersistentActorSpec
    extends TestKit(ActorSystem("MySpec"))
    with ImplicitSender
    with WordSpecLike
    with Matchers
    with GuiceOneAppPerSuite
    with BeforeAndAfterAll {
  import WashingMachinePersistentActor._

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  implicit val timeout: Timeout = Timeout(5 seconds)

  private case class StartedMachineSUT(sutName: String) {
    val washingMachine = system.actorOf(WashingMachinePersistentActor.props("Samsung-" + sutName))

    washingMachine ! StartMachineCmd(
      level = PowerLevel.HIGH
    )
  }

  "A Washing machine actor" must {

    "start with proper state" in {
      val washingMachineSUT = StartedMachineSUT("start-with-proper-state")
      washingMachineSUT.washingMachine ! GetCurrentStateCmd
      expectMsg(DeviceState.ON)
    }

    "return correct power consumption" in {
      val washingMachine = StartedMachineSUT("return-correct-power").washingMachine
      washingMachine ! GetTotalPowerConsumptionCmd
      expectMsg(0)

      washingMachine ! CapturePowerConsumptionCmd(consumption = 25)
      washingMachine ! GetTotalPowerConsumptionCmd
      expectMsg(25)
    }

    "actor should restart if consumption is too high" in {
      val washingMachine = StartedMachineSUT("restart-if-power-too-high").washingMachine

      washingMachine ! GetTotalPowerConsumptionCmd
      expectMsg(0)

      washingMachine ! CapturePowerConsumptionCmd(consumption = 25)
      washingMachine ! GetTotalPowerConsumptionCmd
      expectMsg(25)

      washingMachine ! CapturePowerConsumptionCmd(consumption = 500)

      washingMachine ! GetCurrentStateCmd
      expectMsgPF() {
        case DeviceState.ON => ()
        case _ => fail
      }

      washingMachine ! GetTotalPowerConsumptionCmd
      expectMsg(25)
    }

    "actor should stop id the power is fluctuating" in {
      val probe = TestProbe()

      val washingMachine = system.actorOf(WashingMachinePersistentActor.props("Samsung-stop-if-power-is-fluctuating"))

      probe watch washingMachine
      washingMachine ! StartMachineCmd(
        level = PowerLevel.FLUCTUATING
      )

      probe.expectTerminated(washingMachine)
    }

  }

}
