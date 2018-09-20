import sbt._

object AppDependencies {

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

}
