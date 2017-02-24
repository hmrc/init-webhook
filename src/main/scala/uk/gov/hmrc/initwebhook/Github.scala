/*
 * Copyright 2017 HM Revenue & Customs
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


import play.api.libs.json._
import play.api.libs.ws._
import uk.gov.hmrc.initwebhook.ImplicitPimps._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}




class RequestException(request: WSRequest, response: WSResponse)
  extends Exception(s"Got status ${response.status}: ${request.method} ${request.url} ${response.body}")

case class WebHookCreateConfig(webhookUrl: String,
                               webhookSecret: Option[String])

case class Webhook(id: Int, name: String, url: String, config: HookConfig)

//hooks api return services as well which have domain instead of url that's why option
case class HookConfig(url: Option[String])

object HookConfig {

  implicit val jsonFormat = Json.format[HookConfig]

}

object Webhook {
  implicit val jsonFormat = Json.format[Webhook]
}




object GitType extends Enumeration {
  type GitType = Value
  val Open , Enterprise = Value
}


class Github(githubHttp: GithubHttp,githubUrls: GithubUrls) {

  def getExistingWebhooks(repoName: String) = {

    githubHttp.get(githubUrls.webhook(repoName)).map {
      res => res.json.as[Seq[Webhook]]
    }.liftToTry
  }

  def tryDeleteExistingWebhooks(repoName: String, webhookUrl: String): Future[Seq[Try[String]]] = {
    getExistingWebhooks(repoName).flatMap {
      case Success(hooks) =>
        Future.traverse(
          hooks
            .filter(x => x.name == "web" && x.config.url.contains(webhookUrl))
            .map(x => githubHttp.delete(x.url).map(_ => x.url))
        )(_.liftToTry)
      case Failure(t) => Future.successful(Seq(Failure(t)))
    }
  }

  def tryCreateWebhook(repoName: String, webHookCreateConfig:WebHookCreateConfig, events: Seq[String]): Future[Try[String]] = {
    import webHookCreateConfig._
    Log.info(s"creating github webhook for repo '$repoName' with webhook URL '$webhookUrl', with events : ${events.mkString(",")}")

    tryDeleteExistingWebhooks(repoName, webhookUrl).flatMap { deleteOps =>
      val failedDeletes = deleteOps.filter(_.isFailure)
      if (failedDeletes.isEmpty)
        createHook(repoName, webHookCreateConfig, events).liftToTry
      else Future.successful(Failure(new Exception(s"Failed to create web hook for repo : $repoName")))
    }
  }

  private def createHook(repoName: String, webHookCreateConfig: WebHookCreateConfig, events: Seq[String]): Future[String] = {
    import webHookCreateConfig._
    val secretEntry = webhookSecret.fold("")(_ => s""" "secret": "${webhookSecret.get}",""")
    val payload = s"""{
                     |"name": "web",
                     |"active": true,
                     |"events": ${Json.toJson(events).toString()},
                     |"config": {
                     | "url": "$webhookUrl",
                     | $secretEntry
                     | "content_type": "json"
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
