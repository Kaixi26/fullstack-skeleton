package com.example.repo.model

import java.sql.Timestamp

case class MessageRow(id: Long, name: String, message: String, createdAt: Timestamp)
