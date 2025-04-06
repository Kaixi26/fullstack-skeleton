package com.example.api

import com.example.api.ApiError.*
import zio.http.*
import zio.json.{DeriveJsonCodec, EncoderOps, JsonCodec}
import zio.{Cause, LogLevel, UIO, ZIO}

trait ApiError {
  def status: Status
  def message: String
  def cause: Option[Throwable] = None
  def logLevel: LogLevel = if (status.isServerError) LogLevel.Error else LogLevel.Warning
  def asResponse: Response =
    Response(
      status,
      Headers(Header.ContentType(MediaType.application.json).untyped),
      Body.fromCharSequence(ApiErrorResponseBody(message, cause.map(_.getMessage)).toJson),
    )

  def log: UIO[Unit] =
    ZIO.logLevel(logLevel) {
      cause match {
        case Some(err) => ZIO.logCause(message, Cause.die(err))
        case None => ZIO.log(message)
      }
    }

}

object ApiError {

  private case class ApiErrorResponseBody(message: String, cause: Option[String])

  private object ApiErrorResponseBody {
    implicit val codec: JsonCodec[ApiErrorResponseBody] = DeriveJsonCodec.gen[ApiErrorResponseBody]
  }

  object NotFound extends ApiError {
    override def status: Status = Status.NotFound
    override def message: String = "Route not found."
  }

  class BadRequest(override val message: String, override val cause: Option[Throwable] = None) extends ApiError {
    override def status: Status = Status.BadRequest
  }

  class InternalServerError(override val message: String, override val cause: Option[Throwable] = None)
      extends ApiError {
    override def status: Status = Status.InternalServerError
  }

}
