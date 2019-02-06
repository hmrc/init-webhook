/*
 * Copyright 2019 HM Revenue & Customs
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

import org.scalatest.Matchers._
import org.scalatest.WordSpec
import uk.gov.hmrc.githubclient.HookEvent.{PullRequestReviewComment, Push}
import uk.gov.hmrc.githubclient._
import uk.gov.hmrc.initwebhook.ArgParser.ProgramArguments

class ValidatedArgsSpec extends WordSpec {

  "ValidatedArgs extractor" should {

    "return githubToken, verbosity and new webhooks if all fields are present" in {
      val programArguments = ProgramArguments(
        githubToken   = Some("my-pass"),
        orgName       = OrganisationName("org"),
        repoNames     = Set(RepositoryName("repo1"), RepositoryName("repo2")),
        contentType   = Some(HookContentType.Form),
        webhookUrl    = Some(Url("hook-url")),
        webhookSecret = Some(HookSecret("S3CR3T")),
        events        = Set(Push, PullRequestReviewComment),
        verbose       = true
      )

      programArguments match {
        case ValidatedArgs(githubToken, verbosity, newWebHooks) =>
          githubToken shouldBe "my-pass"
          verbosity   shouldBe true
          newWebHooks should contain only (
            NewWebHook(
              OrganisationName("org"),
              RepositoryName("repo1"),
              HookContentType.Form,
              Url("hook-url"),
              Some(HookSecret("S3CR3T")),
              Set(Push, PullRequestReviewComment)),
            NewWebHook(
              OrganisationName("org"),
              RepositoryName("repo2"),
              HookContentType.Form,
              Url("hook-url"),
              Some(HookSecret("S3CR3T")),
              Set(Push, PullRequestReviewComment))
          )
        case _ =>
          fail("ValidatedArgs extractor failed")
      }
    }

    "return githubToken, verbosity and new webhooks if all fields are present except webhookSecret" in {
      val programArguments = ProgramArguments(
        githubToken   = Some("my-pass"),
        orgName       = OrganisationName("org"),
        repoNames     = Set(RepositoryName("repo1"), RepositoryName("repo2")),
        contentType   = Some(HookContentType.Form),
        webhookUrl    = Some(Url("hook-url")),
        webhookSecret = None,
        events        = Set(Push, PullRequestReviewComment),
        verbose       = true
      )

      programArguments match {
        case ValidatedArgs(githubToken, verbosity, newWebHooks) =>
          githubToken shouldBe "my-pass"
          verbosity   shouldBe true
          newWebHooks should contain only (
            NewWebHook(
              OrganisationName("org"),
              RepositoryName("repo1"),
              HookContentType.Form,
              Url("hook-url"),
              None,
              Set(Push, PullRequestReviewComment)),
            NewWebHook(
              OrganisationName("org"),
              RepositoryName("repo2"),
              HookContentType.Form,
              Url("hook-url"),
              None,
              Set(Push, PullRequestReviewComment))
          )
        case _ =>
          fail("ValidatedArgs extractor failed")
      }
    }

    "return None if githubToken not present" in {
      ValidatedArgs.unapply {
        ProgramArguments(
          githubToken   = None,
          orgName       = OrganisationName("org"),
          repoNames     = Set(RepositoryName("repo1"), RepositoryName("repo2")),
          contentType   = Some(HookContentType.Form),
          webhookUrl    = Some(Url("hook-url")),
          webhookSecret = Some(HookSecret("S3CR3T")),
          events        = Set(Push, PullRequestReviewComment),
          verbose       = true
        )
      } shouldBe None
    }

    "return None if repoNames is empty" in {
      ValidatedArgs.unapply {
        ProgramArguments(
          githubToken   = Some("my-pass"),
          orgName       = OrganisationName("org"),
          repoNames     = Set.empty,
          contentType   = Some(HookContentType.Form),
          webhookUrl    = Some(Url("hook-url")),
          webhookSecret = Some(HookSecret("S3CR3T")),
          events        = Set(Push, PullRequestReviewComment),
          verbose       = true
        )
      } shouldBe None
    }

    "return None if contentType not present" in {
      ValidatedArgs.unapply {
        ProgramArguments(
          githubToken   = Some("my-pass"),
          orgName       = OrganisationName("org"),
          repoNames     = Set(RepositoryName("repo1"), RepositoryName("repo2")),
          contentType   = None,
          webhookUrl    = Some(Url("hook-url")),
          webhookSecret = Some(HookSecret("S3CR3T")),
          events        = Set(Push, PullRequestReviewComment),
          verbose       = true
        )
      } shouldBe None
    }

    "return None if webHookUrl not present" in {
      ValidatedArgs.unapply {
        ProgramArguments(
          githubToken   = Some("my-pass"),
          orgName       = OrganisationName("org"),
          repoNames     = Set(RepositoryName("repo1"), RepositoryName("repo2")),
          contentType   = Some(HookContentType.Form),
          webhookUrl    = None,
          webhookSecret = Some(HookSecret("S3CR3T")),
          events        = Set(Push, PullRequestReviewComment),
          verbose       = true
        )
      } shouldBe None
    }

    "return None if events are empty" in {
      ValidatedArgs.unapply {
        ProgramArguments(
          githubToken   = Some("my-pass"),
          orgName       = OrganisationName("org"),
          repoNames     = Set(RepositoryName("repo1"), RepositoryName("repo2")),
          contentType   = Some(HookContentType.Form),
          webhookUrl    = Some(Url("hook-url")),
          webhookSecret = Some(HookSecret("S3CR3T")),
          events        = Set.empty,
          verbose       = true
        )
      } shouldBe None
    }
  }
}
