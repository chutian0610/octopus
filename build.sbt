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
    Antlr4 / antlr4Version := "4.13.1",
    Antlr4 / antlr4PackageName := Some ("info.victorchu.octopus.sql.parser.antlr"),
    libraryDependencies ++= Seq (
      dependencies.fastparse
      ,dependencies.scalatest
    )
  )

lazy val dependencies = new {
  val fastparseVersion = "3.0.2"
  val scalatestVersion = "3.2.17"
  val antlr4Version = "4.13.1"


  val scalatest = "org.scalatest" %% "scalatest" % scalatestVersion % "test"
  val fastparse = "com.lihaoyi" %% "fastparse" % fastparseVersion
  val antlr4 = "org.antlr" % "antlr-runtime" % antlr4Version
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
  "-unchecked",
  "-deprecation"

)

lazy val commonSettings = Seq(
  scalacOptions ++= compilerOptions
)