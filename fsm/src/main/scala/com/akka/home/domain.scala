package com.akka.home

import org.joda.time.DateTime

import scala.collection.mutable

object PowerLevel extends Enumeration {
  val LOW, MEDIUM, HIGH, FLUCTUATING = Value
}

object DeviceState extends Enumeration {
  val ON, OFF = Value
}
