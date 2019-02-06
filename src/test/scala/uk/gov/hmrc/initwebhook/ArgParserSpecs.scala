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
import uk.gov.hmrc.initwebhook.ArgParser.{ProgramArguments, parser}

class ArgParserSpecs extends WordSpec {

  "ArgParser" should {

    "create correct config if all options are given" in {
      val args = Seq(
        "--github-token",
        "my-pass",
        "--github-org",
        "org",
        "--repositories",
        "repo1,repo2",
        "--content-type",
        "application/x-www-form-urlencoded",
        "--webhook-url",
        "hook-url",
        "--webhook-secret",
        "S3CR3T",
        "--events",
        "push,pull_request_review_comment",
        "--verbose"
      )

      val maybeConfig = parser.parse(args, defaultProgramArguments)

      maybeConfig shouldBe Some(
        ProgramArguments(
          githubToken   = Some("my-pass"),
          orgName       = OrganisationName("org"),
          repoNames     = Set(RepositoryName("repo1"), RepositoryName("repo2")),
          contentType   = Some(HookContentType.Form),
          webhookUrl    = Some(Url("hook-url")),
          webhookSecret = Some(HookSecret("S3CR3T")),
          events        = Set(Push, PullRequestReviewComment),
          verbose       = true
        )
      )
    }

    "trim spaces in github-token, github-org, repositories, webhook-url and events" in {

      val args = Seq(
        "--github-token",
        "my-pass ",
        "--github-org",
        " org ",
        "--repositories",
        " repo1 , repo2",
        "--content-type",
        "application/x-www-form-urlencoded",
        "--webhook-url",
        " hook-url ",
        "--webhook-secret",
        "S3CR3T",
        "--events",
        "push , pull_request_review_comment"
      )

      val maybeConfig = parser.parse(args, defaultProgramArguments)

      maybeConfig shouldBe Some(
        ProgramArguments(
          githubToken   = Some("my-pass"),
          orgName       = OrganisationName("org"),
          repoNames     = Set(RepositoryName("repo1"), RepositoryName("repo2")),
          contentType   = Some(HookContentType.Form),
          webhookUrl    = Some(Url("hook-url")),
          webhookSecret = Some(HookSecret("S3CR3T")),
          events        = Set(Push, PullRequestReviewComment),
          verbose       = false
        )
      )
    }

    "fail if empty github-token given" in {
      val args = Seq(
        "--github-token",
        " ",
        "--github-org",
        "org",
        "--repositories",
        "repo1,repo2",
        "--content-type",
        "application/x-www-form-urlencoded",
        "--webhook-url",
        "hook-url",
        "--webhook-secret",
        "S3CR3T",
        "--events",
        "push,pull_request_review_comment",
        "--verbose"
      )

      parser.parse(args, defaultProgramArguments) shouldBe None
    }

    "fail if empty github-org given" in {
      val args = Seq(
        "--github-token",
        "my-pass",
        "--github-org",
        " ",
        "--repositories",
        "repo1,repo2",
        "--content-type",
        "application/x-www-form-urlencoded",
        "--webhook-url",
        "hook-url",
        "--webhook-secret",
        "S3CR3T",
        "--events",
        "push,pull_request_review_comment",
        "--verbose"
      )

      parser.parse(args, defaultProgramArguments) shouldBe None
    }

    "not fail if no github-org given" in {
      val args = Seq(
        "--github-token",
        "my-pass",
        "--repositories",
        "repo1,repo2",
        "--content-type",
        "application/x-www-form-urlencoded",
        "--webhook-url",
        "hook-url",
        "--webhook-secret",
        "S3CR3T",
        "--events",
        "push,pull_request_review_comment",
        "--verbose"
      )

      val maybeConfig = parser.parse(args, defaultProgramArguments)

      maybeConfig shouldBe Some(
        ProgramArguments(
          githubToken   = Some("my-pass"),
          orgName       = defaultProgramArguments.orgName,
          repoNames     = Set(RepositoryName("repo1"), RepositoryName("repo2")),
          contentType   = Some(HookContentType.Form),
          webhookUrl    = Some(Url("hook-url")),
          webhookSecret = Some(HookSecret("S3CR3T")),
          events        = Set(Push, PullRequestReviewComment),
          verbose       = true
        )
      )
    }

    "fail if empty repository given" in {
      val args = Seq(
        "--github-token",
        "my-pass",
        "--github-org",
        "org",
        "--repositories",
        "repo1, ",
        "--content-type",
        "application/x-www-form-urlencoded",
        "--webhook-url",
        "hook-url",
        "--webhook-secret",
        "S3CR3T",
        "--events",
        "push,pull_request_review_comment",
        "--verbose"
      )

      parser.parse(args, defaultProgramArguments) shouldBe None
    }

    "fail if no repositories given" in {
      val args = Seq(
        "--github-token",
        "my-pass",
        "--github-org",
        "org",
        "--content-type",
        "application/x-www-form-urlencoded",
        "--webhook-url",
        "hook-url",
        "--webhook-secret",
        "S3CR3T",
        "--events",
        "push,pull_request_review_comment",
        "--verbose"
      )

      parser.parse(args, defaultProgramArguments) shouldBe None
    }

    "fail if empty content-type given" in {
      val args = Seq(
        "--github-token",
        "my-pass",
        "--github-org",
        "org",
        "--repositories",
        "repo1,repo2",
        "--content-type",
        "--webhook-url",
        "hook-url",
        "--webhook-secret",
        "S3CR3T",
        "--events",
        "push,pull_request_review_comment",
        "--verbose"
      )

      parser.parse(args, defaultProgramArguments) shouldBe None
    }

    "fail if unknown content-type given" in {
      val args = Seq(
        "--github-token",
        "my-pass",
        "--github-org",
        "org",
        "--repositories",
        "repo1,repo2",
        "--content-type",
        "unknown",
        "--webhook-url",
        "hook-url",
        "--webhook-secret",
        "S3CR3T",
        "--events",
        "push,pull_request_review_comment",
        "--verbose"
      )

      parser.parse(args, defaultProgramArguments) shouldBe None
    }

    "fail if empty webhook-url given" in {
      val args = Seq(
        "--github-token",
        "my-pass",
        "--github-org",
        "org",
        "--repositories",
        "repo1,repo2",
        "--content-type",
        "application/x-www-form-urlencoded",
        "--webhook-url",
        " ",
        "--webhook-secret",
        "S3CR3T",
        "--events",
        "push,pull_request_review_comment",
        "--verbose"
      )

      parser.parse(args, defaultProgramArguments) shouldBe None
    }

    "fail if empty webhook-secret given" in {
      val args = Seq(
        "--github-token",
        "my-pass",
        "--github-org",
        "org",
        "--repositories",
        "repo1,repo2",
        "--content-type",
        "application/x-www-form-urlencoded",
        "--webhook-url",
        "hook-url",
        "--webhook-secret",
        "",
        "--events",
        "push,pull_request_review_comment",
        "--verbose"
      )

      parser.parse(args, defaultProgramArguments) shouldBe None
    }

    "default to None if no webhook-secret given" in {
      val args = Seq(
        "--github-token",
        "my-pass",
        "--github-org",
        "org",
        "--repositories",
        "repo1,repo2",
        "--content-type",
        "application/x-www-form-urlencoded",
        "--webhook-url",
        "hook-url",
        "--events",
        "push,pull_request_review_comment",
        "--verbose"
      )

      val maybeConfig = parser.parse(args, defaultProgramArguments)

      maybeConfig shouldBe Some(
        ProgramArguments(
          githubToken   = Some("my-pass"),
          orgName       = OrganisationName("org"),
          repoNames     = Set(RepositoryName("repo1"), RepositoryName("repo2")),
          contentType   = Some(HookContentType.Form),
          webhookUrl    = Some(Url("hook-url")),
          webhookSecret = None,
          events        = Set(Push, PullRequestReviewComment),
          verbose       = true
        )
      )
    }

    "fail if empty event given" in {
      val args = Seq(
        "--github-token",
        "my-pass",
        "--github-org",
        "org",
        "--repositories",
        "repo1,repo2",
        "--content-type",
        "application/x-www-form-urlencoded",
        "--webhook-url",
        "hook-url",
        "--webhook-secret",
        "S3CR3T",
        "--events",
        " ,pull_request_review_comment",
        "--verbose"
      )

      parser.parse(args, defaultProgramArguments) shouldBe None
    }

    "fail if unknown event given" in {
      val args = Seq(
        "--github-token",
        "my-pass",
        "--github-org",
        "org",
        "--repositories",
        "repo1,repo2",
        "--content-type",
        "application/x-www-form-urlencoded",
        "--webhook-url",
        "hook-url",
        "--webhook-secret",
        "S3CR3T",
        "--events",
        "unknown,pull_request_review_comment",
        "--verbose"
      )

      parser.parse(args, defaultProgramArguments) shouldBe None
    }

    "default to empty Set if no events given" in {
      val args = Seq(
        "--github-token",
        "my-pass",
        "--github-org",
        "org",
        "--repositories",
        "repo1,repo2",
        "--content-type",
        "application/x-www-form-urlencoded",
        "--webhook-url",
        "hook-url",
        "--webhook-secret",
        "S3CR3T",
        "--verbose"
      )

      val maybeConfig = parser.parse(args, defaultProgramArguments)

      maybeConfig shouldBe Some(
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
      )
    }

    "doesn't change to verbose value if not set" in {
      val args = Seq(
        "--github-token",
        "my-pass",
        "--github-org",
        "org",
        "--repositories",
        "repo1,repo2",
        "--content-type",
        "application/x-www-form-urlencoded",
        "--webhook-url",
        "hook-url",
        "--webhook-secret",
        "S3CR3T",
        "--events",
        "push,pull_request_review_comment"
      )

      val maybeConfig = parser.parse(args, defaultProgramArguments)

      maybeConfig shouldBe Some(
        ProgramArguments(
          githubToken   = Some("my-pass"),
          orgName       = OrganisationName("org"),
          repoNames     = Set(RepositoryName("repo1"), RepositoryName("repo2")),
          contentType   = Some(HookContentType.Form),
          webhookUrl    = Some(Url("hook-url")),
          webhookSecret = Some(HookSecret("S3CR3T")),
          events        = Set(Push, PullRequestReviewComment),
          verbose       = defaultProgramArguments.verbose
        )
      )
    }
  }

  private val defaultProgramArguments = ProgramArguments(
    githubToken   = None,
    orgName       = OrganisationName("hmrc"),
    repoNames     = Set.empty,
    contentType   = None,
    webhookUrl    = None,
    webhookSecret = None,
    events        = Set.empty,
    verbose       = false
  )
}
