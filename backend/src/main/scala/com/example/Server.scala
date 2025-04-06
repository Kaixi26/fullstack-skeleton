package com.example

import com.example.Server.*
import com.example.api.ApiResult.*
import com.example.api.model.*
import com.example.api.{Api, ApiError, ApiResult}
import zio.http.*
import zio.http.endpoint.openapi.OpenAPI.SecurityScheme.Http
import zio.json.{DecoderOps, JsonCodec, JsonDecoder}
import zio.metrics.{Metric, MetricLabel}
import zio.metrics.Metric.Counter
import zio.metrics.MetricKeyType.Histogram
import zio.metrics.connectors.prometheus.PrometheusPublisher
import zio.{IO, ZIO, ZLayer, http}

import java.util.UUID
import scala.annotation.unused

class Server(
    config: ApplicationConfig,
    @unused client: http.Client,
    prometheusPublisher: PrometheusPublisher,
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
    Method.GET / trailing -> handler {
      for {
        path <- Handler.param[(Path, Request)](_._1)
        file <- Handler.getResourceAsFile((http.Path("static") ++ path).encode)
        http <- Handler.param[(Path, Request)](_._2).andThen {
          if (file.isFile) {
            Handler.fromFile(file)
          } else {
            Handler.getResourceAsFile("static/index.html").flatMap(Handler.fromFile(_))
          }
        }
      } yield http
    }
  )
    .mapError(err => ApiError.InternalServerError("An error occured while serving static pages", Some(err)))

  private val apiRoutes: Routes[Any, ApiError] = counterRoutes ++ chatRoutes ++ apiFallback

  private val appRoutes: Routes[Any, Nothing] =
    middleware(collectRequestMetrics(sandboxApiErrors(apiRoutes ++ frontend)))

  private val metricRoutes: Routes[Any, Nothing] = Routes(
    Method.GET / "metrics" -> Handler.fromResponseZIO(prometheusPublisher.get.map(Response.text))
  )

  val serve: IO[Throwable, Nothing] = for {
    _ <- http.Server
      .serve(metricRoutes)
      .provide(http.Server.defaultWith(c => c.port(config.prometheus.port)))
      .fork
    never <- http.Server
      .serve[Any](appRoutes)
      .provide(http.Server.defaultWith(c => c.port(config.port).hybridRequestStreaming(1024 * 100)))
  } yield never
}

object Server {
  val layer: ZLayer[ApplicationConfig & Client & Api & PrometheusPublisher, Nothing, Server] =
    ZLayer.fromFunction(new Server(_, _, _, _))

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

  private val requestCount: Counter[Long] = Metric.counter("requestCount")

  val requestDuration: Metric.Histogram[Double] =
    Metric.histogram(
      "requestDuration",
      // Up to ~1sec
      Histogram.Boundaries.exponential(1.0d, 2.0d, 11)
    )

  private def collectRequestMetrics[Env, Err](routes: Routes[Env, Err]): Routes[Env, Err] =
    Routes(
      routes.routes.map { route =>
        val method = route.routePattern.method
        val path = route.routePattern.pathCodec.toString
        val tags = Set(MetricLabel("path", path), MetricLabel("method", method.toString))
        route.transform { hndlr =>
          for {
            start <- Handler.fromZIO(zio.Clock.instant)
            result <- hndlr
            _ <- Handler.fromZIO {
              for {
                end <- zio.Clock.instant
                duration = java.time.Duration.between(start, end)
                _ <- requestCount.tagged(tags).increment
                _ <- requestDuration.tagged(tags).update(duration.toMillis.toDouble)
              } yield ()
            }
          } yield result
        }
      }
    )

  private def middleware: Middleware[Any] =
    Middleware.logAnnotate("request_id", UUID.randomUUID().toString) ++
      Middleware.dropTrailingSlash ++
      Middleware.requestLogging(logRequestBody = false, logResponseBody = false) ++
      HandlerAspect.customAuthProvidingZIO(res => ZIO.some(0))

  private def sandboxApiErrors[Env](routes: Routes[Env, ApiError]): Routes[Env, Nothing] =
    routes
      .tapErrorZIO(err => err.log)
      .handleError(err => err.asResponse)
}
