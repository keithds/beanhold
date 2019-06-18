organization := "DSE"

name := """beanhold"""

version := "1.1"

scalaVersion := "2.12.7"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "com.typesafe.akka" %% "akka-remote" % "2.5.17",
  "com.typesafe.akka" %% "akka-actor" % "2.5.17",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.17",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "junit" % "junit" % "4.12" % "test",
  "com.google.protobuf" % "protobuf-java" % "3.6.1",
  "com.novocode" % "junit-interface" % "0.11" % "test"
)

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")

import xsbti.compile

enablePlugins(ProtobufPlugin, JavaAppPackaging)

version in ProtobufConfig := "3.6.1"

compileOrder := CompileOrder.JavaThenScala

compileOrder in Docker := compileOrder.value


// Docker settings
dockerBaseImage := "anapsix/alpine-java"
maintainer:= "Rob Kewley"
//dockerExposedPorts in Docker := Seq(9000)
dockerExposedVolumes := Seq()
dockerUpdateLatest := true
//dockerRepository := Some("dregistry:5000/dmf")
