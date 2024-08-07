import OctopusResolvers.*
import OctopusDependencies.*

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "io.github.chutian0610.octopus"
ThisBuild / scalaVersion := "3.4.1"
ThisBuild / semanticdbEnabled:= true

// ================================ multi projects structure ==============================================

lazy val root = (project in file("."))
  .settings(
    name := "octopus",
    commonSettings
  )
  .aggregate(
    parser,benchmark,common,memory
  )

lazy val parser = (project in file("parser"))
  .dependsOn(common)
  .settings(
    name := "parser",
    commonSettings,
    libraryDependencies ++= Seq (
      scalatest
      ,antlr4
      ,guava
      ,scalaLogging
      ,slf4jSimple
      ,enumeratum
    )
  )

lazy val benchmark = (project in file("benchmark"))
  .enablePlugins(Antlr4Plugin)
  .dependsOn(parser)
  .settings(
    name := "benchmark",
    commonSettings,
    Antlr4 / antlr4GenVisitor := true,
    Antlr4 / antlr4GenListener := true,
    Antlr4 / antlr4Version := "4.13.1",
    Antlr4 / antlr4PackageName := Some ("io.github.chutian0610.octopus.benchmark.antlrparser"),
    libraryDependencies ++= Seq (
      scalatest
      ,antlr4
      ,guava
      ,scalaLogging
      ,slf4jSimple
    )
  )
lazy val common = (project in file("common"))
  .settings(
    name := "common",
    commonSettings,
    libraryDependencies ++= Seq (
      scalatest
      ,guava
      ,scalaLogging
      ,slf4jSimple
      ,enumeratum
      ,quest
    )
  )

  lazy val memory = (project in file("memory"))
  .dependsOn(common)
  .settings(
    name := "memory",
    commonSettings,
    libraryDependencies ++= Seq (
      scalatest
      ,guava
      ,scalaLogging
      ,slf4jSimple
      ,enumeratum
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
  "-unchecked",
  "-explain"
)