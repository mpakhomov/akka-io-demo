package com.mpakhomov

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import java.net.InetSocketAddress

import akka.actor.Actor.Receive
import akka.event.Logging

class UpstreamClient(remote: InetSocketAddress, listener: ActorRef) extends Actor with ActorLogging {

  import Tcp._
  import context.system

  IO(Tcp) ! Connect(remote)

  def receive = {
    case CommandFailed(_: Connect) =>
      listener ! "connect failed"
      context stop self

    case c @ Connected(remote, local) =>
      listener ! c
      val connection = sender()
      connection ! Register(self)
      context become {
        case data: ByteString =>
          connection ! Write(data)
        case CommandFailed(w: Write) =>
          // O/S buffer was full
          listener ! "write failed"
        case Received(data) =>
          listener ! data
        case "close" =>
          connection ! Close
        case _: ConnectionClosed =>
          listener ! "connection closed"
          context stop self
      }
  }
}

object UpstreamClient {

  def props(remote: InetSocketAddress, replies: ActorRef) =
    Props(classOf[UpstreamClient], remote, replies)

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("akka-io-demo")
    val listener = system.actorOf(UpstreamEventsListener.props())
    val inetSocketAddress = new InetSocketAddress("localhost", 5555)
    val upstreamClient = system.actorOf(UpstreamClient.props(inetSocketAddress, listener))
  }

}
