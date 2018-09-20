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

import org.scalatest.{Matchers, OptionValues, WordSpec}
import uk.gov.hmrc.initwebhook.ArgParser.Config

class ArgParserSpecs extends WordSpec with Matchers with WireMockEndpoints with OptionValues {

  "ArgParser" should {
    "create correct config" in {

      var args = Seq(
        "--github-username",
        "my-user",
        "--github-password",
        "my-pass",
        "--api-host",
        "http://api.base.url",
        "--org",
        "org",
        "--repo-names",
        "repo1,repo2",
        "--webhook-url",
        "hook-url",
        "--webhook-secret",
        "S3CR3T",
        "--events",
        "push,team_add",
        "--content-type",
        "application/x-www-form-urlencoded"
      )

      val maybeConfig = ArgParser.parser.parse(args, Config())

      maybeConfig.value shouldBe Config(
        githubUsername = "my-user",
        githubPassword = "my-pass",
        gitApiBaseUrl  = "http://api.base.url",
        org            = "org",
        repoNames      = Seq("repo1", "repo2"),
        contentType    = "application/x-www-form-urlencoded",
        webhookUrl     = "hook-url",
        webhookSecret  = Some("S3CR3T"),
        events         = Seq("push", "team_add")
      )

      args = Array(
        """--github-username my-user --github-password my-pass --api-host http://api.base.url --org org --repo-names repo1,repo2 --webhook-url hook-url --content-type application/x-www-form-urlencoded --webhook-secret S3CR3T --events push,team_add """
          .split(" "): _*)

      ArgParser.parser.parse(args, Config()).value shouldBe Config(
        githubUsername = "my-user",
        githubPassword = "my-pass",
        gitApiBaseUrl  = "http://api.base.url",
        org            = "org",
        repoNames      = Seq("repo1", "repo2"),
        contentType    = "application/x-www-form-urlencoded",
        webhookUrl     = "hook-url",
        webhookSecret  = Some("S3CR3T"),
        events         = Seq("push", "team_add")
      )

    }

    "trim spaces in repo names argument" in {

      var args = Seq(
        "--github-username",
        "my-user",
        "--github-password",
        "my-pass",
        "--api-host",
        "http://api.base.url",
        "--org",
        "org",
        "--repo-names",
        " repo1 , repo2 ",
        "--webhook-url",
        "hook-url",
        "--webhook-secret",
        "S3CR3T",
        "--content-type",
        "application/x-www-form-urlencoded",
        "--events",
        "push,team_add"
      )

      val maybeConfig = ArgParser.parser.parse(args, Config())

      maybeConfig.value shouldBe Config(
        githubUsername = "my-user",
        githubPassword = "my-pass",
        gitApiBaseUrl  = "http://api.base.url",
        org            = "org",
        repoNames      = Seq("repo1", "repo2"),
        contentType    = "application/x-www-form-urlencoded",
        webhookUrl     = "hook-url",
        webhookSecret  = Some("S3CR3T"),
        events         = Seq("push", "team_add")
      )

    }

    "webhook secret is optional" in {

      var args = Array(
        """--github-username my-user --github-password my-pass --api-host http://api.base.url --org org --repo-names repo1,repo2 --webhook-url hook-url --content-type application/x-www-form-urlencoded --events push,team_add """
          .split(" "): _*)

      ArgParser.parser.parse(args, Config()).value shouldBe Config(
        githubUsername = "my-user",
        githubPassword = "my-pass",
        gitApiBaseUrl  = "http://api.base.url",
        org            = "org",
        repoNames      = Seq("repo1", "repo2"),
        contentType    = "application/x-www-form-urlencoded",
        webhookUrl     = "hook-url",
        webhookSecret  = None,
        events         = Seq("push", "team_add")
      )
    }

    "only accept json or form as content type" in {
      var args = Array(
        """--github-username my-user --github-password my-pass -h http://api.base.url --org org --repo-names repo1,repo2 --webhook-url hook-url --content-type foo --events push,team_add """
          .split(" "): _*)

      ArgParser.parser.parse(args, Config()) shouldBe None
    }
  }
}
