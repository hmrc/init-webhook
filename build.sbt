import sbtassembly.AssemblyKeys.{assembly, assemblyJarName, assemblyMergeStrategy}
import sbt._
import sbtassembly.{MergeStrategy, PathList}

val appName = "init-webhook"

lazy val library = Project(appName, file("."))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning, SbtArtifactory)
  .settings(
    majorVersion := 2,
    makePublicallyAvailableOnBintray := true,
    libraryDependencies ++= LibDependencies.compile ++ LibDependencies.test,
    assemblySettings,
    addArtifact(artifact in(Compile, assembly), assembly)
  )

lazy val assemblySettings = Seq(
  assemblyJarName in assembly := "init-webhook.jar",
  assemblyMergeStrategy in assembly := {
    case PathList("org", "apache", "commons", "logging", _*) => MergeStrategy.first
    case PathList("play", "core", "server", _*)              => MergeStrategy.first
    case PathList("uk", "gov", "hmrc", "BuildInfo$.class")   => MergeStrategy.discard
    case x                                                   =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  },
  artifact in(Compile, assembly) := {
    val art = (artifact in(Compile, assembly)).value
    art.copy(`classifier` = Some("assembly"))
  }
)