package com.mpakhomov.json

import java.sql.Timestamp

import com.mpakhomov.model.Candlestick
import spray.json.{DefaultJsonProtocol, DeserializationException, JsNumber, JsValue, JsonFormat}

object MyJsonProtocol extends DefaultJsonProtocol {
  implicit object TimestampFormat extends JsonFormat[Timestamp] {
    def write(obj: Timestamp) = JsNumber(obj.getTime)

    def read(json: JsValue) = json match {
      case JsNumber(time) => new Timestamp(time.toLong)

      case _ => throw new DeserializationException("Date expected")
    }
  }

  implicit val candlestickFormat = jsonFormat7(Candlestick)
}
