import sbt._

import sbtprotobuf.{ProtobufPlugin=>PB}

object MyBuild extends Build {
  lazy val beanhold = Project(
    id = "beanhold",
    base = file(".")
  ).settings(
    PB.protobufSettings : _*
  ).settings(
    /* custom settings here */
  )
}