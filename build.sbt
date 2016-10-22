organization := "DSE"

name := """beanhold"""

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "com.typesafe.akka" %% "akka-remote" % "2.4.7",
  "com.typesafe.akka" %% "akka-actor" % "2.4.7",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.7",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "junit" % "junit" % "4.12" % "test",
  "com.google.protobuf" % "protobuf-java" % "3.0.2",
  "com.novocode" % "junit-interface" % "0.11" % "test"
)

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")

import sbtprotobuf.{ProtobufPlugin=>PB}
import xsbti.compile

PB.protobufSettings

version in PB.protobufConfig := "3.0.2"

compileOrder := CompileOrder.JavaThenScala

compileOrder in Docker := compileOrder.value

enablePlugins(JavaAppPackaging)

// Docker settings
dockerBaseImage := "anapsix/alpine-java"
maintainer:= "Rob Kewley"
//dockerExposedPorts in Docker := Seq(9000)
dockerExposedVolumes := Seq()
dockerUpdateLatest := true
//dockerRepository := Some("dregistry:5000/dmf")