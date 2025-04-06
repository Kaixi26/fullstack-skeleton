package com.example.api.model

import zio.json.{DeriveJsonCodec, JsonCodec}

final case class GetChatResponse(messages: List[ChatMessage])

object GetChatResponse {
  implicit val codec: JsonCodec[GetChatResponse] = DeriveJsonCodec.gen[GetChatResponse]
}
