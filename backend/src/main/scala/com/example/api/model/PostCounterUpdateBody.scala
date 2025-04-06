package com.example.api.model

import zio.json.{DeriveJsonCodec, JsonCodec}

final case class PostCounterUpdateBody(delta: Long)

object PostCounterUpdateBody {
  implicit val codec: JsonCodec[PostCounterUpdateBody] = DeriveJsonCodec.gen[PostCounterUpdateBody]
}
