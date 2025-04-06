package com.example.api

import zio.{IO, ZIO, http}
import zio.json.{EncoderOps, JsonEncoder}

type ApiResult[R] = IO[ApiError, R]

object ApiResult {

  implicit final class ApiResultAsResult[R: JsonEncoder](val underlying: ApiResult[R]) {
    def asResponse: IO[ApiError, http.Response] =
      underlying.either.flatMap {
        case Left(err)       => ZIO.fail(err)
        case Right(response) => ZIO.succeed(http.Response.json(response.toJson))
      }
  }
}
