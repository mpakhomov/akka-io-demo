package com.mpakhomov.actors

import java.sql.Timestamp

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.ByteString
import model.UpstreamMessage

class EventProcessor(val candlestickAggregator: ActorRef) extends Actor with ActorLogging {
  override def receive: Receive = {
    case data: ByteString => processEvent(data)
  }

  def processEvent(data: ByteString): Unit = {
    try {
      val msg = parse(data)
      candlestickAggregator ! msg
    } catch {
      case ex: Throwable => log.error(ex, s"Error parsing message: ${ex.getMessage}")
    }
  }

  def parse(data: ByteString) = {
    val len = data.take(2).asByteBuffer.getShort
    // get actual message, drop first 2 bytes which contain the length of the message
    val msg = data.drop(2).take(len)
    val ts = new Timestamp(msg.slice(0, 8).asByteBuffer.getLong)
    val tickerLen = msg.slice(8, 10).asByteBuffer.getShort
    val ticker = msg.slice(10, 10 + tickerLen).utf8String
    val priceStart = 10 + tickerLen
    val price: Double = msg.slice(priceStart, priceStart + 8).asByteBuffer.getDouble
    val sizeStart = priceStart + 8
    val size: Int = msg.slice(sizeStart, sizeStart + 4).asByteBuffer.getInt
    log.info(s"$ts $ticker $price $size")
    UpstreamMessage(truncateTimestamp(ts), ticker, price, size)
  }

  // truncate timestamp: discard its seconds part
  def truncateTimestamp(ts: Timestamp): Timestamp =
    new Timestamp(ts.getTime - (ts.getTime % (60 * 1000)))

}

object EventProcessor {
  def props(candlestickAggregator: ActorRef): Props = Props(new EventProcessor(candlestickAggregator))
}
