package com.akka.home.actors

import com.akka.home.PowerLevel
import org.joda.time.DateTime

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
