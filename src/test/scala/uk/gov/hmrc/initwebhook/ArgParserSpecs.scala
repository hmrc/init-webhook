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

import org.scalatest.{Matchers, OptionValues, WordSpec}
import uk.gov.hmrc.initwebhook.ArgParser.Config


class ArgParserSpecs extends WordSpec with Matchers with FutureValues with WireMockEndpoints with OptionValues {


  "ArgParser" should {
    "create correct config" in {


      var args = Array("""-cf /credentials/file -h http://api.base.url -o org -rn repo1,repo2 -wu hook-url -e event1,event2 """.split(" "): _*)


      ArgParser.parser.parse(args, Config()).value shouldBe Config(
        "/credentials/file",
       "http://api.base.url",
        "org",
        Seq("repo1", "repo2"),
        "hook-url",
        Seq("event1", "event2"))

      args = Array("""--cred-file-path /credentials/file --api-host http://api.base.url --org org --repo-names repo1,repo2 --webhook-url hook-url --events event1,event2 """.split(" "): _*)


      ArgParser.parser.parse(args, Config()).value shouldBe Config(
        "/credentials/file",
        "http://api.base.url",
        "org",
        Seq("repo1", "repo2"),
        "hook-url",
        Seq("event1", "event2")
      )

    }
  }
}
