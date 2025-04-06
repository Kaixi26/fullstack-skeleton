package com.example

import com.example.ApplicationConfig.PrometheusConfig
import com.typesafe.config.{Config, ConfigFactory}
import io.getquill.JdbcContextConfig
import zio.{ZIO, ZLayer}

import scala.util.Try

class ApplicationConfig private (config: Config) {
  val port: Int = config.getInt("port")
  val database: JdbcContextConfig = JdbcContextConfig(config.getConfig("database"))
  val prometheus: PrometheusConfig = PrometheusConfig(config.getConfig("prometheus"))

}

object ApplicationConfig {

  final class PrometheusConfig(config: Config) {
    val port: Int = config.getInt("port")
  }

  val layer: ZLayer[Any, Throwable, ApplicationConfig] = ZLayer.fromZIO {
    ZIO.fromTry {
      Try {
        val cfg = ConfigFactory.load()
        new ApplicationConfig(cfg)
      }
    }
  }

}
