package com.akka.streamsAndPersistence.api

import com.akka.streamsAndPersistence.actors.WashingMachineStreamsActor.CapturePowerConsumptionInBatches
import org.joda.time.DateTime
import play.api.libs.json.{Format, Json, Reads, Writes}

trait Serializers {

  val pattern = "yyyy-MM-dd'T'HH:mm:ss"
  implicit val dateFormat = Format[DateTime](Reads.jodaDateReads(pattern), Writes.jodaDateWrites(pattern))

  implicit val capturePowerConsumptionReads = Json.reads[CapturePowerConsumptionCmd]
  implicit val capturePowerConsumptionInBatchesReads = Json.reads[CapturePowerConsumptionInBatches]

}
