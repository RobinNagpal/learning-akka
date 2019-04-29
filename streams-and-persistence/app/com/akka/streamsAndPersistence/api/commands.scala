package com.akka.streamsAndPersistence.api

case class TriggerNewMigration(
    numberOfChecklists: Int,
    newRevision: Int
)
