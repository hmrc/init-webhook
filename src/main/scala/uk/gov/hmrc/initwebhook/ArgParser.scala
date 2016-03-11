/*
 * Copyright 2016 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.initwebhook


object ArgParser{


  case class Config(
                     repoName: String = "",
                     teamName:String = "",
                     webhookUrl: String = "",
                     verbose:Boolean = false)

  val parser = new scopt.OptionParser[Config]("init-webhook") {

    override def showUsageOnError = true

    head(s"\nInit web hook\n")

    help("help") text "prints this usage text"

    arg[String]("repo-names") action { (x, c) =>
      c.copy(repoName = x) } text "the name of the github repository"

    arg[String]("team-name") action { (x, c) =>
      c.copy(teamName = x) } text "the github team name"

    opt[String]("webhook-url") action { (x, c) =>
      c.copy(webhookUrl = x) } text "the url to add as a github webhook"

    opt[Unit]('v', "verbose") action { (x, c) =>
      c.copy(verbose = true) } text "verbose mode (debug logging)"
  }
}
