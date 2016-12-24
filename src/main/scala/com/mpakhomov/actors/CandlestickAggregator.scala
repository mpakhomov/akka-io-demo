package com.mpakhomov.actors

import akka.actor.{Actor, ActorLogging, Props}
import com.mpakhomov.actors.CandlestickAggregator.{GetDataForLastMinute, GetDataForLastNMinutes}
import com.mpakhomov.collection.CircularBuffer
import com.mpakhomov.model.Candlestick
import model.UpstreamMessage

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class CandlestickAggregator(val keepDataMinutes: Int = 10) extends Actor with ActorLogging {

  // type alias for convenience
  type Candlesticks = mutable.LinkedHashMap[String, Candlestick]

  val ringBuffer = new CircularBuffer[Candlesticks](keepDataMinutes)

  override def receive: Receive = {
    case msg: UpstreamMessage => processNewMessage(msg)
    case GetDataForLastNMinutes => getDataForLastNMinutes()
    case GetDataForLastMinute => getDataForLastMinute()
  }

  def processNewMessage(msg: UpstreamMessage): Unit = {
    val candlesticks = ringBuffer.get(keepDataMinutes - 1)
    if (!candlesticks.contains(msg.ticker)) {
      candlesticks(msg.ticker) = Candlestick(ticker = msg.ticker, timestamp = msg.ts, open = msg.price,
        high = msg.price, low = msg.price, close = msg.price, volume = msg.size)
    } else {
      val oldCandlestick = candlesticks(msg.ticker)
      candlesticks(msg.ticker) = Candlestick(ticker = msg.ticker, timestamp = msg.ts, open = oldCandlestick.open,
        high = if (oldCandlestick.high < msg.price) msg.price else oldCandlestick.high,
        low = if (oldCandlestick.low > msg.price) msg.price else oldCandlestick.low,
        close = msg.price,
        volume = oldCandlestick.volume + msg.size)
    }
  }

  def getDataForLastNMinutes(): Seq[Candlestick] = {
    import scala.collection.JavaConverters._
    val buf = new ArrayBuffer[Candlestick]()
    for (c <- ringBuffer.asScala) buf ++= c.values
    buf
  }

  def getDataForLastMinute(): Seq[Candlestick] = ringBuffer.get(keepDataMinutes - 1).values.toSeq
}

object CandlestickAggregator {

  case class ProcessNewMessage(msg: UpstreamMessage)
  case object GetDataForLastNMinutes
  case object GetDataForLastMinute

  def props(keepDataMinutes: Int): Props = Props(new CandlestickAggregator(keepDataMinutes))
}
