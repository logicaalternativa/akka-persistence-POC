name := """akka-persistence-POC"""

version := "2.4.17"

scalaBinaryVersion := "2.12"
scalaVersion := "2.12.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %%"akka-persistence-query-experimental" % "2.4.17",
  "com.typesafe.akka" %% "akka-actor" % "2.4.17",
  "com.typesafe.akka" %% "akka-persistence" % "2.4.17",
  "com.typesafe.akka" %% "akka-cluster" % "2.4.17",
  "com.typesafe.akka" %% "akka-cluster-sharding" % "2.4.17",
  "com.typesafe.akka" %% "akka-distributed-data-experimental" % "2.4.17",
  "com.typesafe.akka" %% "akka-persistence-cassandra" % "0.23",
  "com.lightbend.akka" %% "akka-split-brain-resolver" % "1.0.1",
  "ch.qos.logback"    %  "logback-classic" % "1.1.3",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.17",
  "org.scalaz" %% "scalaz-core" % "7.2.7",
  "org.scalaz" %% "scalaz-scalacheck-binding" % "7.2.7"
)

licenses := Seq(("CC0", url("http://creativecommons.org/publicdomain/zero/1.0")))

// It needs for pack task (see plugins project)
packAutoSettings

fork in run := true

cancelable in Global := true
