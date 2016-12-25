package model

import java.sql.Timestamp

// this class represents incoming message from upstream tcp server
case class UpstreamMessage(
  ts: Timestamp,
  ticker: String,
  price: Double,
  size: Int
)
