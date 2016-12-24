name := """akka-io-demo"""

version := "1.0"

scalaVersion := "2.12.1"

resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.16",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.16",
  "ch.qos.logback" % "logback-classic" % "1.1.8",
  "com.enragedginger" %% "akka-quartz-scheduler" % "1.6.0-akka-2.4.x",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.16" % "test",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test")
