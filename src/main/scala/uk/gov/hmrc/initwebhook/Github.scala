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
import ImplicitPimps._

import scala.util.{Success, Failure, Try}


class GithubUrls(orgName: String = "hmrc",
                 apiRoot: String = "https://api.github.com") {

  def webhook(repoName: String) =
    s"$apiRoot/repos/$orgName/$repoName/hooks"
}

class RequestException(request: WSRequest, response: WSResponse)
  extends Exception(s"Got status ${response.status}: ${request.method} ${request.url} ${response.body}") {

}


case class Webhook(id: Int, url: String, config: HookConfig)

case class HookConfig(url: String)

object HookConfig {
  implicit val jsonFormat = Json.format[HookConfig]
}

object Webhook {
  implicit val jsonFormat = Json.format[Webhook]
}

trait Github {

  def githubHttp: GithubHttp

  def githubUrls: GithubUrls

  def getExistingWebhooks(repoName: String) = {

    githubHttp.get(githubUrls.webhook(repoName)).map { res => res.json.as[Seq[Webhook]] }.liftToTry
  }

  def tryDeleteExistingWebhooks(repoName: String, webhookUrl: String): Future[Seq[Try[String]]] = {
    getExistingWebhooks(repoName).flatMap {
      case Success(hooks) =>
        Future.traverse(
          hooks.filter(_.config.url == webhookUrl).map(x => githubHttp.delete(x.url).map(_ => x.url))
        )(_.liftToTry)
      case Failure(t) => Future.successful(Seq(Failure(t)))
    }
  }

  def tryCreateWebhook(repoName: String, webhookUrl: String, events: Seq[String]): Future[Try[String]] = {
    Log.info(s"creating github webhook for repo '$repoName' with webhook URL '$webhookUrl', with events : ${events.mkString(",")}")

    tryDeleteExistingWebhooks(repoName, webhookUrl).flatMap { deleteOps =>
      val failedDeletes = deleteOps.filter(_.isFailure)
      if (failedDeletes.isEmpty)
        createHook(repoName, webhookUrl, events).liftToTry
      else Future.successful(Failure(new Exception(s"Failed to create web hook for repo : $repoName")))
    }
  }

  private def createHook(repoName: String, webhookUrl: String, events: Seq[String]): Future[String] = {
    val payload = s"""{
                     |"name": "web",
                     |"active": true,
                     |"events": ${Json.toJson(events).toString()},
                     |"config": {
                     |   "url": "$webhookUrl",
                     |   "content_type": "json"
                     |}
                     |}
                 """.stripMargin
    githubHttp.postJsonString(githubUrls.webhook(repoName), payload).map {
      response =>
        (Json.parse(response) \ "url").as[String]
    }
  }

  def close() = githubHttp.close()
}


trait GithubHttp {

  def
  creds: ServiceCredentials

  private val
  ws = new NingWSClient(new NingAsyncHttpClientConfigBuilder(new NingWSClientConfig()).build())

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
    resultF.map { result => result.status match {
      case s if (s >= 200 && s < 300) || (s == 404) => result
      case _ =>
        val msg = s"Didn't get expected status code when writing to Github. Got status ${result.status}: DELETE ${url} ${result.body}"
        Log.error(msg)
        throw new scala.Exception(msg)
    }
    }
  }


  def get(url: String): Future[WSResponse] = {
    val resultF = buildJsonCall("GET", new URL(url)).execute()
    resultF.map { result => result.status match {
      case s if s >= 200 && s < 300 => result
      case _ =>
        val msg = s"Didn't get expected status code when writing to Github. Got status ${result.status}: GET ${url} ${result.body}"
        Log.error(msg)
        throw new scala.Exception(msg)
    }
    }
  }

  def postJsonString(url: String, body: String): Future[String] = {
    buildJsonCall("POST", new URL(url), Some(Json.parse(body))).execute().map { case result =>
      result.status match {
        case s if s >= 200 && s < 300 => result.body
        case _ =>
          val msg = s"Didn't get expected status code when writing to Github. Got status ${result.status}: POST ${url} ${result.body}"
          Log.error(msg)
          throw new scala.Exception(msg)
      }
    }
  }

}
