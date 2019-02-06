import sbt._

object LibDependencies {

  val compile: Seq[ModuleID] = Seq(
    "com.github.scopt" %% "scopt"          % "3.3.0",
    "ch.qos.logback"   % "logback-classic" % "1.2.3",
    "uk.gov.hmrc"      %% "github-client"  % "2.7.0",
    // force dependencies due to security flaws found in jackson-databind < 2.9.x using XRay
    "com.fasterxml.jackson.core"     % "jackson-core"            % "2.9.7",
    "com.fasterxml.jackson.core"     % "jackson-databind"        % "2.9.7",
    "com.fasterxml.jackson.core"     % "jackson-annotations"     % "2.9.7",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8"   % "2.9.7",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % "2.9.7"
  )

  lazy val test: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % "3.0.5" % Test,
    "org.scalamock" %% "scalamock" % "4.1.0" % Test,
    "org.pegdown"   % "pegdown"    % "1.6.0" % Test
  )
}
