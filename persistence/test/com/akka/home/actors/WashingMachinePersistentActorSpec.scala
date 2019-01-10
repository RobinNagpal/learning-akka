package com.akka.home.actors

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import akka.util.Timeout
import com.akka.home.{DeviceState, PowerLevel}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import scala.concurrent.duration.DurationLong

class WashingMachinePersistentActorSpec extends  TestKit(ActorSystem("MySpec")) with ImplicitSender with WordSpecLike with Matchers with GuiceOneAppPerSuite with BeforeAndAfterAll{

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  implicit val timeout: Timeout = Timeout(5 seconds)

  private case class StartedMachineSUT(){
    val washingMachine = system.actorOf(WashingMachinePersistentActor.props("Samsung"))

    washingMachine ! WashingMachinePersistentActor.StartMachine(
      level = PowerLevel.HIGH
    )
  }

  "A Washing machine actor" must {

    "start with proper state" in {
      val washingMachineSUT = StartedMachineSUT()
      washingMachineSUT.washingMachine ! WashingMachinePersistentActor.GetCurrentState
      expectMsg(DeviceState.ON)
    }

    "return correct power consumption" in {
      val washingMachine = StartedMachineSUT().washingMachine
      washingMachine ! WashingMachinePersistentActor.GetTotalPowerConsumption
      expectMsg(0)


      washingMachine ! WashingMachinePersistentActor.CapturePowerConsumption(consumption = 25)
      washingMachine ! WashingMachinePersistentActor.GetTotalPowerConsumption
      expectMsg(25)
    }

    "actor should restart if consumption is too high" in {
      val washingMachine = StartedMachineSUT().washingMachine

      washingMachine ! WashingMachinePersistentActor.GetTotalPowerConsumption
      expectMsg(0)


      washingMachine ! WashingMachinePersistentActor.CapturePowerConsumption(consumption = 25)
      washingMachine ! WashingMachinePersistentActor.GetTotalPowerConsumption
      expectMsg(25)

      washingMachine ! WashingMachinePersistentActor.CapturePowerConsumption(consumption = 500)

      washingMachine ! WashingMachinePersistentActor.GetCurrentState
      expectMsgPF(){
        case DeviceState.OFF => ()
        case _ => fail
      }

      washingMachine ! WashingMachinePersistentActor.GetTotalPowerConsumption
      expectMsg(0)
    }

    "actor should stop id the power is fluctuating" in {
      val probe = TestProbe()

      val washingMachine = system.actorOf(WashingMachinePersistentActor.props("Samsung"))


      probe watch washingMachine
      washingMachine ! WashingMachinePersistentActor.StartMachine(
        level = PowerLevel.FLUCTUATING
      )

      probe.expectTerminated(washingMachine)
    }

  }

}
