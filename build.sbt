ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild/organization := "info.victorchu"
ThisBuild / scalaVersion := "3.3.1"

lazy val root = (project in file("."))
  .settings(
    name := "octopus",
    settings
  )
  .aggregate(
    parser
  )

lazy val parser = (project in file("parser"))
  .settings(
    name := "parser",
    settings,
    libraryDependencies ++= Seq (
      dependencies.fastparse
      ,dependencies.scalatest

    )
  )

lazy val dependencies = new {
  val fastparseVersion = "3.0.2"
  val scalatestVersion = "3.2.17"


  val scalatest = "org.scalatest" %% "scalatest" % scalatestVersion % "test"
  val fastparse = "com.lihaoyi" %% "fastparse" % fastparseVersion
}

lazy val settings =
  commonSettings

lazy val compilerOptions = Seq(
  "-unchecked",
  "-feature",
  "-language:existentials",
  "-language:implicitConversions",
  "-deprecation",
//  "-nowarn",
  "-encoding",
  "utf8"
)

lazy val commonSettings = Seq(
  scalacOptions ++= compilerOptions
)