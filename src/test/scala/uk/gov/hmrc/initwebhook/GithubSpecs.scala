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

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global


class GithubSpecs extends WordSpec with Matchers with FutureValues with WireMockEndpoints {

  class FakeGithubHttp extends GithubHttp {
    override def creds: ServiceCredentials = ServiceCredentials("", "")
  }

  val github: Github = new Github{
    override def githubHttp: GithubHttp = new FakeGithubHttp()

    override def githubUrls: GithubUrls = new GithubUrls(apiRoot = endpointMockUrl)
  }


  val notificationUrl = "http://webhookurl"

  val githubGetAllHooksReponse =
    s"""
       |[
       |{
       |    "id": 1,
       |    "url": "${endpointMockUrl}/repos/hmrc/domain/hooks/1",
       |    "test_url": "https://api.github.com/repos/hmrc/domain/hooks/1/test",
       |    "ping_url": "https://api.github.com/repos/hmrc/domain/hooks/1/pings",
       |    "name": "web",
       |    "events": [
       |      "push",
       |      "pull_request"
       |    ],
       |    "active": true,
       |    "config": {
       |      "url": "$notificationUrl",
       |      "content_type": "json"
       |    }
       |  },
       |{
       |  "id": 2,
       |  "url": "https://api.github.com/repos/hmrc/domain/hooks/2",
       |  "test_url": "https://api.github.com/repos/hmrc/domain/hooks/2/test",
       |  "ping_url": "https://api.github.com/repos/hmrc/domain/hooks/2/pings",
       |  "name": "web",
       |  "events": [
       |    "push",
       |    "pull_request"
       |  ],
       |  "active": true,
       |  "config": {
       |    "url": "http://someother/url",
       |    "content_type": "json"
       |  }
       |}
       |]
        """.stripMargin


  "Github" should {
    "createWebhook successfully create webhook when not already exists" in {
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
        method = GET,
        url = "/repos/hmrc/domain/hooks",
        willRespondWith = (200, Some("[]"))
      )

      givenGitHubExpects(
        method = POST,
        url = "/repos/hmrc/domain/hooks",
        willRespondWith = (200, Some(gitHubResponse))
      )

      val webhookResponse = github.createWebhook("domain", "http://webhookurl", Seq("event1", "event2", "event3")).await

      assertRequest(
        method = POST,
        url = "/repos/hmrc/domain/hooks",
        jsonBody = Some(s"""{
                           |    "name": "web",
                           |    "active": true,
                           |    "events": ["event1","event2","event3"],
                           |    "config": {
                           |        "url": "http://webhookurl",
                           |        "content_type": "json"
                           |    }
                           |}
                 """.stripMargin)
      )


      webhookResponse shouldBe "https://api.github.com/repos/hmrc/domain/hooks/1"
    }


    "createWebhook successfully deletes the existing web hook and re creates with new events" in {


      val gitHubCreateResponse =
        """
          |{
          |   "id": 1,
          |   "url": "https://api.github.com/repos/hmrc/domain/hooks/1",
          |   "test_url": "https://api.github.com/repos/hmrc/domain/hooks/1/test",
          |   "ping_url": "https://api.github.com/repos/hmrc/domain/hooks/1/pings"
          |}
        """.stripMargin



      givenGitHubExpects(
        method = GET,
        url = "/repos/hmrc/domain/hooks",
        willRespondWith = (200, Some(githubGetAllHooksReponse))
      )

      givenGitHubExpects(
        method = DELETE,
        url = "/repos/hmrc/domain/hooks/1",
        willRespondWith = (200, None)
      )

      givenGitHubExpects(
        method = POST,
        url = "/repos/hmrc/domain/hooks",
        willRespondWith = (200, Some(gitHubCreateResponse))
      )


      val webhookResponse = github.createWebhook("domain", "http://webhookurl", Seq("event1", "event2", "event3")).await


      assertRequest(
        method = DELETE,
        url = "/repos/hmrc/domain/hooks/1",
        jsonBody = None
      )

      assertRequest(
        method = POST,
        url = "/repos/hmrc/domain/hooks",
        jsonBody = Some(s"""{
                           |    "name": "web",
                           |    "active": true,
                           |    "events": ["event1","event2","event3"],
                           |    "config": {
                           |        "url": "http://webhookurl",
                           |        "content_type": "json"
                           |    }
                           |}
                 """.stripMargin)
      )


      webhookResponse shouldBe "https://api.github.com/repos/hmrc/domain/hooks/1"
    }


    "not create web hook if can't remove the existing webhook" in {

      givenGitHubExpects(
        method = GET,
        url = "/repos/hmrc/domain/hooks",
        willRespondWith = (200, Some(githubGetAllHooksReponse))
      )

      givenGitHubExpects(
        method = DELETE,
        url = "/repos/hmrc/domain/hooks/1",
        willRespondWith = (400, None)
      )

      github.createWebhook("domain", "http://webhookurl", Seq("event1", "event2", "event3")).onComplete{
          case Success(value) => fail("")
          case Failure(e) =>
      }

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
