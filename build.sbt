name := """akka-persistence-poc"""

version := "2.4.4"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-persistence-query-experimental" % "2.4.10",
  "com.typesafe.akka" %% "akka-actor" % "2.4.4",
  "com.typesafe.akka" %% "akka-persistence" % "2.4.4",
  "com.typesafe.akka" %% "akka-cluster" % "2.4.4",
  "com.typesafe.akka" %% "akka-cluster-sharding" % "2.4.4",
  "com.typesafe.akka" %% "akka-distributed-data-experimental" % "2.4.4",
  "com.typesafe.akka" %% "akka-persistence-cassandra" % "0.21",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.4",
  "org.json4s" % "json4s-native_2.11" % "3.5.0",
  "com.typesafe.akka" % "akka-http-experimental_2.11" % "2.4.4",
  "de.heikoseeberger" % "akka-http-json4s_2.11" % "1.11.0"
)

licenses := Seq(("CC0", url("http://creativecommons.org/publicdomain/zero/1.0")))

fork in run := true

cancelable in Global := true
