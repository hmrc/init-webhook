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

import java.net.URL

import play.api.libs.json._
import play.api.libs.ws._
import play.api.libs.ws.ning.{NingAsyncHttpClientConfigBuilder, NingWSClient, NingWSClientConfig}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class GithubUrls(orgName: String = "hmrc",
                 apiRoot: String = "https://api.github.com") {

  def webhook(repoName: String) =
    s"$apiRoot/repos/$orgName/$repoName/hooks"
}

class RequestException(request: WSRequest, response: WSResponse)
  extends Exception(s"Got status ${response.status}: ${request.method} ${request.url} ${response.body}") {

}


case class Webhook(id: Int, url: String, config :HookConfig)

case class HookConfig(url : String)

object HookConfig {
  implicit val jsonFormat = Json.format[HookConfig]
}

object Webhook {
  implicit val jsonFormat = Json.format[Webhook]
}

trait Github {

  def githubHttp: GithubHttp

  def githubUrls: GithubUrls

  def getExistingWebhooks(repoName: String): Future[Seq[Webhook]] = {

    githubHttp.get(githubUrls.webhook(repoName)).map{res => res.json.as[Seq[Webhook]] }
  }

  def tryDeleteExistingWebhooks(repoName: String, webhookUrl: String): Future[Seq[Webhook]] = {
    getExistingWebhooks(repoName).flatMap { hooks =>
      Future.sequence(
        hooks.filter(_.config.url == webhookUrl).map(x => githubHttp.delete(x.url).map(_ => x))
      )
    }
  }


  def createWebhook(repoName: String, webhookUrl: String, events: Seq[String]): Future[String] = {
    Log.info(s"creating github webhook for repo '$repoName' with webhook URL '$webhookUrl', with events : ${events.mkString(",")}")

    tryDeleteExistingWebhooks(repoName, webhookUrl).flatMap{_ =>
      val payload = s"""{
                       | "name": "web",
                       | "active": true,
                       | "events": ${Json.toJson(events).toString()},
                       | "config": {
                       |     "url": "$webhookUrl",
                       |     "content_type": "json"
                       | }
                       |}
                 """.stripMargin
      githubHttp.postJsonString(githubUrls.webhook(repoName), payload).map { response =>
        (Json.parse(response) \ "url").as[String]
      }

    }
  }

  def close() = githubHttp.close()
}


trait GithubHttp {

  def creds: ServiceCredentials

  private val ws = new NingWSClient(new NingAsyncHttpClientConfigBuilder(new NingWSClientConfig()).build())

  def close() = {
    ws.close()
    Log.debug("closing github http client")
  }

  def buildJsonCall(method: String, url: URL, body: Option[JsValue] = None): WSRequest = {

    val req = ws.url(url.toString)
      .withMethod(method)
      .withAuth(creds.user, creds.pass, WSAuthScheme.BASIC)
      .withHeaders(
        "content-type" -> "application/json")

    Log.debug("req = " + req)

    body.map { b =>
      req.withBody(b)
    }.getOrElse(req)
  }

  def delete(url: String): Future[WSResponse] = {
    val resultF = buildJsonCall("DELETE", new URL(url)).execute()
    resultF.flatMap { res => res.status match {
      case s if (s >= 200 && s < 300) || (s == 404) => Future.successful(res)
      case _@e => Future.failed(new scala.Exception(s"Didn't get expected status code when reading from Github. Got status ${res.status}: DELETE ${url} ${res.body}"))
    }
    }
  }


  def get(url: String): Future[WSResponse] = {
    val resultF = buildJsonCall("GET", new URL(url)).execute()
    resultF.flatMap { res => res.status match {
      case s if s >= 200 && s < 300 => Future.successful(res)
      case _@e => Future.failed(new scala.Exception(s"Didn't get expected status code when reading from Github. Got status ${res.status}: GET ${url} ${res.body}"))
    }
    }
  }

  def postJsonString(url: String, body: String): Future[String] = {
    buildJsonCall("POST", new URL(url), Some(Json.parse(body))).execute().flatMap { case result =>
      result.status match {
        case s if s >= 200 && s < 300 => Future.successful(result.body)
        case _@e => Future.failed(new scala.Exception(s"Didn't get expected status code when writing to Github. Got status ${result.status}: POST ${url} ${result.body}"))
      }
    }
  }

}
