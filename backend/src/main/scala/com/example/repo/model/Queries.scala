package com.example.repo.model

import io.getquill._

trait Queries {

  inline def qCounter: EntityQuery[CounterRow] = querySchema[CounterRow]("counter")

  inline def qMessage: EntityQuery[MessageRow] = querySchema[MessageRow]("message")

  inline def qMigration: EntityQuery[MigrationRow] = querySchema[MigrationRow]("migration")

  inline def qInsertMigration: EntityQuery[MigrationRow] = querySchema[MigrationRow]("migration")

}
