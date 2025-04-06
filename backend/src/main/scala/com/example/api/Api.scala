package com.example.api

import com.example.api.model.*
import com.example.repo.{ChatRepo, CounterRepo}
import zio.*

import java.sql.Timestamp
import java.time.Instant

class Api(chatRepo: ChatRepo, counterRepo: CounterRepo) {

  private val counter: Ref[Long] =
    Unsafe.unsafe(implicit unsafe => Ref.unsafe.make(0L))

  private val counterId: Long = 69

  def getCounter: ApiResult[GetCounterResponse] =
    for {
      optCounter <- counterRepo
        .getCounter(counterId)
        .mapError(err => ApiError.InternalServerError("Error while retriving counter", Some(err)))
    } yield GetCounterResponse(optCounter.map(_.count).getOrElse(0))

  def postCounter(body: PostCounterUpdateBody): ApiResult[GetCounterResponse] =
    counterRepo
      .updateCounter(counterId, body.delta)
      .mapError(err => ApiError.InternalServerError("Error while retriving counter", Some(err)))
      .zipRight(getCounter)

  def getChat: ApiResult[GetChatResponse] =
    for {
      rows <- chatRepo
        .getMessagesAfter(new Timestamp(0))
        .mapError(err => ApiError.InternalServerError("Error while retriving messages", Some(err)))
      messages = rows.map(row => ChatMessage(row.name, row.message))
    } yield GetChatResponse(messages)

  def postChatMessage(body: PostChatMessageBody): ApiResult[GetChatResponse] =
    chatRepo
      .insertMessage(body.user, body.message)
      .mapError(err => ApiError.InternalServerError("Error while posting message", Some(err)))
      .zipRight(getChat)

}

object Api {

  val layer: ZLayer[ChatRepo & CounterRepo, Nothing, Api] =
    ZLayer.fromFunction(new Api(_, _))

}
