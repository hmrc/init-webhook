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

import scopt.OptionParser
import uk.gov.hmrc.githubclient.HookEvent._
import uk.gov.hmrc.githubclient._

import scala.util.Try

private object ArgParser {

  val parser: OptionParser[ProgramArguments] = new OptionParser[ProgramArguments]("init-webhook") {

    override def showUsageOnError = true

    head(s"\nInit web hook\n")

    help("help") text "prints this usage text"

    opt[String]("github-token")
      .required()
      .text("github token")
      .validate { x =>
        if (x.trim.nonEmpty) success
        else failure(s"empty github-token not allowed")
      }
      .action { (x, c) =>
        c.copy(githubToken = Some(x.trim))
      }

    opt[String]("github-org")
      .optional()
      .text("the name of the github organization. Defaults to hmrc")
      .validate { x =>
        if (x.trim.nonEmpty) success
        else failure(s"empty github-org not allowed")
      }
      .action { (x, c) =>
        c.copy(orgName = OrganisationName(x.trim))
      }

    opt[Seq[String]]("repositories")
      .required()
      .text("the name of the github repository")
      .valueName("<repo1>,<repo2>...")
      .validate { x =>
        if (x.forall(_.trim.nonEmpty)) success
        else failure(s"empty repository names not allowed")
      }
      .action { (x, c) =>
        c.copy(repoNames = x.map(_.trim).map(RepositoryName.apply).toSet)
      }

    opt[String]("content-type")
      .required()
      .text("the body format sent to the Webhook")
      .valueName("'application/json', 'application/x-www-form-urlencoded'")
      .validate {
        case "application/json" | "application/x-www-form-urlencoded" => success
        case ct =>
          failure(
            s"Unsupported content type '$ct'. Allowed values: 'application/json' and 'application/x-www-form-urlencoded'"
          )
      }
      .action { (x, c) =>
        c.copy(contentType = x match {
          case "application/json"                  => Some(HookContentType.Json)
          case "application/x-www-form-urlencoded" => Some(HookContentType.Form)
        })
      }

    opt[String]("webhook-url")
      .required()
      .text("the url to add as a github Webhook")
      .validate { x =>
        if (x.trim.nonEmpty) success
        else failure(s"empty webhook-url not allowed")
      }
      .action { (x, c) =>
        c.copy(webhookUrl = Some(Url(x.trim)))
      }

    opt[String]("webhook-secret")
      .optional()
      .text("an optional webhook secret key to be added to the Webhook")
      .action { (x, c) =>
        c.copy(webhookSecret = Some(HookSecret(x)))
      }

    opt[Seq[String]]("events")
      .text(
        s"optional comma separated events for notification. Defaults to: ${ProgramArguments.defaultEvents.mkString(", ")}"
      )
      .valueName("<event1>,<event2>...")
      .validate { x =>
        if (Try(HookEvent(x.map(_.trim): _*)).isSuccess) success
        else failure(s"invalid github event found in $x.\n\tValid values are: ${HookEvent.all.mkString(", ")}")
      }
      .action { (x, c) =>
        c.copy(events = HookEvent(x.map(_.trim): _*))
      }

    opt[Unit]("verbose")
      .optional()
      .text("verbose mode (debug logging). Defaults to false")
      .action { (x, c) =>
        c.copy(verbose = true)
      }
  }

  private[initwebhook] case class ProgramArguments(
    githubToken: Option[String],
    orgName: OrganisationName,
    repoNames: Set[RepositoryName],
    contentType: Option[HookContentType],
    webhookUrl: Option[Url],
    webhookSecret: Option[HookSecret],
    events: Set[HookEvent],
    verbose: Boolean
  )

  private[initwebhook] object ProgramArguments {

    private[initwebhook] val defaultEvents: Set[HookEvent] = Set(
      Issues,
      PullRequest,
      PullRequestReviewComment,
      Release,
      Status
    )

    val default: ProgramArguments = ProgramArguments(
      githubToken   = None,
      orgName       = OrganisationName("hmrc"),
      repoNames     = Set.empty,
      contentType   = None,
      webhookUrl    = None,
      webhookSecret = None,
      events        = defaultEvents,
      verbose       = false
    )
  }
}
