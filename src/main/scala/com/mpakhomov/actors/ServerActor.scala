package com.mpakhomov.actors

import java.net.InetSocketAddress
import java.nio.ByteBuffer

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Tcp}
import akka.util.{ByteString, Timeout}
import com.mpakhomov.actors.CandlestickAggregatorActor.GetDataForLastNMinutes
import com.mpakhomov.actors.ServerActor.SendDataForLastOneMinute
import com.mpakhomov.model.Candlestick

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class ServerActor(val addr: InetSocketAddress, val candlestickAggregatorActor: ActorRef) extends Actor with ActorLogging {

  import akka.io.Tcp._
  import context.system
  import akka.pattern.ask
  import spray.json._
  import com.mpakhomov.json.MyJsonProtocol._

  val clients = ListBuffer[ActorRef]()

  IO(Tcp) ! Bind(self, addr)

  def receive = {
    case CommandFailed(b: Bind) =>
      log.error(s"CommandFailed: ${b}")
      context stop self

    case c @ Connected(remote, local) => handleConnection(sender())
    case Received(data) => {
      sender() ! Write(data)
      for (m <- clients) m ! Write(data)
    }
    case SendDataForLastOneMinute => {
      log.info("writing to a client!!!")
//      Tcp.Write(ByteString("Hello from server!"))
//      sender() ! Write(ByteString("Hello from server!"))
      for (m <- clients) m ! Write(ByteString("SendDataForLastOneMinute: Hello from server!"))
    }
  }

  def handleConnection(connection: ActorRef): Unit = {
    val connection = sender()
    connection ! Register(self)
    clients += connection
//    implicit val timeout = Timeout(5.seconds) // needed for `?` below
//    val future: Future[Candlestick] = (candlestickAggregatorActor ? GetDataForLastNMinutes).mapTo[Candlestick]
//    val data = Await.result(future, 3.seconds)
//
//    val json = data.toJson
//    log.info(s"$data")
//    Tcp.Write(ByteString("handleConnection: Hello from server!"))
    for (m <- clients) m ! Write(ByteString("handleConnection: Hello from server!"))
  }
}

object ServerActor {

  // messages
  case object SendDataForLastOneMinute

  def props(addr: InetSocketAddress, candlestickAggregator: ActorRef) =
    Props(classOf[ServerActor], addr, candlestickAggregator)
}
