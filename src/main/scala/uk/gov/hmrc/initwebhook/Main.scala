/*
 * Copyright 2019 HM Revenue & Customs
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

import ch.qos.logback.classic.Level.{DEBUG, INFO}
import ch.qos.logback.classic.Logger
import org.slf4j.LoggerFactory
import uk.gov.hmrc.githubclient._
import uk.gov.hmrc.initwebhook.ArgParser.{ProgramArguments, parser}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

object Main {

  private val timeout = 5 seconds
  private val logger  = LoggerFactory.getLogger(Log.loggerName).asInstanceOf[Logger]

  def main(args: Array[String]) {

    parser.parse(args, ProgramArguments.default) match {
      case Some(ValidatedArgs(githubToken, verbosity, newWebHooks)) =>
        setLogLevel(verbosity)

        new HooksProcessor(GithubApiClient("https://api.github.com", githubToken), logger)
          .createWebHooks(newWebHooks)
          .map { _ =>
            logger.info("Web Hooks created")
            0
          }
          .recover {
            case _: Exception =>
              logger.error("Creation of one or more Web Hooks failed")
              1
          }
          .map(System.exit)
          .await()
      case _ =>
        System.exit(1)
    }
  }

  private def setLogLevel(verbosity: Boolean): Unit =
    if (verbosity) {
      logger.setLevel(DEBUG)
    } else {
      logger.setLevel(INFO)
    }

  implicit class FutureOps(future: Future[Unit]) {
    def await(): Unit = Await.result(future, timeout)
  }
}
