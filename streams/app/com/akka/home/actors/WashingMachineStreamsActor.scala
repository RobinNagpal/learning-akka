package com.akka.home.actors

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.akka.home.actors.WashingMachineStreamsActor.CapturePowerConsumption
import org.joda.time.DateTime

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

/*

 * Can capture given power in batches of given amount between start and end time
 *
 * Can return the power consumption by day

 */
class WashingMachineStreamsActor(name: String) extends Actor with ActorLogging {

  var currentState: DeviceState.Value = DeviceState.OFF
  var powerConsumptionMap: mutable.Map[DateTime, Int] = mutable.Map.empty
  val r = new scala.util.Random

  implicit val materializer = ActorMaterializer()

  override def receive: Receive = {
    case cmd: WashingMachineStreamsActor.StartMachine => startMachine(cmd)
    case cmd: WashingMachineStreamsActor.StopMachine => stopMachine(cmd)
    case cmd: WashingMachineStreamsActor.CapturePowerConsumption => capturePowerConsumption(cmd)
    case cmd: WashingMachineStreamsActor.CapturePowerConsumptionInBatches => capturePowerConsumptionInBatches(cmd)
    case WashingMachineStreamsActor.GetCurrentState => returnCurrentState()
    case WashingMachineStreamsActor.GetTotalPowerConsumption => returnTotalPowerConsumption()
    case WashingMachineStreamsActor.GetAllReadings => returnTotalPowerConsumption()
  }

  def startMachine(cmd: WashingMachineStreamsActor.StartMachine): Unit = {
    if (currentState == DeviceState.ON)
      throw new IllegalArgumentException(s"Cannot reissue start. Washing machine: ${name} already in ON state")

    if (cmd.level == PowerLevel.FLUCTUATING)
      context.stop(self)

    log.info(s"Started washing machine ${name}")

    currentState = DeviceState.ON

    Future {
      while (currentState == DeviceState.ON) {
        Thread.sleep(50000)
        val powerConsumption = r.nextInt(50)
        val timeNow = DateTime.now()
        log.info(s"recording power consumption of ${powerConsumption} at ${timeNow}")
        powerConsumptionMap += (timeNow -> powerConsumption)
      }
    }

  }

  def stopMachine(cmd: WashingMachineStreamsActor.StopMachine): Unit = {
    currentState = DeviceState.OFF
  }

  def capturePowerConsumption(cmd: WashingMachineStreamsActor.CapturePowerConsumption): Unit = {
    if (cmd.consumption > 50)
      throw new IllegalArgumentException(s"${cmd.consumption} exceeds the maximum allowed consumption of 50")

    powerConsumptionMap += (cmd.time -> cmd.consumption)
  }

  def capturePowerConsumptionInBatches(cmd: WashingMachineStreamsActor.CapturePowerConsumptionInBatches): Unit = {

    def randomDateTimeFn(): DateTime = randomDateTime(cmd.startDate, cmd.endDate)
    def toReadingsInBatchFn(batchNumber: Int):Source[(Int, DateTime, Int), NotUsed] = Source(0 to cmd.batchSize).map(_ => (batchNumber, randomDateTimeFn(), 1))

    val numberOfBatchedReadings = Math.ceil(cmd.totalNumberOfReadings.toDouble / cmd.batchSize.toDouble).toInt




    // The Source type is parameterized with two types:
    // - first one is the type of element that this source emits
    // - second one may signal that running the source produces some auxiliary value (e.g. a network source may provide information about the bound port or the peer’s address).
    // Where no auxiliary information is produced, the type akka.NotUsed is used—and a simple range of integers surely falls into this category
    val source: Source[Int, NotUsed] = Source(1 to numberOfBatchedReadings)
    //
    val done = source
      .flatMapConcat(batchNumber => toReadingsInBatchFn(batchNumber))
      .throttle(5, 5 seconds)
      .runForeach(reading => {
        log.info(s"Recording reading of batch ${reading._1} @ ${reading._2} -> ${reading._3}")
        self ! CapturePowerConsumption(consumption = reading._3, time = reading._2)
      })(materializer)

    done.onComplete(_ ⇒ log.info("Done capturing the power of batch"))
  }



  def returnCurrentState(): Unit = {
    sender ! currentState
  }

  def returnTotalPowerConsumption(): Unit = {
    sender ! (0 /: powerConsumptionMap.values)(_ + _)
  }

  def returnAllReadings(): Unit = {
    sender ! powerConsumptionMap
  }

  def randomDateTime(start: DateTime, end: DateTime)(): DateTime = new DateTime(Math.random * (end.getMillis - start.getMillis))
}

object WashingMachineStreamsActor {

  def props(name: String): Props = Props(new WashingMachineStreamsActor(name = name))

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

  case object GetAllReadings

  case class GetConsumptionSince(time: DateTime)

  case class CapturePowerConsumptionInBatches(
      batchSize: Int,
      singleReadingAmount: Int,
      totalNumberOfReadings: Int,
      startDate: DateTime,
      endDate: DateTime
  )

}
