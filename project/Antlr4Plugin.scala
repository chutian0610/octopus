import sbt.*
import sbt.Keys.*
import sbt.internal.io.Source

import scala.sys.process.Process

object Antlr4Plugin extends AutoPlugin {
  object autoImport {
    val Antlr4 = config("antlr4")
    val antlr4Version = settingKey[String]("Version of antlr4")
    val antlr4Generate = taskKey[Seq[File]]("Generate classes from antlr4 grammars")
    val antlr4RuntimeDependency = settingKey[ModuleID]("Library dependency for antlr4 runtime")
    val antlr4Dependency = settingKey[ModuleID]("Build dependency required for parsing grammars")
    val antlr4PackageName = settingKey[Option[String]]("Name of the package for generated classes")
    val antlr4GenListener = settingKey[Boolean]("Generate listener")
    val antlr4GenVisitor = settingKey[Boolean]("Generate visitor")
    val antlr4TreatWarningsAsErrors = settingKey[Boolean]("Treat warnings as errors when generating parser")
  }

  import autoImport.*

  private val antlr4BuildDependency = settingKey[ModuleID]("Build dependency required for parsing grammars, scoped to plugin")

  def antlr4GeneratorTask: Def.Initialize[Task[Seq[File]]] = Def.task {
    val targetBaseDir = (Antlr4 / javaSource).value
    val classpath = (Antlr4 / managedClasspath).value.files
    val log = streams.value.log
    val packageName = (Antlr4 / antlr4PackageName).value
    val listenerOpt = (Antlr4 / antlr4GenListener).value
    val visitorOpt = (Antlr4 / antlr4GenVisitor).value
    val warningsAsErrorOpt = (Antlr4 / antlr4TreatWarningsAsErrors).value
    val cachedCompile = FileFunction.cached(streams.value.cacheDirectory / "antlr4", FilesInfo.lastModified, FilesInfo.exists) {
      in: Set[File] =>
        runAntlr(
          srcFiles = in,
          targetBaseDir = targetBaseDir,
          classpath = classpath,
          log = log,
          packageName = packageName,
          listenerOpt = listenerOpt,
          visitorOpt = visitorOpt,
          warningsAsErrorOpt = warningsAsErrorOpt
        )
    }
    cachedCompile((((Antlr4 / sourceDirectory).value ** "*.g4") --- ((Antlr4 / sourceDirectory).value ** "imports" ** "*.g4")).get.toSet).toSeq
  }

  def runAntlr(
                srcFiles: Set[File],
                targetBaseDir: File,
                classpath: Seq[File],
                log: Logger,
                packageName: Option[String],
                listenerOpt: Boolean,
                visitorOpt: Boolean,
                warningsAsErrorOpt: Boolean): Set[File] = {
    val targetDir = packageName.map {
      _.split('.').foldLeft(targetBaseDir) {
        _ / _
      }
    }.getOrElse(targetBaseDir)
    val baseArgs = Seq("-cp", Path.makeString(classpath), "org.antlr.v4.Tool")
    val listenerArgs = if (listenerOpt) Seq("-listener") else Seq("-no-listener")
    val visitorArgs = if (visitorOpt) Seq("-visitor") else Seq("-no-visitor")
    val warningAsErrorArgs = if (warningsAsErrorOpt) Seq("-Werror") else Seq.empty
    for (srcFile <- srcFiles) {
      val packageArgs = packageName.map {
        _ + "." + srcFile.getAbsoluteFile.getParent.split("/src/main/antlr4/").last.replaceAll("/", ".")
      }.toSeq.flatMap { p => Seq("-package", p) }
      val output = Seq(targetDir, srcFile.getAbsoluteFile.getParent.split("/src/main/antlr4/").last).mkString("/")
      val outArgs = Seq("-o", output)
      val args = baseArgs ++ outArgs ++ packageArgs ++ listenerArgs ++ visitorArgs ++ warningAsErrorArgs ++ Seq(srcFile.toString)
      log.info(
        s"""[Antlr4Plugin] call antlr4 Tool \n[Antlr4Plugin] |- g4 file: ${srcFile.toString} \n[Antlr4Plugin] |- output: $output""".stripMargin)
      val exitCode = Process("java", args) ! log
      if (exitCode != 0) sys.error(s"Antlr4 failed with exit code $exitCode")
    }
    (targetDir ** "*.java").get.toSet
  }

  override def projectSettings: Seq[Def.Setting[?]] = inConfig(Antlr4)(Seq(
    sourceDirectory := (Compile / sourceDirectory).value / "antlr4",
    javaSource := (Compile / sourceManaged).value / "java",
    managedClasspath := Classpaths.managedJars(configuration.value, classpathTypes.value, update.value),
    antlr4Version := "4.13.1",
    antlr4Generate := antlr4GeneratorTask.value,
    antlr4Dependency := "org.antlr" % "antlr4" % antlr4Version.value,
    antlr4RuntimeDependency := "org.antlr" % "antlr4-runtime" % antlr4Version.value,
    antlr4BuildDependency := antlr4Dependency.value % Antlr4.name,
    antlr4PackageName := None,
    antlr4GenListener := true,
    antlr4GenVisitor := false,
    antlr4TreatWarningsAsErrors := false
  )) ++ Seq(
    ivyConfigurations += Antlr4,
    Compile / managedSourceDirectories += (Antlr4 / javaSource).value,
    Compile / sourceGenerators += (Antlr4 / antlr4Generate).taskValue,
    watchSources += new Source(sourceDirectory.value, "*.g4", HiddenFileFilter),
    cleanFiles += (Antlr4 / javaSource).value,
    libraryDependencies += (Antlr4 / antlr4BuildDependency).value,
    libraryDependencies += (Antlr4 / antlr4RuntimeDependency).value
  )
}