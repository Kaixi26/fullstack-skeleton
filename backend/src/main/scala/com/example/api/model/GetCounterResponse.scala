package com.example.api.model

import zio.json.{DeriveJsonCodec, JsonCodec}

final case class GetCounterResponse(count: Long)

object GetCounterResponse {
  implicit val codec: JsonCodec[GetCounterResponse] = DeriveJsonCodec.gen[GetCounterResponse]
}
