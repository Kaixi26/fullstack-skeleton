package com.example

import com.example.Server.*
import com.example.api.model.*
import com.example.api.{Api, ApiError, ApiResult}
import com.example.api.ApiResult.*
import zio.http.*
import zio.http.ChannelEvent.UserEvent
import zio.json.{DecoderOps, JsonCodec, JsonDecoder}
import zio.{Cause, IO, ZIO, ZLayer, http}

import java.util.UUID
import scala.annotation.unused

class Server(
    config: ApplicationConfig,
    @unused client: http.Client,
    api: Api,
) {

  private val counterRoutes =
    Routes(
      Method.GET / "api" / "counter" -> http.Handler.fromResponseZIO(api.getCounter.asResponse),
      Method.POST / "api" / "counter" ->
        handler { (request: Request) =>
          withBody(request)((body: PostCounterUpdateBody) => api.postCounter(body).asResponse)
        },
    )

  private val chatRoutes: Routes[Any, ApiError] =
    Routes(
      Method.GET / "api" / "chat" -> http.Handler.fromResponseZIO(api.getChat.asResponse),
      Method.POST / "api" / "chat" / "message" ->
        handler { (request: Request) =>
          withBody(request)((body: PostChatMessageBody) => api.postChatMessage(body).asResponse)
        },
    )

  private val apiFallback: Routes[Any, ApiError] = Routes(
    Method.ANY / "api" / trailing -> handler(ZIO.fail(ApiError.NotFound))
  )

  private val frontend: Routes[Any, ApiError] = Routes(
    Method.GET / "static" / trailing -> handler {
      for {
        path <- Handler.param[(Path, Request)](_._1)
        file <- Handler.getResourceAsFile((http.Path("static") ++ path).encode)
        http <- Handler.param[(Path, Request)](_._2).andThen {
          if (file.isFile) Handler.fromFile(file) else Handler.notFound
        }
      } yield http
    },
    Method.GET / trailing -> handler {
      for {
        path <- Handler.param[(Path, Request)](_._1)
        file <- Handler.getResourceAsFile("static/index.html")
        http <- Handler.fromFile(file)
      } yield http
    },
  )
    .mapError(err => ApiError.InternalServerError("An error occured while serving static pages", Some(err)))

  private val apiRoutes: Routes[Any, ApiError] = counterRoutes ++ chatRoutes ++ apiFallback

  private val appRoutes: Routes[Any, Nothing] =
    middleware(sandboxApiErrors(apiRoutes ++ frontend))

  private def middleware: Middleware[Any] =
    Middleware.logAnnotate("request_id", UUID.randomUUID().toString) ++
      Middleware.dropTrailingSlash ++
      Middleware.requestLogging(logRequestBody = false, logResponseBody = false) ++
      HandlerAspect.customAuthProvidingZIO(res => ZIO.some(0))

  private def sandboxApiErrors[Env](routes: Routes[Env, ApiError]): Routes[Env, Nothing] =
    routes
      .tapErrorZIO(err => err.log)
      .handleError(err => err.asResponse)

  val serve: IO[Throwable, Nothing] =
    http.Server
      .serve[Any](appRoutes)
      .provide(
        http.Server.defaultWith(c => c.port(config.port).hybridRequestStreaming(1024 * 100))
      )
}

object Server {
  val layer: ZLayer[ApplicationConfig & Client & Api, Nothing, Server] =
    ZLayer.fromFunction(new Server(_, _, _))

  def extractBody[Body: JsonCodec]: HandlerAspect[Any, Body] = HandlerAspect.interceptIncomingHandler {
    Handler.fromFunctionZIO[Request] { request =>
      val wf: IO[ApiError, Body] =
        for {
          _ <- ZIO.when(!request.hasJsonContentType)(ZIO.fail(ApiError.BadRequest("Expected json content type.")))
          body <- request.body
            .asString(Charsets.Utf8)
            .mapError(cause => ApiError.BadRequest("Could not get body.", Some(cause)))
          b <- ZIO
            .fromEither(body.fromJson[Body])
            .mapError(cause => ApiError.BadRequest(s"Failed parsing body: `$cause`"))
        } yield b

      wf.mapBoth(_.asResponse, body => (request, body))
    }
  }

  private def withBody[Body: JsonDecoder, R](request: Request)(fn: Body => ApiResult[R]): ApiResult[R] =
    for {
      _ <- ZIO.when(!request.hasJsonContentType)(ZIO.fail(ApiError.BadRequest("Expected json content type.")))
      body <- request.body
        .asString(Charsets.Utf8)
        .mapError(cause => ApiError.BadRequest("Could not get body.", Some(cause)))
      b <- ZIO
        .fromEither(body.fromJson[Body])
        .mapError(cause => ApiError.BadRequest(s"Failed parsing body: `$cause`"))
      result <- fn(b)
    } yield result

}
