package com.mpakhomov

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import com.mpakhomov.actors._
import com.typesafe.config.ConfigFactory

object ClientApp {

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    val system = ActorSystem("akka-io-demo")
    val inetSocketAddress = new InetSocketAddress("localhost", config.getInt("app.server.port"))
    val client = system.actorOf(ClientActor.props(inetSocketAddress))
  }
}