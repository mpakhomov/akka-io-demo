package com.mpakhomov.actors

import java.net.InetSocketAddress
import java.text.DecimalFormat
import java.time.format.DateTimeFormatter

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Tcp}
import akka.util.{ByteString, Timeout}
import com.mpakhomov.actors.CandlestickAggregatorActor.{GetDataForLastMinute, GetDataForLastNMinutes}
import com.mpakhomov.model.Candlestick

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration._

class ServerActor(val addr: InetSocketAddress, val candlestickAggregatorActor: ActorRef) extends Actor with ActorLogging {

  import ServerActor._
  import akka.io.Tcp._
  import akka.pattern.ask
  import context.system

  // a set of connected clients
  val clients = mutable.Set[ActorRef]()

  // for ask `?` syntax and futures
  implicit val timeout = Timeout(1.minutes)

  IO(Tcp) ! Bind(self, addr)

  def receive = {
    case CommandFailed(b: Bind) =>
      log.error("CommandFailed")
      context stop self
    case c @ Connected(remote, local) => handleNewConnection(sender())
    case SendDataForLastMinute => sendDataForLastMinute()
    case c: ConnectionClosed => handleClientDisconnected(sender())
  }

  def handleNewConnection(connection: ActorRef): Unit = {
    val connection = sender()
    connection ! Register(self)
    clients += connection
    log.info("New client connected. Sending data for the last 10 minutes")
    val future = (candlestickAggregatorActor ? GetDataForLastNMinutes).mapTo[Seq[Candlestick]]
    val data = Await.result(future, timeout.duration)
    val jsonStr = buildJsonStr(data)
    log.info(s"Sending to the client:\n$jsonStr")
    connection ! Write(ByteString(jsonStr))
  }

  def sendDataForLastMinute(): Unit = {
    val future = (candlestickAggregatorActor ? GetDataForLastMinute).mapTo[Seq[Candlestick]]
    val data = Await.result(future, timeout.duration)
    val jsonStr = buildJsonStr(data)
    log.info(s"Sending to the clients:\n$jsonStr")
    for (m <- clients) m ! Write(ByteString(jsonStr))
  }

  def handleClientDisconnected(connection: ActorRef): Unit = {
    log.info("Client disconnected")
    clients.remove(connection)
  }
}

object ServerActor {

  // messages
  case object SendDataForLastMinute

  def props(addr: InetSocketAddress, candlestickAggregator: ActorRef) =
    Props(classOf[ServerActor], addr, candlestickAggregator)

  def candleStick2Json(c: Candlestick): String = {
    // datetime format looks similar to ISO_INSTANT, but not exactly the same
    // to https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html
    val dt = c.timestamp.toLocalDateTime
    val dtf1 = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val dtf2 = DateTimeFormatter.ofPattern("HH:mm")
    val timestamp =  dt.format(dtf1) + "T" + dt.format(dtf2) + ":00Z"

    // just in case: format doubles exactly at it is in the spec
    val formatter = new DecimalFormat("#.#")
    val open = formatter.format(c.open)
    val low = formatter.format(c.low)
    val high = formatter.format(c.high)
    val close = formatter.format(c.close)
    s"""{ "ticker": "${c.ticker}", "timestamp": "${timestamp}", "open": ${open}, "high": ${high},""" +
    s""" "low": ${low}, "close": ${close}, "volume": ${c.volume} }"""
  }

  // I extracted it to a function, so that it's easier to verify in my integration testing
  def buildJsonStr(data: Seq[Candlestick]): String = data.map(candleStick2Json(_)).mkString("\n")
}
