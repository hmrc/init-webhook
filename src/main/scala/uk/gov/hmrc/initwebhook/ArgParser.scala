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


object ArgParser {


  case class Config(
                     repoNames: Seq[String] = Seq(),
                     webhookUrl: String = "",
                     events: Seq[String] = Seq(),
                     verbose: Boolean = false)

  val parser = new scopt.OptionParser[Config]("init-webhook") {

    override def showUsageOnError = true

    head(s"\nInit web hook\n")

    help("help") text "prints this usage text"

    opt[Seq[String]]("repo-names") abbr ("rn") required() valueName ("<repo1>,<repo3>...") action { (x, c) =>
      c.copy(repoNames = x)
    } text "the name of the github repository"

    opt[Seq[String]]("events") abbr ("e") valueName ("<event1>,<event2>...") action { (x, c) =>
      c.copy(events = x)
    } text "coma separated events to for notifiation"

    opt[String]("webhook-url") abbr ("wu") required() action { (x, c) =>
      c.copy(webhookUrl = x)
    } text "the url to add as a github webhook"

    opt[Unit]('v', "verbose") action { (x, c) =>
      c.copy(verbose = true)
    } text "verbose mode (debug logging)"
  }
}
