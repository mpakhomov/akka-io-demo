package com.mpakhomov.actors

import akka.actor.{Actor, ActorLogging, Props}
import akka.util.ByteString

class EventProcessor extends Actor with ActorLogging {
  override def receive: Receive = {
    case data: ByteString => processEvent(data)
  }

  def processEvent(data: ByteString): Unit = {
    log.info(data.utf8String)
  }
}

object EventProcessor {
  def props(): Props = Props(classOf[EventProcessor])
}
