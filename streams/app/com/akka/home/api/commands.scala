package com.akka.home.api

import org.joda.time.DateTime

case class CapturePowerConsumptionCmd(
    consumption: Int
)

case class CapturePowerConsumptionInBatches(
    batchSize: Int,
    singleReadingAmount: Int,
    totalNumberOfReadings: Int,
    startDate: DateTime,
    endDate: DateTime
)
