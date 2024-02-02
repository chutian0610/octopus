import OctopusResolvers.*
import OctopusDependencies.*

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "info.victorchu"
ThisBuild / scalaVersion := "3.3.1"


// ================================ multi projects structure ==============================================

lazy val root = (project in file("."))
  .settings(
    name := "octopus",
    commonSettings
  )
  .aggregate(
    parser
  )

lazy val parser = (project in file("parser"))
  .enablePlugins(Antlr4Plugin)
  .settings(
    name := "parser",
    commonSettings,
    Antlr4 / antlr4GenVisitor := true,
    Antlr4 / antlr4GenListener := true,
    Antlr4 / antlr4Version := "4.13.1",
    Antlr4 / antlr4PackageName := Some ("info.victorchu.octopus.sql.parser.antlr"),
    libraryDependencies ++= Seq (
       fastparse
      ,scalatest
      ,antlr4
      ,guava
      ,scalaLogging
      ,slf4jSimple

    )
  )

// ===================================== common setting ======================================================

lazy val commonSettings = Seq(
  scalacOptions ++= compilerOptions,
  resolvers := allResolver
)

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
