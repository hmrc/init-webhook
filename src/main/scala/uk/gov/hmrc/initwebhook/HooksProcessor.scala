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

import org.slf4j.Logger
import uk.gov.hmrc.githubclient.{GithubApiClient, Hook, HookConfig, HookName}

import scala.concurrent.{ExecutionContext, Future}

private class HooksProcessor(githubClient: GithubApiClient, logger: Logger) {

  import githubClient._

  def createWebHooks(newWebhooks: Set[NewWebHook])(implicit executionContext: ExecutionContext): Future[Unit] =
    Future
      .sequence(newWebhooks.toSeq map recreateHook)
      .map(_ => ())

  private def recreateHook(newWebHook: NewWebHook)(implicit executionContext: ExecutionContext): Future[Unit] =
    for {
      allFoundHooks <- findHooks(newWebHook.organisationName, newWebHook.repositoryName).logEventualError(newWebHook)
      _             <- deleteMatchingWebHook(allFoundHooks, newWebHook).logEventualError(newWebHook)
      _             <- create(newWebHook).logEventualError(newWebHook)
    } yield ()

  private def deleteMatchingWebHook(allFoundHooks: Set[Hook], newWebHook: NewWebHook)(
    implicit executionContext: ExecutionContext): Future[Unit] =
    allFoundHooks.find(hook => hook.name == HookName.Web && hook.config.url == newWebHook.webHookUrl) match {
      case Some(matchingHook) =>
        deleteHook(newWebHook.organisationName, newWebHook.repositoryName, matchingHook.id)
      case None =>
        Future.successful(())
    }

  private def create(newWebHook: NewWebHook)(implicit executionContext: ExecutionContext): Future[Unit] =
    createWebHook(
      newWebHook.organisationName,
      newWebHook.repositoryName,
      HookConfig(newWebHook.webHookUrl, Some(newWebHook.contentType), newWebHook.secret),
      newWebHook.events
    ) map { _ =>
      logger.info(s"WebHook with url: ${newWebHook.webHookUrl} created")
    }

  private implicit class FutureOps[T](future: Future[T])(implicit executionContext: ExecutionContext) {
    def logEventualError(newWebHook: NewWebHook): Future[T] = future.recoverWith {
      case exception: Throwable =>
        logger.error(s"WebHook with url: ${newWebHook.webHookUrl} not created", exception)
        future
    }
  }
}
