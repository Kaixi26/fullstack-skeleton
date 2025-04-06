package com.example.repo

import com.example.ApplicationConfig
import com.example.repo.DataService.MigrationStep
import com.example.repo.model.{MigrationRow, Queries}
import com.zaxxer.hikari.pool.{HikariProxyConnection, ProxyConnection}
import io.getquill.*
import io.getquill.jdbczio.Quill
import org.sqlite.SQLiteConnection
import zio.{IO, ZIO, ZLayer}

import java.sql.{SQLException, Timestamp}
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.sql.DataSource
import scala.util.{Try, Using}

final class DataService(config: ApplicationConfig, quill: Quill.Sqlite[SnakeCase]) extends Queries {

  import quill._
  import io.getquill.autoQuote

  def up: IO[Throwable, Unit] = for {
    _ <- ZIO.logInfo("Running migrations")
    _ <- fromResource("migrations/00_initial.sql")
    appliedMigrations <- getMigrations.map(_.map(_.name).toSet)
    _ <-
      if (appliedMigrations != steps.map(_.name).toSet) {
        ZIO.foreachDiscard(steps)(step => runStep(step, appliedMigrations))
      } else {
        ZIO.logDebug("All migrations applied, skipping")
      }
    _ <- ZIO.logInfo("Finished running migrations")
  } yield ()

  private def runStep(step: MigrationStep, appliedMigrations: Set[String]) = for {
    isApplied <- ZIO.succeed(appliedMigrations.contains(step.name))
    _ <- ZIO.logDebug(s"Step ${step.name} (isApplied = $isApplied)")
    _ <- ZIO.when(!isApplied)(step.wf.zip(insertMigration(step.name)))
  } yield ()

  private def steps: Seq[MigrationStep] = Array(
    MigrationStep("01_counter", fromResource("migrations/01_counter.sql")),
    MigrationStep("02_message", fromResource("migrations/02_message.sql")),
  )

  private def fromResource(resource: String): IO[Throwable, Unit] = for {
    queries <- ZIO.fromTry(
      Using(scala.io.Source.fromResource(resource, classLoader = DataService.getClass.getClassLoader)) { r =>
        r.mkString
          .replaceAll("--.*[\n]", "")
          .replaceAll("/[*].*[*]/", "")
          .split(";")
          .map(_.trim)
          .filter(!_.isBlank)
      }
    )
    _ <- ZIO.foreachDiscard(queries) { query =>
      ZIO
        .logInfo(s"Executing $query")
        .zipRight(quill.run(sql"#${query}".as[Action[Unit]]))
    }
  } yield ()

  private def getMigrations: IO[SQLException, List[MigrationRow]] =
    run(qMigration)

  private def insertMigration(name: String): IO[SQLException, Unit] = {
    val migration = MigrationRow(name, Timestamp.from(Instant.now()))
    run(qMigration.insertValue(lift(migration)).onConflictIgnore).unit
  }

}

object DataService {

  val layer: ZLayer[ApplicationConfig & Quill.Sqlite[SnakeCase], Nothing, DataService] =
    ZLayer.fromFunction(new DataService(_, _))

  final private case class MigrationStep(name: String, wf: IO[Throwable, Unit])

}
