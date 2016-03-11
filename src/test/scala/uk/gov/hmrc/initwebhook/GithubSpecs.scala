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

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.client.{MappingBuilder, RequestPatternBuilder, ResponseDefinitionBuilder}
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.http.RequestMethod._
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json



class GithubSpecs extends WordSpec with Matchers with FutureValues with WireMockEndpoints {

  class FakeGithubHttp extends GithubHttp {
    override def creds: ServiceCredentials = ServiceCredentials("", "")
  }

  val github: Github = new Github{
    override def githubHttp: GithubHttp = new FakeGithubHttp()

    override def githubUrls: GithubUrls = new GithubUrls(apiRoot = endpointMockUrl)
  }

  "Github.createWebhook" should {
    "successfully create webhook" in {
      val gitHubResponse =
        """
          |{
          |   "id": 1,
          |   "url": "https://api.github.com/repos/hmrc/domain/hooks/1",
          |   "test_url": "https://api.github.com/repos/hmrc/domain/hooks/1/test",
          |   "ping_url": "https://api.github.com/repos/hmrc/domain/hooks/1/pings"
          |}
        """.stripMargin

      givenGitHubExpects(
        method = POST,
        url = "/repos/hmrc/domain/hooks",
        willRespondWith = (200, Some(gitHubResponse))
      )

      val webhookResponse = github.createWebhook("domain", "http://webhookurl").await

      assertRequest(
        method = POST,
        url = "/repos/hmrc/domain/hooks",
        jsonBody = Some(s"""{
                           |    "name": "web",
                           |    "active": true,
                           |    "events": [
                           |        "issues",
                           |        "pull_request",
                           |        "pull_request_review_comment",
                           |        "release",
                           |        "status"
                           |    ],
                           |    "config": {
                           |        "url": "http://webhookurl",
                           |        "content_type": "json"
                           |    }
                           |}
                 """.stripMargin)
      )


      webhookResponse shouldBe "https://api.github.com/repos/hmrc/domain/hooks/1"
    }
  }

  case class GithubRequest(method:RequestMethod, url:String, body:Option[String]){

    {
      body.foreach { b => Json.parse(b) }
    }

    def req:RequestPatternBuilder = {
      val builder = new RequestPatternBuilder(method, urlEqualTo(url))
      body.map{ b =>
        builder.withRequestBody(equalToJson(b))
      }.getOrElse(builder)
    }
  }

  def assertRequest(
                     method:RequestMethod,
                     url:String,
                     extraHeaders:Map[String,String] = Map(),
                     jsonBody:Option[String]): Unit ={
    val builder = new RequestPatternBuilder(method, urlEqualTo(url))
    extraHeaders.foreach { case(k, v) =>
      builder.withHeader(k, equalTo(v))
    }

    jsonBody.map{ b =>
      builder.withRequestBody(equalToJson(b))
    }.getOrElse(builder)
    endpointMock.verifyThat(builder)
  }

  def assertRequest(req:GithubRequest): Unit ={
    endpointMock.verifyThat(req.req)
  }

  def givenGitHubExpects(
                          method:RequestMethod,
                          url:String,
                          extraHeaders:Map[String,String] = Map(),
                          willRespondWith: (Int, Option[String])): Unit = {

    val builder = new MappingBuilder(method, urlEqualTo(url))
      .withHeader("Content-Type", equalTo("application/json"))
      

    val response: ResponseDefinitionBuilder = new ResponseDefinitionBuilder()
      .withStatus(willRespondWith._1)

    val resp = willRespondWith._2.map { b =>
      response.withBody(b)
    }.getOrElse(response)

    builder.willReturn(resp)

    endpointMock.register(builder)
  }
}
