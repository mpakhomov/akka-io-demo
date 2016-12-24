package com.mpakhomov

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import com.mpakhomov.actors.CandlestickAggregatorActor.GetDataForLastNMinutes
import com.mpakhomov.actors.ServerActor.SendDataForLastOneMinute
import com.mpakhomov.actors.{CandlestickAggregatorActor, EventProcessorActor, ServerActor, UpstreamClientActor}
import com.typesafe.config.ConfigFactory

object ServerApp {

  val cronJobName = "EveryMinute"

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    val system = ActorSystem("akka-io-demo")
    val candlestickAggregator = system.actorOf(CandlestickAggregatorActor.props(config.getInt("app.keep-data-minutes")))
    val eventProcessor = system.actorOf(EventProcessorActor.props(candlestickAggregator))
    val inetSocketAddress = new InetSocketAddress(
      config.getString("app.upstream.hostname"),
      config.getInt("app.upstream.port"))
    val upstreamClient = system.actorOf(UpstreamClientActor.props(inetSocketAddress, eventProcessor))
    val server = system.actorOf(ServerActor.props(
      new InetSocketAddress(config.getInt("app.server.port")),
      candlestickAggregator
    ))


    // schedule a job that runs at the beginning of every minute
    import com.typesafe.akka.extension.quartz.QuartzSchedulerExtension
    val scheduler = QuartzSchedulerExtension(system)
    scheduler.schedule(cronJobName, server, SendDataForLastOneMinute)
  }
}
