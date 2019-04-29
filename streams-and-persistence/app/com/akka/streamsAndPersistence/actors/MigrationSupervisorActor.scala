package com.akka.streamsAndPersistence.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

class MigrationSupervisorActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case cmd: MigrationSupervisorActor.TriggerNewMigration =>
      startMigration(cmd)

    case MigrationSupervisorActor.GetAllChildren =>
      sender() ! context.children.map(_.path.toStringWithoutAddress)
  }

  def startMigration(cmd: MigrationSupervisorActor.TriggerNewMigration): Unit = {
    val child = context.child(cmd.name).getOrElse(context.actorOf(TemplateMigrationActor.props(cmd.name), cmd.name))
    child ! TemplateMigrationActor.StartMigratingChecklists(
      numberOfChecklists = cmd.numberOfChecklists,
      newRevision = cmd.newRevision
    )
  }

}

object MigrationSupervisorActor {

  def props(): Props = Props(new MigrationSupervisorActor())

  case class TriggerNewMigration(
      name: String,
      numberOfChecklists: Int,
      newRevision: Int
  )

  case object GetAllChildren

}
