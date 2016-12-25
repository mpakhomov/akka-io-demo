package com.mpakhomov.actors

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, Props}
import akka.io.{IO, Tcp}

class ClientActor(remote: InetSocketAddress) extends Actor with ActorLogging {

  import Tcp._
  import context.system

  IO(Tcp) ! Connect(remote)

  def receive = {
    case CommandFailed(_: Connect) =>
      log.error("connect failed")
      context stop self
    case c @ Connected(remote, local) =>
      val connection = sender()
      connection ! Register(self)
      context become {
        case CommandFailed(w: Write) =>
          // O/S buffer was full
          log.error("write failed")
        case Received(data) =>
          log.info(data.utf8String)
        case _: ConnectionClosed =>
          log.info("Connection closed")
          context stop self
      }
  }
}

object ClientActor {

  def props(remote: InetSocketAddress) =
    Props(classOf[ClientActor], remote)

}
