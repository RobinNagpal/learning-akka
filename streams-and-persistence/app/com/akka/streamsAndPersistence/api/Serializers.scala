package com.akka.streamsAndPersistence.api

import org.joda.time.DateTime
import play.api.libs.json.{Format, Json, Reads, Writes}

trait Serializers {

  val pattern = "yyyy-MM-dd'T'HH:mm:ss"
  implicit val dateFormat: Format[DateTime] =
    Format[DateTime](Reads.jodaDateReads(pattern), Writes.jodaDateWrites(pattern))

  implicit val triggerNewMigrationReads: Reads[TriggerNewMigration] =
    Json.reads[TriggerNewMigration]

}
