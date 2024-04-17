import sbt.*

object OctopusDependencies {
    val scalatest = "org.scalatest" %% "scalatest" % "3.2.17" % "test"
    val fastparse = "com.lihaoyi" %% "fastparse" % "3.0.2"
    val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4"

    //  ========================= not scala based lib =================================
    val antlr4 = "org.antlr" % "antlr4-runtime" %  "4.13.1"
    val guava ="com.google.guava" % "guava" %  "22.0"
    val slf4jSimple = "org.slf4j" % "slf4j-simple" % "1.7.30" % "test"
}