/*
 * Copyright 2018 HM Revenue & Customs
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

import scopt.Read

import scala.util.Try

object ArgParser {

  import GithubEvents._

  val parser = new scopt.OptionParser[Config]("init-webhook") {

    override def showUsageOnError = true

    head(s"\nInit web hook\n")

    help("help") text "prints this usage text"

    opt[String]("github-username") abbr "gu" required () action { (x, c) =>
      c.copy(githubUsername = x)
    } text "github username"

    opt[String]("github-password") abbr "gp" required () action { (x, c) =>
      c.copy(githubPassword = x)
    } text "github password"

    opt[String]("api-host") abbr "ah" required () valueName "https://api.github.com" action { (x, c) =>
      c.copy(gitApiBaseUrl = x)
    } text "git api base url"

    opt[String]("org") abbr "o" required () valueName "hmrc" action { (x, c) =>
      c.copy(org = x)
    } text "the name of the github organization"

    opt[String]("content-type") abbr "ct" required () valueName "json" validate {
      case "application/json" | "application/x-www-form-urlencoded" => success
      case ct =>
        failure(
          s"Unsupported content type '$ct'. Accepted values: 'application/json' and 'application/x-www-form-urlencoded'")
    } action { (x, c) =>
      c.copy(contentType = x)
    } text "the body format sent to the Webhook. Accepted values: 'application/json' and 'application/x-www-form-urlencoded'"

    opt[Seq[String]]("repo-names") abbr "rn" required () valueName "<repo1>,<repo3>..." action { (x, c) =>
      c.copy(repoNames = x.map(_.trim))
    } text "the name of the github repository"

    opt[Seq[String]]("events")
      .abbr("e")
      .valueName("<event1>,<event2>...")
      .action { (x, c) =>
        c.copy(events = x)
      }
      .text("comma separated events to for notification")
      .validate { x =>
        val t = Try(GithubEvents.withNames(x))
        if (t.isSuccess) success
        else failure(s"invalid github event found in $x.\n\tValid values are: ${GithubEvents.values.mkString(", ")}")
      }
      .action { (x, c) =>
        c.copy(events = withNames(x).map(_.toString))
      }

    opt[String]("webhook-url") abbr "wu" required () action { (x, c) =>
      c.copy(webhookUrl = x)
    } text "the url to add as a github Webhook"

    opt[Option[String]]("webhook-secret")
      .abbr("ws")
      .optional()
      .action { (x, c) =>
        c.copy(webhookSecret = x)
      }
      .text("Webhook secret key to be added to the Webhook")

    opt[Unit]('v', "verbose") action { (x, c) =>
      c.copy(verbose = true)
    } text "verbose mode (debug logging)"

  }

  implicit def optionStringRead: Read[Option[String]] = Read.reads { (s: String) =>
    Option(s)
  }

  case class Config(
    githubUsername: String        = "",
    githubPassword: String        = "",
    gitApiBaseUrl: String         = "",
    org: String                   = "",
    repoNames: Seq[String]        = Seq.empty,
    contentType: String           = "",
    webhookUrl: String            = "",
    webhookSecret: Option[String] = None,
    events: Seq[String]           = GithubEvents.defaultEvents,
    verbose: Boolean              = false)

}
