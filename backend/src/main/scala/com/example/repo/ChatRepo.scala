package com.example.repo

import com.example.repo.model.{CounterRow, MessageRow, Queries}
import io.getquill.*
import io.getquill.extras.SqlTimestampOps
import io.getquill.jdbczio.Quill
import zio.{IO, ZLayer}

import java.sql.{SQLException, Timestamp}
import java.time.Instant

final class ChatRepo(quill: Quill.Sqlite[SnakeCase]) extends Queries {

  import quill.*

  def getMessagesAfter(timestamp: Timestamp): IO[SQLException, List[MessageRow]] = run {
    qMessage
      .filter(_.createdAt > lift(timestamp))
      .sortBy(_.createdAt)(Ord.asc)
  }

  def getMessagesBefore(timestamp: Timestamp): IO[SQLException, List[MessageRow]] = run {
    qMessage
      .filter(_.createdAt < lift(timestamp))
      .sortBy(_.createdAt)(Ord.desc)
  }

  def insertMessage(name: String, message: String): IO[SQLException, Long] = {
    implicit inline def messageInsertMeta: InsertMeta[MessageRow] = insertMeta[MessageRow](_.id)
    val row = MessageRow(0, name, message, Timestamp.from(Instant.now()))
    run {
      qMessage.insertValue(lift(row)).returning(_.id)
    }
  }

}

object ChatRepo {
  val layer: ZLayer[Quill.Sqlite[SnakeCase], Nothing, ChatRepo] =
    ZLayer.fromFunction(new ChatRepo(_))
}
