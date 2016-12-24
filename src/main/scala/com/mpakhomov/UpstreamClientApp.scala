package com.mpakhomov

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import com.mpakhomov.actors.CandlestickAggregatorActor.GetDataForLastNMinutes
import com.mpakhomov.actors.{CandlestickAggregatorActor, EventProcessorActor, UpstreamClientActor}
import com.typesafe.config.ConfigFactory

object UpstreamClientApp {

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

    // schedule a job that runs at the beginning of every minute
    import com.typesafe.akka.extension.quartz.QuartzSchedulerExtension
    val scheduler = QuartzSchedulerExtension(system)
    scheduler.schedule(cronJobName, candlestickAggregator, GetDataForLastNMinutes)
  }
}
