logLevel := Level.Info

resolvers += Resolver.url("hmrc-sbt-plugin-releases",
  url("https://dl.bintray.com/hmrc/sbt-plugin-releases"))

addSbtPlugin("uk.gov.hmrc" % "sbt-auto-build" % "0.9.0")

addSbtPlugin("uk.gov.hmrc" % "sbt-git-versioning" % "0.8.0")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.13.0")

