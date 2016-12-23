package com.mpakhomov.actors

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import com.mpakhomov.actors.UpstreamClient.CloseCommand
import com.typesafe.config.ConfigFactory

class UpstreamClient(remote: InetSocketAddress, eventProcessor: ActorRef) extends Actor with ActorLogging {

  import Tcp._
  import context.system

  IO(Tcp) ! Connect(remote)

  def receive = {
    case CommandFailed(_: Connect) =>
      log.error("connect failed")
      context stop self

    case c @ Connected(remote, local) =>
      eventProcessor ! c
      val connection = sender()
      connection ! Register(self)
      context become {
        case data: ByteString =>
          connection ! Write(data)
        case CommandFailed(w: Write) =>
          // O/S buffer was full
          log.error("write failed")
        case Received(data) =>
          eventProcessor ! data
        case CloseCommand =>
          connection ! Close
        case _: ConnectionClosed =>
          log.info("Connection closed")
          context stop self
      }
  }
}

object UpstreamClient {

  case object CloseCommand

  def props(remote: InetSocketAddress, replies: ActorRef) =
    Props(classOf[UpstreamClient], remote, replies)

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    val system = ActorSystem("akka-io-demo")
    val candlestickAggregator = system.actorOf(CandlestickAggregator.props(config.getInt("app.keep-data-minutes")))
    val eventProcessor = system.actorOf(EventProcessor.props(candlestickAggregator))
    val inetSocketAddress = new InetSocketAddress(
      config.getString("app.upstream.hostname"),
      config.getInt("app.upstream.port"))
    val upstreamClient = system.actorOf(UpstreamClient.props(inetSocketAddress, eventProcessor))
  }

}
