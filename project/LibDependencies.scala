import sbt._

object LibDependencies {

  val compile: Seq[ModuleID] = Seq(
    "com.github.scopt" %% "scopt"          % "3.3.0",
    "ch.qos.logback"   % "logback-classic" % "1.2.3",
    "uk.gov.hmrc"      %% "github-client"  % "2.6.0"
  )

  lazy val test: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % "3.0.5" % Test,
    "org.scalamock" %% "scalamock" % "4.1.0" % Test,
    "org.pegdown"   % "pegdown"    % "1.6.0" % Test
  )
}
