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
  .enablePlugins(Antlr4Plugin)
  .settings(
    name := "parser",
    settings,
    Antlr4 / antlr4GenVisitor := true,
    Antlr4 / antlr4GenListener := true,
    Antlr4 / antlr4Version := "4.13.1",
    Antlr4 / antlr4PackageName := Some ("info.victorchu.octopus.sql.parser.antlr"),
    libraryDependencies ++= Seq (
       dependencies.fastparse
      ,dependencies.scalatest
      ,dependencies.antlr4
      ,dependencies.guava
      ,dependencies.scalaLogging
      ,dependencies.slf4jSimple

    )
  )

lazy val dependencies = new {
  val fastparseVersion = "3.0.2"
  val scalatestVersion = "3.2.17"
  val scalaLoggingVersion = "3.9.4"
  val antlr4Version = "4.13.1"
  val guavaVersion = "22.0"
  val slf4jSimpleVersion = "1.7.30"


  val scalatest = "org.scalatest" %% "scalatest" % scalatestVersion % "test"
  val fastparse = "com.lihaoyi" %% "fastparse" % fastparseVersion
  val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion

  // not scala based lib
  val antlr4 = "org.antlr" % "antlr4-runtime" % antlr4Version
  val guava ="com.google.guava" % "guava" % guavaVersion
  val slf4jSimple = "org.slf4j" % "slf4j-simple" % slf4jSimpleVersion % "test"


}

lazy val settings =
  commonSettings

/*
 * @see https://docs.scala-lang.org/overviews/compiler-options/index.html
 */
lazy val compilerOptions = Seq(
  "-encoding", "utf8",
  "-feature",
  "-language:existentials",
  "-language:implicitConversions",
  "-unchecked"

)

lazy val commonSettings = Seq(
  scalacOptions ++= compilerOptions
)