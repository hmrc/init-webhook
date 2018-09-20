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

import java.net.URL

import play.api.libs.ws._
import play.api.libs.ws.ning.{NingAsyncHttpClientConfigBuilder, NingWSClient, NingWSClientConfig}
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class GithubHttp(username: String, password: String) {

  private val ws = new NingWSClient(new NingAsyncHttpClientConfigBuilder(new NingWSClientConfig()).build())

  def close() = {
    ws.close()
    Log.debug("closing github http client")
  }

  def buildJsonCall(method: String, url: URL, body: Option[JsValue] = None): WSRequest = {

    val req = ws
      .url(url.toString)
      .withMethod(method)
      .withAuth(username, password, WSAuthScheme.BASIC)
      .withHeaders("content-type" -> "application/json")

    Log.debug("req = " + req)

    body
      .map { b =>
        req.withBody(b)
      }
      .getOrElse(req)
  }

  def delete(url: String): Future[WSResponse] = {
    val resultF = buildJsonCall("DELETE", new URL(url)).execute()
    resultF.map { result =>
      result.status match {
        case s if (s >= 200 && s < 300) || (s == 404) => result
        case _ =>
          val msg =
            s"Didn't get expected status code when writing to Github. Got status ${result.status}: DELETE $url ${result.body}"
          Log.error(msg)
          throw new scala.Exception(msg)
      }
    }
  }

  def get(url: String): Future[WSResponse] = {
    val resultF = buildJsonCall("GET", new URL(url)).execute()
    resultF.map { result =>
      result.status match {
        case s if s >= 200 && s < 300 => result
        case _ =>
          val msg =
            s"Didn't get expected status code when writing to Github. Got status ${result.status}: GET $url ${result.body}"
          Log.error(msg)
          throw new scala.Exception(msg)
      }
    }
  }

  def postJsonString(url: String, body: String): Future[String] =
    buildJsonCall("POST", new URL(url), Some(Json.parse(body))).execute().map {
      case result =>
        result.status match {
          case s if s >= 200 && s < 300 => result.body
          case _ =>
            val msg =
              s"Didn't get expected status code when writing to Github. Got status ${result.status}: POST $url ${result.body}"
            Log.error(msg)
            throw new scala.Exception(msg)
        }
    }

}
