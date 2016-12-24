package com.mpakhomov.actors

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import com.mpakhomov.actors.CandlestickAggregatorActor.GetDataForLastNMinutes
import com.mpakhomov.actors.UpstreamClientActor.CloseCommand
import com.typesafe.config.ConfigFactory

class UpstreamClientActor(remote: InetSocketAddress, eventProcessor: ActorRef) extends Actor with ActorLogging {

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

object UpstreamClientActor {

  // messages
  case object CloseCommand

  def props(remote: InetSocketAddress, replies: ActorRef) =
    Props(classOf[UpstreamClientActor], remote, replies)
}
