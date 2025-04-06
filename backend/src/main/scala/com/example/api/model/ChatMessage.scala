package com.example.api.model

import zio.json.{DeriveJsonCodec, JsonCodec}

final case class ChatMessage(user: String, message: String)

object ChatMessage {
  implicit val codec: JsonCodec[ChatMessage] = DeriveJsonCodec.gen[ChatMessage]
}
