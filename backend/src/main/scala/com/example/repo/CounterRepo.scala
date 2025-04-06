package com.example.repo

import com.example.repo.model.{CounterRow, Queries}
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.{IO, ZLayer}

import java.sql.SQLException

final class CounterRepo(quill: Quill.Sqlite[SnakeCase]) extends Queries {

  import quill.*

  def getCounter(id: Long): IO[SQLException, Option[CounterRow]] = run {
    qCounter.filter(_.id == lift(id))
  }
    .map(_.headOption)

  def updateCounter(id: Long, delta: Long): IO[SQLException, Unit] = run {
    qCounter
      .insertValue(lift(CounterRow(id, delta)))
      .onConflictUpdate(_.id)((t, e) => t.count -> (t.count + lift(delta)))
  }.unit

}

object CounterRepo {
  val layer: ZLayer[Quill.Sqlite[SnakeCase], Nothing, CounterRepo] =
    ZLayer.fromFunction(new CounterRepo(_))
}
