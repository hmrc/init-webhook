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
import java.nio.file.Files
import java.util.concurrent.TimeUnit

import ch.qos.logback.classic.{Level, Logger}
import org.slf4j.LoggerFactory
import uk.gov.hmrc.initwebhook.ArgParser.Config

import scala.concurrent.Await
import scala.concurrent.duration.Duration


object Main {

  def findGithubCreds(): ServiceCredentials = {
    val githubCredsFile = System.getProperty("user.home") + "/.github/.credentials"
    val githubCredsOpt = CredentialsFinder.findGithubCredsInFile(new File(githubCredsFile).toPath)
    val creds = githubCredsOpt.getOrElse(throw new scala.IllegalArgumentException(s"Did not find valid Github credentials in ${githubCredsFile}"))

    Log.debug(s"github client_id ${creds.user}")
    Log.debug(s"github client_secret ${creds.pass.takeRight(3)}*******")

    creds
  }


  def buildGithub() = new Github {

    override val githubHttp: GithubHttp = new GithubHttp {
      override val creds: ServiceCredentials = findGithubCreds()
    }

    override val githubUrls: GithubUrls = new GithubUrls()
  }

  def main(args: Array[String]) {

    ArgParser.parser.parse(args, Config()) foreach { config =>
      val root = LoggerFactory.getLogger(Log.loggerName).asInstanceOf[Logger]
      if (config.verbose) {
        root.setLevel(Level.DEBUG)
      } else {
        root.setLevel(Level.INFO)
      }

      start(config.repoName, config.teamName, config.webhookUrl)
    }
  }

  def start(newRepoName: String, team: String, webhookUrl: String): Unit = {

    val github = buildGithub()

    try {
      val result = github.createWebhook(newRepoName, webhookUrl)
      Await.result(result, Duration(60, TimeUnit.SECONDS))
    } finally {
      github.close()
    }
  }
}
