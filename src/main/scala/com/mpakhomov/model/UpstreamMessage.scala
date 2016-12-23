package model

import java.sql.Timestamp

case class UpstreamMessage(
  ts: Timestamp,
  ticker: String,
  price: Double,
  size: Int
)
