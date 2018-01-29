import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import sbtassembly.AssemblyKeys._
import sbtassembly.{MergeStrategy, PathList}
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning

object HmrcBuild extends Build {

  val appName = "init-webhook"

  lazy val library = Project(appName, file("."))
    .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning)
    .settings(
      scalaVersion := "2.11.11",
      scalacOptions ++= Seq(
        "-Xlint",
        "-Xmax-classfile-name",
        "100",
        "-encoding",
        "UTF-8"
      ),
      AssemblySettings(),
      addArtifact(artifact in (Compile, assembly), assembly)
    )
    .settings(
      parallelExecution in Test := false,
      fork in Test := false,
      retrieveManaged := true
    )
    .settings(libraryDependencies ++= AppDependencies())
}

private object AppDependencies {

  val compile = Seq(
    "com.github.scopt"          %% "scopt"      % "3.3.0",
    "com.typesafe.play"         %% "play-ws"    % "2.4.3",
    "commons-io"                % "commons-io"  % "2.4",
    "org.apache.httpcomponents" % "httpcore"    % "4.3.2",
    "org.apache.httpcomponents" % "httpclient"  % "4.3.5",
    "io.spray"                  %% "spray-json" % "1.3.2"
  )

  lazy val test: Seq[ModuleID] = Seq(
    "org.scalatest"          %% "scalatest"  % "2.2.4" % "test",
    "org.pegdown"            % "pegdown"     % "1.4.2" % "test",
    "org.mockito"            % "mockito-all" % "1.9.5" % "test",
    "com.github.tomakehurst" % "wiremock"    % "1.52"  % "test"
  )

  def apply() = compile ++ test
}

object AssemblySettings {
  def apply() = Seq(
    assemblyJarName in assembly := "init-webhook.jar",
    assemblyMergeStrategy in assembly := {
      case PathList("org", "apache", "commons", "logging", xs @ _*) => MergeStrategy.first
      case PathList("play", "core", "server", xs @ _*)              => MergeStrategy.first
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },
    artifact in (Compile, assembly) := {
      val art = (artifact in (Compile, assembly)).value
      art.copy(`classifier` = Some("assembly"))
    }
  )
}
