package com.mpakhomov.actors

import akka.actor.{Actor, ActorLogging, Props}
import com.mpakhomov.actors.CandlestickAggregatorActor.{GetDataForLastMinute, GetDataForLastNMinutes}
import com.mpakhomov.collection.CircularBuffer
import com.mpakhomov.model.Candlestick
import model.UpstreamMessage

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class CandlestickAggregatorActor(val keepDataMinutes: Int = 10) extends Actor with ActorLogging {

  // type alias for convenience
  type Candlesticks = mutable.LinkedHashMap[String, Candlestick]

  // our requirement is to keep data for the last 10 minutes. Circular Buffer (Ring Buffer) is a good fit.
  // it keeps data only for the last 10 minutes. when it runs out of space, old data gets overwritten with
  // new data. it means that we don't need to implement any kind of eviction policy to get rid of old data
  val ringBuffer = new CircularBuffer[Candlesticks](keepDataMinutes)
  // add an empty container, so that ring buffer is never empty
  ringBuffer.offer(new Candlesticks)

  override def receive: Receive = {
    case msg: UpstreamMessage => processNewMessage(msg)
    case GetDataForLastNMinutes => sender() ! getDataForLastNMinutes()
    case GetDataForLastMinute =>
      sender() ! getDataForLastMinute()
      // one minute passed. let's add a container to keep data for the next minute
      ringBuffer.offer(new Candlesticks)
  }

  // process new incoming message from upstream tcp server. convert the message to candlestick format
  // and aggregate it (store it in the ring buffer)
  def processNewMessage(msg: UpstreamMessage): Unit = {
    val ringBufferLastElemIndex = ringBuffer.size() - 1
    val candlesticks = ringBuffer.get(ringBufferLastElemIndex)
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

  // when new client connects to the server we should serve candlesticks for the last N minutes (10, by default)
  def getDataForLastNMinutes(): Seq[Candlestick] = {
    import scala.collection.JavaConverters._
    val buf = new ArrayBuffer[Candlestick]()
    for (c <- ringBuffer.asScala) buf ++= c.values
    buf
  }

  // every minute we should serve to the clients all candlesticks for the last minute
  def getDataForLastMinute(): Seq[Candlestick] = ringBuffer.get(ringBuffer.size() - 1).values.toSeq
}

object CandlestickAggregatorActor {

  // messages
  case class ProcessNewMessage(msg: UpstreamMessage)
  case object GetDataForLastNMinutes
  case object GetDataForLastMinute

  def props(keepDataMinutes: Int): Props = Props(new CandlestickAggregatorActor(keepDataMinutes))
}
