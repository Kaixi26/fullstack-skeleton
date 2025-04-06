package com.example.repo.model

import java.sql.Timestamp

final case class MigrationRow(name: String, createdAt: Timestamp)
