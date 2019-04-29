package com.akka.streamsAndPersistence.actors

import akka.actor.{Actor, ActorLogging, Props}

class TemplateMigrationActor(name: String) extends Actor with ActorLogging {

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
    case cmd: TemplateMigrationActor.StartMigratingChecklists =>
      log.info("TemplateMigrationActor.StartMigratingChecklists" + cmd)
  }
}

object TemplateMigrationActor {
  def props(name: String): Props = Props(new TemplateMigrationActor(name))

  case class StartMigratingChecklists(numberOfChecklists: Int, newRevision: Int)

}
