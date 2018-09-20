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

import ch.qos.logback.classic.{Level, Logger}
import org.slf4j.LoggerFactory
import uk.gov.hmrc.initwebhook.ArgParser.Config
import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.Failure

object Main {

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
    val github =
      new Github(new GithubHttp(config.githubUsername, config.githubPassword), config.gitApiBaseUrl, config.org)
    val webHookCreateConfig = WebHookCreateConfig(config.webhookUrl, config.webhookSecret, config.contentType)

    try {

      val createHooksFuture = Future.sequence(
        config.repoNames.map(repoName => github.tryCreateWebhook(repoName, webHookCreateConfig, config.events))
      )

      Await.result(
        createHooksFuture
          .map(_.filter(_.isFailure))
          .map { failures =>
            val failedMessages: Seq[String] = failures.collect { case Failure(t) => t.getMessage }
            if (failedMessages.nonEmpty) {
              val errorMessage =
                "########### Failure while creating some repository hooks, please see previous errors ############\n" + failedMessages
                  .mkString("\n")

              throw new RuntimeException(errorMessage)
            }
          },
        30.seconds
      )

    } finally {
      github.close()
    }
  }
}
