package com.akka.home.actors

object PowerLevel extends Enumeration {
  val LOW, MEDIUM, HIGH, FLUCTUATING = Value
}

object DeviceState extends Enumeration {
  val ON, OFF = Value
}