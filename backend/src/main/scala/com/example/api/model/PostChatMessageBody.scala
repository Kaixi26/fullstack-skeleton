package com.example.api.model

import zio.json.{DeriveJsonCodec, JsonCodec}

final case class PostChatMessageBody(user: String, message: String)

object PostChatMessageBody {
  implicit val codec: JsonCodec[PostChatMessageBody] = DeriveJsonCodec.gen[PostChatMessageBody]
}
