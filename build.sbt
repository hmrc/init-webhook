import sbtassembly.AssemblyKeys.assembly
import sbt._

val appName = "init-webhook"

lazy val library = Project(appName, file("."))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning, SbtArtifactory)
  .settings(
    majorVersion        := 1,
    makePublicallyAvailableOnBintray := true,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    AssemblySettings(),
    addArtifact(artifact in (Compile, assembly), assembly)
  )

