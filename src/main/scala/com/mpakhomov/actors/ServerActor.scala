package com.mpakhomov.actors

import java.net.InetSocketAddress
import java.time.format.DateTimeFormatter

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Tcp}
import akka.util.{ByteString, Timeout}
import com.mpakhomov.actors.CandlestickAggregatorActor.{GetDataForLastMinute, GetDataForLastNMinutes}
import com.mpakhomov.model.Candlestick

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration._

class ServerActor(val addr: InetSocketAddress, val candlestickAggregatorActor: ActorRef) extends Actor with ActorLogging {

  import ServerActor._
  import akka.io.Tcp._
  import akka.pattern.ask
  import context.system

  // list of connected clients
  val clients = ListBuffer[ActorRef]()

  // for ask `?` syntax and futures
  implicit val timeout = Timeout(30.minutes)

  IO(Tcp) ! Bind(self, addr)

  def receive = {
    case CommandFailed(b: Bind) =>
      log.error("CommandFailed")
      context stop self
    case c @ Connected(remote, local) => handleNewConnection(sender())
    case SendDataForLastOneMinute => sendDataForLastMinute()
  }

  def handleNewConnection(connection: ActorRef): Unit = {
    val connection = sender()
    connection ! Register(self)
    clients += connection
    log.info("New client connected. Sending data for the last 10 minutes")
    val future = (candlestickAggregatorActor ? GetDataForLastNMinutes).mapTo[Seq[Candlestick]]
    val data = Await.result(future, timeout.duration)
    val jsonStr = data.map(candleStick2Json(_)).mkString("\n")
    log.info(s"Sending to the client:\n$jsonStr")
    connection ! Write(ByteString(jsonStr))
  }

  def sendDataForLastMinute(): Unit = {
    val future = (candlestickAggregatorActor ? GetDataForLastMinute).mapTo[Seq[Candlestick]]
    val data = Await.result(future, timeout.duration)
    val jsonStr = data.map(candleStick2Json(_)).mkString("\n")
    log.info(s"Sending to the clients:\n$jsonStr")
    for (m <- clients) m ! Write(ByteString(jsonStr))
  }
}

object ServerActor {

  // messages
  case object SendDataForLastOneMinute

  def props(addr: InetSocketAddress, candlestickAggregator: ActorRef) =
    Props(classOf[ServerActor], addr, candlestickAggregator)

  def candleStick2Json(c: Candlestick): String = {
    // datetime format looks similar to ISO_INSTANT, but not exactly the same
    // to https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html
    val dt = c.timestamp.toLocalDateTime
    val dtf1 = DateTimeFormatter.ofPattern("YYYY-MM-dd")
    val dtf2 = DateTimeFormatter.ofPattern("mm:ss")
    val timestamp =  dt.format(dtf1) + "T" + dt.format(dtf2) + ":00Z"
    s"""{ "ticker": ${c.ticker}, "timestamp": ${timestamp}, "open": ${c.open}, "high": ${c.high}""" +
    s""" "low": ${c.low}, "close": ${c.close}, "volume": ${c.volume} }"""
  }
}
