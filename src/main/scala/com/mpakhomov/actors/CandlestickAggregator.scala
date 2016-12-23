package com.mpakhomov.actors

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, Props}
import com.mpakhomov.collection.CircularBuffer
import com.mpakhomov.model.Candlestick
import model.UpstreamMessage

import scala.collection.mutable.ListBuffer

class CandlestickAggregator(val keepDataMinutes: Int = 10) extends Actor with ActorLogging {

  type Candlesticks = ListBuffer[Candlestick]

  val ringBuffer = new CircularBuffer[Candlesticks](keepDataMinutes)

  override def receive: Receive = {
    case msg: UpstreamMessage => processNewMessage(msg)
  }

  def processNewMessage(msg: UpstreamMessage): Unit = {
    log.info(s"$msg")
  }
}

object CandlestickAggregator {

  case class ProcessNewMessage(msg: UpstreamMessage)

  def props(keepDataMinutes: Int): Props = Props(new CandlestickAggregator(keepDataMinutes))
}
