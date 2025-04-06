package com.example

import com.example.api.Api
import com.example.repo.{ChatRepo, CounterRepo, DataService, QuillSqlite}
import zio.{Config, ConfigProvider, LogLevel, Scope, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer, http, logging}
import zio.logging.*

object Main extends ZIOAppDefault {

  private val logFilter: LogFilter.LogLevelByNameConfig =
    LogFilter.LogLevelByNameConfig(LogLevel.Debug)

  val developmentLogger: ZLayer[Any, Config.Error, Unit] = {
    val config = ConsoleLoggerConfig(
      LogFormat.default.spaced(LogFormat.allAnnotations) + LogFormat.cause,
      logFilter,
    )
    consoleLogger(config)
  }

  val productionLogger: ZLayer[Any, Config.Error, Unit] = {
    val config = ConfigProvider.fromMap(
      Map(
        "logger/format" -> "%label{timestamp}{%timestamp{yyyy-MM-dd'T'HH:mm:ssZ}} %label{level}{%level} %label{fiberId}{%fiberId} %label{message}{%message} %label{cause}{%cause} %label{name}{%name} %kvs",
        "logger/filter/rootLevel" -> LogLevel.Info.label,
      ),
      "/",
    )
    zio.Runtime.setConfigProvider(config) >>> consoleJsonLogger()
  }

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    zio.Runtime.removeDefaultLoggers >>> developmentLogger

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    program
      .provide(
        ApplicationConfig.layer,
        QuillSqlite.layer,
        DataService.layer,
        CounterRepo.layer,
        ChatRepo.layer,
        http.Client.default,
        Api.layer,
        Server.layer,
      )

  private def program: ZIO[DataService & Server, Throwable, Unit] =
    for {
      _ <- ZIO.serviceWithZIO[DataService](_.up)
      _ <- ZIO.serviceWithZIO[Server](_.serve)
    } yield ()

}
