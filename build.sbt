organization := "DSE"

name := """beanhold"""

version := "1.0"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "com.typesafe.akka" %% "akka-remote" % "2.3.11",
  "com.typesafe.akka" %% "akka-actor" % "2.3.11",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.11",
  "com.typesafe.akka" % "akka-slf4j_2.11" % "2.3.11",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "junit" % "junit" % "4.12" % "test",
  "com.novocode" % "junit-interface" % "0.11" % "test"
)

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")

enablePlugins(JavaAppPackaging)

// Docker settings
dockerBaseImage := "java"
maintainer:= "Rob Kewley"
//dockerExposedPorts in Docker := Seq(9000)
dockerExposedVolumes := Seq()
dockerUpdateLatest := true
//dockerRepository := Some("dregistry:5000/dmf")