package com.mpakhomov.actors

import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef, TestKit}
import com.mpakhomov.actors.CandlestickAggregatorActor.{GetDataForLastMinute, GetDataForLastNMinutes}
import com.mpakhomov.model.Candlestick
import com.typesafe.config.ConfigFactory
import model.UpstreamMessage
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.duration._

class CandlestickAggregatorActorUsageSpec
  extends TestKit(ActorSystem("CandlestickAggregatorActorUsageSpec"))
    with DefaultTimeout with ImplicitSender
    with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {
    shutdown()
  }

  val config = ConfigFactory.load()
  val actorRef = TestActorRef(Props(new CandlestickAggregatorActor(config.getInt("app.keep-data-minutes"))))

  "CandlestickAggregatorActorUsageSpec" should {
    "return candlesticks for the last 10 minutes" in {
//      within(500.millis) {
      within(1.minute) {
        createData1().foreach(actorRef ! _)
        actorRef ! GetDataForLastMinute

        createData2().foreach(actorRef ! _)
        actorRef ! GetDataForLastMinute

        val future = (actorRef ? GetDataForLastNMinutes).mapTo[Seq[Candlestick]]
        val candlesticks = Await.result(future, 500.millis)
        candlesticks should not be empty
        candlesticks.size shouldBe 4
        val resultStr = ServerActor.buildJsonStr(candlesticks)
        val expectedStr = """{ "ticker": "AAPL", "timestamp": "2016-01-01T15:02:00Z", "open": 101.1, "high": 101.3, "low": 101, "close": 101, "volume": 1300 }
                            |{ "ticker": "MSFT", "timestamp": "2016-01-01T15:02:00Z", "open": 120.1, "high": 120.1, "low": 120.1, "close": 120.1, "volume": 500 }
                            |{ "ticker": "AAPL", "timestamp": "2016-01-01T15:03:00Z", "open": 102.1, "high": 103.2, "low": 102.1, "close": 103.2, "volume": 1100 }
                            |{ "ticker": "MSFT", "timestamp": "2016-01-01T15:03:00Z", "open": 120.2, "high": 120.2, "low": 120, "close": 120, "volume": 1700 }""".stripMargin
        resultStr shouldEqual expectedStr
      }
    }
  }

  "CandlestickAggregatorActorUsageSpec" should {
    "return candlesticks for last minute" in {
      //      within(500.millis) {
      within(1.minute) {
        createData1().foreach(actorRef ! _)
        actorRef ! GetDataForLastMinute

        createData2().foreach(actorRef ! _)
        actorRef ! GetDataForLastMinute

        createData3().foreach(actorRef ! _)

        val future = (actorRef ? GetDataForLastMinute).mapTo[Seq[Candlestick]]
        val candlesticks = Await.result(future, 500.millis)
        candlesticks should not be empty
        candlesticks.size shouldBe 2
        val resultStr = ServerActor.buildJsonStr(candlesticks)
        // MP: in the spec the results for 15:04 are wrong. Should be 102.1
        val expectedStr = """{ "ticker": "AAPL", "timestamp": "2016-01-01T15:04:00Z", "open": 102.1, "high": 102.1, "low": 102.1, "close": 102.1, "volume": 100 }
                            |{ "ticker": "MSFT", "timestamp": "2016-01-01T15:04:00Z", "open": 102.1, "high": 102.1, "low": 102.1, "close": 102.1, "volume": 200 }""".stripMargin
        resultStr shouldEqual expectedStr
      }
    }
  }

  /*
    2016-01-01 15:02:10 AAPL 101.1 200
    2016-01-01 15:02:15 AAPL 101.2 100
    2016-01-01 15:02:25 AAPL 101.3 300
    2016-01-01 15:02:35 MSFT 120.1 500
    2016-01-01 15:02:40 AAPL 101.0 700

    2016-01-01 15:03:10 AAPL 102.1 1000
    2016-01-01 15:03:11 MSFT 120.2 1000
    2016-01-01 15:03:30 AAPL 103.2 100
    2016-01-01 15:03:31 MSFT 120.0 700

    2016-01-01 15:04:21 AAPL 102.1 100
    2016-01-01 15:04:21 MSFT 102.1 200
   */

  def str2Timestamp(s: String): Timestamp = {
    val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val dt = LocalDateTime.parse(s, dtf)
    Timestamp.valueOf(dt)
  }

  def createData1(): Seq[UpstreamMessage] = {
    val data = new ArrayBuffer[UpstreamMessage]()
    data += UpstreamMessage(str2Timestamp("2016-01-01 15:02:10"), "AAPL", 101.1, 200)
    data += UpstreamMessage(str2Timestamp("2016-01-01 15:02:15"), "AAPL", 101.2, 100)
    data += UpstreamMessage(str2Timestamp("2016-01-01 15:02:25"), "AAPL", 101.3, 300)
    data += UpstreamMessage(str2Timestamp("2016-01-01 15:02:35"), "MSFT", 120.1, 500)
    data += UpstreamMessage(str2Timestamp("2016-01-01 15:02:40"), "AAPL", 101.0, 700)
    data
  }

  def createData2(): Seq[UpstreamMessage] = {
    val data = new ArrayBuffer[UpstreamMessage]()
    data += UpstreamMessage(str2Timestamp("2016-01-01 15:03:10"), "AAPL", 102.1, 1000)
    data += UpstreamMessage(str2Timestamp("2016-01-01 15:03:11"), "MSFT", 120.2, 1000)
    data += UpstreamMessage(str2Timestamp("2016-01-01 15:03:30"), "AAPL", 103.2, 100)
    data += UpstreamMessage(str2Timestamp("2016-01-01 15:03:31"), "MSFT", 120.0, 700)
    data
  }

  def createData3(): Seq[UpstreamMessage] = {
    val data = new ArrayBuffer[UpstreamMessage]()
    data += UpstreamMessage(str2Timestamp("2016-01-01 15:04:21"), "AAPL", 102.1, 100)
    data += UpstreamMessage(str2Timestamp("2016-01-01 15:04:21"), "MSFT", 102.1, 200)
    data
  }




}

object CandlestickAggregatorActorUsageSpec {

}



