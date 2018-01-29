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

    opt[String]("cred-file-path") abbr "cf" required () valueName "/cred/file" action { (x, c) =>
      c.copy(credentialsFile = x)
    } text "git credentials file path"

    opt[String]("api-host") abbr "h" required () valueName "https://api.github.com" action { (x, c) =>
      c.copy(gitApiBaseUrl = x)
    } text "git api base url"

    opt[String]("org") abbr "o" required () valueName "hmrc" action { (x, c) =>
      c.copy(org = x)
    } text "the name of the github organization"

    opt[String]("content-type") abbr "ct" required () valueName "json" validate {
      case "json" | "form" => success
      case _ =>
        failure(
          "Unsupported content type. Accepted values: 'json' for 'application/json' and 'form' for 'application/x-www-form-urlencoded'")
    } action { (x, c) =>
      c.copy(contentType = x)
    } text "the body format sent to the Webhook. Accepted values: 'json' for 'application/json' and 'form' for 'application/x-www-form-urlencoded'"

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

  implicit val weekDaysRead: scopt.Read[GitType.Value] = scopt.Read.reads(GitType withName)

  implicit def optionStringRead: Read[Option[String]] = Read.reads { (s: String) =>
    Option(s)
  }

  case class Config(
    credentialsFile: String       = "",
    gitApiBaseUrl: String         = "",
    org: String                   = "",
    repoNames: Seq[String]        = Seq(),
    contentType: String           = "json",
    webhookUrl: String            = "",
    webhookSecret: Option[String] = None,
    events: Seq[String]           = GithubEvents.defaultEvents,
    verbose: Boolean              = false) {}

}
