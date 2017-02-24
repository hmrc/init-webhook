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

import scopt.Read
import uk.gov.hmrc.initwebhook.GitType.{GitType, Open}

import scala.util.Try




object ArgParser {

  object GithubEvents extends Enumeration {
    val issues, pull_request, pull_request_review_comment, release, status = Value
  }

//  val defaultEvents = Seq("issues", "pull_request", "pull_request_review_comment", "release", "status")
  val defaultEvents = GithubEvents.values.toSeq.map(_.toString)

  implicit val weekDaysRead: scopt.Read[GitType.Value] = scopt.Read.reads(GitType withName)

  implicit def optionStringRead: Read[Option[String]] = Read.reads { (s: String) =>
    Option(s)
  }



  case class Config(
                     credentialsFile : String ="",
                     gitApiBaseUrl : String = "",
                     org :String = "",
                     repoNames: Seq[String] = Seq(),
                     webhookUrl: String = "",
                     webhookSecret: Option[String] = None,
                     events: Seq[String] = defaultEvents,
                     verbose: Boolean = false) {


  }

  val parser = new scopt.OptionParser[Config]("init-webhook") {

    override def showUsageOnError = true

    head(s"\nInit web hook\n")

    help("help") text "prints this usage text"


    opt[String]("cred-file-path") abbr "cf" required() valueName "/cred/file" action { (x, c) =>
      c.copy(credentialsFile = x)
    } text "git credentials file path"

    opt[String]("api-host") abbr "h" required() valueName "https://api.github.com" action { (x, c) =>
      c.copy(gitApiBaseUrl = x)
    } text "git api base url"

    opt[String]("org") abbr "o" required() valueName "hmrc" action { (x, c) =>
      c.copy(org = x)
    } text "the name of the github organization"

    opt[Seq[String]]("repo-names") abbr "rn" required() valueName "<repo1>,<repo3>..." action { (x, c) =>
      c.copy(repoNames = x)
    } text "the name of the github repository"

    opt[Seq[String]]("events") abbr "e" valueName "<event1>,<event2>..." action { (x, c) =>
      c.copy(events = x)
    } text "coma separated events to for notifiation"

    validate { x =>
      val t = Try(GithubEvents.withName(x))
      if(t.isSuccess) success
      else failure(s"$x is not a valid ServiceHookType. Valid values are: ${WebHookType.values.mkString(", ")}")
    }
      .action {(x, c) =>
        c.copy(webHookType = WebHookType.withName(x))
      }

    opt[String]("webhook-url") abbr "wu" required() action { (x, c) =>
      c.copy(webhookUrl = x)
    } text "the url to add as a github webhook"

    opt[Option[String]]("webhook-secret")
      .optional()
      .action { (x, c) =>
        c.copy(webhookSecret = x)
      }
      .text("Webhook secret key to be added to the Webhook")

    opt[Unit]('v', "verbose") action { (x, c) =>
      c.copy(verbose = true)
    } text "verbose mode (debug logging)"


  }
}
