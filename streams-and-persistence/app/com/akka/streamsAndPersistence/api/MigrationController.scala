package com.akka.streamsAndPersistence.api

import akka.actor.{ActorRef, ActorSystem}
import akka.util.Timeout
import akka.pattern.ask
import com.akka.streamsAndPersistence.actors.MigrationSupervisorActor
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsError, Json, Reads}
import play.api.mvc._

import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class MigrationController @Inject()(system: ActorSystem, cc: ControllerComponents) extends AbstractController(cc) with Serializers {

  val migrationSupervisor: ActorRef = system.actorOf(MigrationSupervisorActor.props())

  implicit val timeout: Timeout = Timeout(50 seconds)

  def getTemplateMigrationActors(): Action[AnyContent] = Action.async {
    val result = (migrationSupervisor ? MigrationSupervisorActor.GetAllChildren).mapTo[Seq[String]]
    result.map {
      actors => Results.Ok(Json.toJson(actors))
    }
  }

  def triggerMigration(id: String) = Action(validateJson[TriggerNewMigration]) { cmd =>
    migrationSupervisor ! MigrationSupervisorActor.TriggerNewMigration(
      name = id,
      numberOfChecklists = cmd.body.numberOfChecklists,
      newRevision = cmd.body.newRevision,
    )
    Results.Ok
  }

  private def validateJson[A: Reads] = parse.json.validate(
    _.validate[A].asEither.left.map(e => BadRequest(JsError.toJson(e)))
  )

}
