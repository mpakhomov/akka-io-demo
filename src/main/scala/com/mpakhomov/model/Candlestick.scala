package com.mpakhomov.model

import java.sql.Timestamp

// this class represents a candlestick. this what we parse incoming messages to.
// candlesticks are aggregated and sent to the clients
case class Candlestick(
  ticker: String,
  timestamp: Timestamp,
  open: Double,
  high: Double,
  low: Double,
  close: Double,
  volume: Long
)

