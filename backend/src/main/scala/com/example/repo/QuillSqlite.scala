package com.example.repo

import com.example.ApplicationConfig
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio.{ZIO, ZLayer}

import javax.sql.DataSource
import scala.util.Try

object QuillSqlite {

  val layer: ZLayer[ApplicationConfig, Throwable, Quill.Sqlite[SnakeCase]] =
    ZLayer
      .fromFunction { (c: ApplicationConfig) =>
        Quill.DataSource.fromJdbcConfig(c.database)
      }
      .flatten
      .andTo {
        ZLayer.fromZIO {
          for {
            ds <- ZIO.service[DataSource]
            _ <- ZIO.fromTry {
              Try {
                val stmt = ds.getConnection.createStatement()
                stmt.execute("PRAGMA journal_mode=WAL")
                stmt.execute("PRAGMA foreign_keys=ON")
                stmt.execute("PRAGMA cache_size=2000")
                stmt.execute("PRAGMA synchronous=NORMAL")
                stmt.execute("PRAGMA mmap_size=134217728")
                stmt.execute("PRAGMA busy_timeout=5000")
                stmt.execute("PRAGMA journal_size_limit=67108864")
                stmt.close()
              }
            }
          } yield ds
        }
      }
      .andTo(Quill.Sqlite.fromNamingStrategy(SnakeCase))


}
