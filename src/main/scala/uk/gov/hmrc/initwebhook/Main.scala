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

import java.io.File

import ch.qos.logback.classic.{Level, Logger}
import org.slf4j.LoggerFactory
import uk.gov.hmrc.initwebhook.ArgParser.Config
import uk.gov.hmrc.initwebhook.GitType.{Open, GitType}
import scala.concurrent.ExecutionContext.Implicits.global

import ImplicitPimps._

import scala.concurrent.Future
import scala.util.{Success, Failure, Try}

object Main {

  def buildGithub(credentialsFile: String, apiBaseUrl: String, org: String) = {
    new Github(
      new GithubHttp(ServiceCredentials(credentialsFile)),
      new GithubUrls(apiBaseUrl, org)
    )
  }

  def main(args: Array[String]) {

    ArgParser.parser.parse(args, Config()) foreach { config =>
      val root = LoggerFactory.getLogger(Log.loggerName).asInstanceOf[Logger]
      if (config.verbose) {
        root.setLevel(Level.DEBUG)
      } else {
        root.setLevel(Level.INFO)
      }

      start(config)
    }
  }

  def start(config: Config): Unit = {

    val github = buildGithub(config.credentialsFile, config.gitApiBaseUrl, config.org)

    try {

      val createHooksF = Future.sequence(
        config.repoNames.map(repon => github.tryCreateWebhook(repon, config.webhookUrl, config.events))
      )

      createHooksF.map(_.filter(_.isFailure)).map { failures =>
        val failedMessages: Seq[String] = failures.collect { case Failure(t) => t.getMessage }
        if (failedMessages.nonEmpty) {
          val errorMessage =
            "########### Failure while creating some repository hooks, please see previous errors ############\n" + failedMessages.mkString("\n")

          throw new RuntimeException(errorMessage)
        }
      }.await

    } finally {
      github.close()
    }
  }
}
