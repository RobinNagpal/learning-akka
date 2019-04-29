package com.akka.streamsAndPersistence.actors

import akka.actor.{ActorLogging, Props}
import akka.event.{Logging, LoggingReceive}
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}

class TemplateMigrationActor(name: String) extends PersistentActor with ActorLogging {

  import TemplateMigrationActor._

  var state: TemplateMigrationState = TemplateMigrationState(MigrationState.WAITING, None, List.empty, List.empty, List.empty)
  override def preStart(): Unit = {
    super.preStart()
    log.info("preStart - " + name)
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message)
    log.info("preRestart - " + name)
  }

  override def postRestart(reason: Throwable): Unit = {
    super.postRestart(reason)
    log.info("postRestart - " + name)
  }

  override def receive: Receive = {
    case cmd: StartMigratingChecklistsCmd =>
      log.info("TemplateMigrationActor.StartMigratingChecklists" + cmd)
  }


  def migrateSingleChecklist(evt: MigrateSingleChecklistEvt): Unit = ???

  def migrateAllChecklists(evt: StartMigratingChecklistsEvt): Unit = ???

  def migrateChecklistsGroup(evt: MigrateChecklistGroupEvt): Unit = ???

  def updateState: Receive = LoggingReceive.withLabel("WashingMachinePersistentActor", Logging.InfoLevel) {

    case evt: StartMigratingChecklistsEvt => migrateAllChecklists(evt)
    case evt: MigrateChecklistGroupEvt => migrateChecklistsGroup(evt)
    case evt: MigrateSingleChecklistEvt => migrateSingleChecklist(evt)
    case RecoveryCompleted => log.info("Done with replay of the all the events")
    case msg => log.error("Unknown message received {}", msg)
  }


  def persistAndUpdateState(evt: TemplateMigrationActor.Event) = persist(evt)(persistedEvt => updateState(persistedEvt))

  override def receiveRecover: Receive = {
    case SnapshotOffer(_, snapshot: TemplateMigrationState) ⇒
      log.info(s"- $name - Restoring from snapshot")
      state = snapshot
    case evt @ _ =>
      log.info(s"- $name - Receive recover event received {}", evt)
      updateState(evt)
  }

  override def receiveCommand: Receive = ???

  override def persistenceId: String = ???

  case class TemplateMigrationState(
      currentState: MigrationState.Value,
      migrateToRevision: Option[Int],
      toBeMigratedChecklists: List[String],
      inProgressChecklists: List[String],
      migratedChecklists: List[String]
  )

  object MigrationState extends Enumeration {
    val WAITING, RUNNING, RECOVERING = Value
  }
}

object TemplateMigrationActor {
  def props(name: String): Props = Props(new TemplateMigrationActor(name))

  case class StartMigrating()




  /******************************** Commands ******************************************************/
  trait Command

  case class StartMigratingChecklistsCmd(numberOfChecklists: Int, newRevision: Int) extends Command

  case class MigrateChecklistGroupCmd(checklistIds: List[String]) extends Command

  case class MigrateSingleChecklistCmd(checklistId: String) extends Command

  /******************************** Events ******************************************************/
  trait Event

  case class StartMigratingChecklistsEvt(numberOfChecklists: Int, newRevision: Int) extends Event

  case class MigrateChecklistGroupEvt(checklistIds: List[String]) extends Event

  case class MigrateSingleChecklistEvt(checklistId: String) extends Event

}
