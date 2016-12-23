package com.mpakhomov.model

import java.sql.Timestamp

case class Candlestick(
  ticker: String,
  timestamp: Timestamp,
  open: Double,
  high: Double,
  low: Double,
  close: Double,
  volume: Long
)

