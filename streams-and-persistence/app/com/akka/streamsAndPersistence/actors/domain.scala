package com.akka.streamsAndPersistence.actors

object PowerLevel extends Enumeration {
  val LOW, MEDIUM, HIGH, FLUCTUATING = Value
}

object DeviceState extends Enumeration {
  val ON, OFF = Value
}
