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

class ArgParserSpecs extends WordSpec with Matchers with FutureValues with WireMockEndpoints with OptionValues {

  "ArgParser" should {
    "create correct config" in {

      var args = Seq(
        "-cf",
        "/credentials/file",
        "-h",
        "http://api.base.url",
        "-o",
        "org",
        "-rn",
        "repo1,repo2",
        "-wu",
        "hook-url",
        "-ws",
        "S3CR3T",
        "-e",
        "push,team_add",
        "-ct",
        "form"
      )

      val maybeConfig = ArgParser.parser.parse(args, Config())

      maybeConfig.value shouldBe Config(
        credentialsFile = "/credentials/file",
        gitApiBaseUrl   = "http://api.base.url",
        org             = "org",
        repoNames       = Seq("repo1", "repo2"),
        contentType     = "form",
        webhookUrl      = "hook-url",
        webhookSecret   = Some("S3CR3T"),
        events          = Seq("push", "team_add")
      )

      args = Array(
        """--cred-file-path /credentials/file --api-host http://api.base.url --org org --repo-names repo1,repo2 --webhook-url hook-url --content-type form --webhook-secret S3CR3T --events push,team_add """
          .split(" "): _*)

      ArgParser.parser.parse(args, Config()).value shouldBe Config(
        credentialsFile = "/credentials/file",
        gitApiBaseUrl   = "http://api.base.url",
        org             = "org",
        repoNames       = Seq("repo1", "repo2"),
        contentType     = "form",
        webhookUrl      = "hook-url",
        webhookSecret   = Some("S3CR3T"),
        events          = Seq("push", "team_add")
      )

    }

    "trim spaces in repo names argument" in {

      var args = Seq(
        "-cf",
        "/credentials/file",
        "-h",
        "http://api.base.url",
        "-o",
        "org",
        "-rn",
        " repo1 , repo2 ",
        "-wu",
        "hook-url",
        "-ws",
        "S3CR3T",
        "-ct",
        "form",
        "-e",
        "push,team_add"
      )

      val maybeConfig = ArgParser.parser.parse(args, Config())

      maybeConfig.value shouldBe Config(
        credentialsFile = "/credentials/file",
        gitApiBaseUrl   = "http://api.base.url",
        org             = "org",
        repoNames       = Seq("repo1", "repo2"),
        contentType     = "form",
        webhookUrl      = "hook-url",
        webhookSecret   = Some("S3CR3T"),
        events          = Seq("push", "team_add")
      )

    }

    "webhook secret is optional" in {

      var args = Array(
        """-cf /credentials/file -h http://api.base.url -o org -rn repo1,repo2 -wu hook-url -ct form -e push,team_add """
          .split(" "): _*)

      ArgParser.parser.parse(args, Config()).value shouldBe Config(
        credentialsFile = "/credentials/file",
        gitApiBaseUrl   = "http://api.base.url",
        org             = "org",
        repoNames       = Seq("repo1", "repo2"),
        contentType     = "form",
        webhookUrl      = "hook-url",
        webhookSecret   = None,
        events          = Seq("push", "team_add")
      )
    }

    "only accept json or form as content type" in {
      var args = Array(
        """-cf /credentials/file -h http://api.base.url -o org -rn repo1,repo2 -wu hook-url -ct foo -e push,team_add """
          .split(" "): _*)

      ArgParser.parser.parse(args, Config()) shouldBe None
    }
  }
}
