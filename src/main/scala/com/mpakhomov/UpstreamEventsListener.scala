package com.mpakhomov

import java.net.InetSocketAddress

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.ByteString

class UpstreamEventsListener extends Actor with ActorLogging {
  override def receive: Receive = {
    case data: ByteString =>
      log.info(data.utf8String)
    case msg: String =>
      log.info(msg)
//    case x: Any =>
//      log.info(s"Unknown message: $x")
  }
}

object UpstreamEventsListener {
  def props(): Props = Props(classOf[UpstreamEventsListener])
}
