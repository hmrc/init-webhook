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

import org.scalamock.scalatest.MockFactory
import org.scalatest.Matchers._
import org.scalatest.WordSpec
import org.slf4j.Logger
import uk.gov.hmrc.githubclient.HookEvent.{PullRequestReviewComment, Push}
import uk.gov.hmrc.githubclient._

import scala.concurrent.ExecutionContext.Implicits.{global => executionContext}
import scala.concurrent.{ExecutionContext, Future}

class HooksProcessorSpec extends WordSpec with MockFactory {

  "createWebHooks" should {

    "findExisting hooks, delete them and recreate" in new Setup {
      (gitHubClient
        .findHooks(_: OrganisationName, _: RepositoryName)(_: ExecutionContext))
        .expects(newWebHook.organisationName, newWebHook.repositoryName, executionContext)
        .returning(Future.successful(Set(existingWebHook)))

      (gitHubClient
        .deleteHook(_: OrganisationName, _: RepositoryName, _: HookId)(_: ExecutionContext))
        .expects(newWebHook.organisationName, newWebHook.repositoryName, existingWebHook.id, executionContext)
        .returning(Future.successful(()))

      (gitHubClient
        .createWebHook(_: OrganisationName, _: RepositoryName, _: HookConfig, _: Set[HookEvent], _: Boolean)(
          _: ExecutionContext))
        .expects(
          newWebHook.organisationName,
          newWebHook.repositoryName,
          HookConfig(newWebHook.webHookUrl, Some(newWebHook.contentType), newWebHook.secret),
          newWebHook.events,
          true,
          executionContext
        )
        .returning(Future.successful(existingWebHook.copy(id = HookId(2))))

      (logger
        .info(_: String))
        .expects(s"WebHook with url: ${newWebHook.webHookUrl} created")

      hooksProcessor.createWebHooks(Set(newWebHook)).await()
    }

    "findExisting hooks and create one if found hooks are not Web Hooks" in new Setup {
      (gitHubClient
        .findHooks(_: OrganisationName, _: RepositoryName)(_: ExecutionContext))
        .expects(newWebHook.organisationName, newWebHook.repositoryName, executionContext)
        .returning(
          Future.successful(
            Set(existingWebHook.copy(config = existingWebHook.config.copy(url = Url("http://different"))))
          )
        )

      (gitHubClient
        .createWebHook(_: OrganisationName, _: RepositoryName, _: HookConfig, _: Set[HookEvent], _: Boolean)(
          _: ExecutionContext))
        .expects(
          newWebHook.organisationName,
          newWebHook.repositoryName,
          HookConfig(newWebHook.webHookUrl, Some(newWebHook.contentType), newWebHook.secret),
          newWebHook.events,
          true,
          executionContext
        )
        .returning(Future.successful(existingWebHook.copy(id = HookId(2))))

      (logger
        .info(_: String))
        .expects(s"WebHook with url: ${newWebHook.webHookUrl} created")

      hooksProcessor.createWebHooks(Set(newWebHook)).await()
    }

    "findExisting hooks and create one if found web hook has different url" in new Setup {
      (gitHubClient
        .findHooks(_: OrganisationName, _: RepositoryName)(_: ExecutionContext))
        .expects(newWebHook.organisationName, newWebHook.repositoryName, executionContext)
        .returning(Future.successful(Set(existingNonWebHook)))

      (gitHubClient
        .createWebHook(_: OrganisationName, _: RepositoryName, _: HookConfig, _: Set[HookEvent], _: Boolean)(
          _: ExecutionContext))
        .expects(
          newWebHook.organisationName,
          newWebHook.repositoryName,
          HookConfig(newWebHook.webHookUrl, Some(newWebHook.contentType), newWebHook.secret),
          newWebHook.events,
          true,
          executionContext
        )
        .returning(Future.successful(existingWebHook.copy(id = HookId(2))))

      (logger
        .info(_: String))
        .expects(s"WebHook with url: ${newWebHook.webHookUrl} created")

      hooksProcessor.createWebHooks(Set(newWebHook)).await()
    }

    "findExisting hooks and create one - case with more than one new webHooks" in new Setup {
      (gitHubClient
        .findHooks(_: OrganisationName, _: RepositoryName)(_: ExecutionContext))
        .expects(newWebHook.organisationName, newWebHook.repositoryName, executionContext)
        .returning(
          Future.successful(
            Set(existingWebHook.copy(name = HookName("non-web")))
          )
        )

      (gitHubClient
        .findHooks(_: OrganisationName, _: RepositoryName)(_: ExecutionContext))
        .expects(otherNewWebHook.organisationName, otherNewWebHook.repositoryName, executionContext)
        .returning(
          Future.successful(
            Set(otherExistingWebHook.copy(config = otherExistingWebHook.config.copy(url = Url("http://different"))))
          )
        )

      (gitHubClient
        .createWebHook(_: OrganisationName, _: RepositoryName, _: HookConfig, _: Set[HookEvent], _: Boolean)(
          _: ExecutionContext))
        .expects(
          newWebHook.organisationName,
          newWebHook.repositoryName,
          HookConfig(newWebHook.webHookUrl, Some(newWebHook.contentType), newWebHook.secret),
          newWebHook.events,
          true,
          executionContext
        )
        .returning(Future.successful(existingWebHook.copy(id = HookId(2))))

      (gitHubClient
        .createWebHook(_: OrganisationName, _: RepositoryName, _: HookConfig, _: Set[HookEvent], _: Boolean)(
          _: ExecutionContext))
        .expects(
          otherNewWebHook.organisationName,
          otherNewWebHook.repositoryName,
          HookConfig(otherNewWebHook.webHookUrl, Some(otherNewWebHook.contentType), otherNewWebHook.secret),
          otherNewWebHook.events,
          true,
          executionContext
        )
        .returning(Future.successful(otherExistingWebHook.copy(id = HookId(3))))

      (logger
        .info(_: String))
        .expects(s"WebHook with url: ${newWebHook.webHookUrl} created")
      (logger
        .info(_: String))
        .expects(s"WebHook with url: ${otherNewWebHook.webHookUrl} created")

      hooksProcessor.createWebHooks(Set(newWebHook, otherNewWebHook)).await()
    }

    "fail if findExisting hooks call fails" in new Setup {
      val exception = new RuntimeException("some error")

      (gitHubClient
        .findHooks(_: OrganisationName, _: RepositoryName)(_: ExecutionContext))
        .expects(newWebHook.organisationName, newWebHook.repositoryName, executionContext)
        .returning(Future.failed(exception))

      (logger
        .error(_: String, _: Throwable))
        .expects(s"WebHook with url: ${newWebHook.webHookUrl} not created", exception)

      intercept[Exception] {
        hooksProcessor.createWebHooks(Set(newWebHook)).await()
      } shouldBe exception
    }

    "fail if delete hook fails" in new Setup {
      (gitHubClient
        .findHooks(_: OrganisationName, _: RepositoryName)(_: ExecutionContext))
        .expects(newWebHook.organisationName, newWebHook.repositoryName, executionContext)
        .returning(Future.successful(Set(existingWebHook)))

      val exception = new RuntimeException("some error")
      (gitHubClient
        .deleteHook(_: OrganisationName, _: RepositoryName, _: HookId)(_: ExecutionContext))
        .expects(newWebHook.organisationName, newWebHook.repositoryName, existingWebHook.id, executionContext)
        .returning(Future.failed(exception))

      (logger
        .error(_: String, _: Throwable))
        .expects(s"WebHook with url: ${newWebHook.webHookUrl} not created", exception)

      intercept[Exception] {
        hooksProcessor.createWebHooks(Set(newWebHook)).await()
      } shouldBe exception
    }

    "fail if create hook fails" in new Setup {
      (gitHubClient
        .findHooks(_: OrganisationName, _: RepositoryName)(_: ExecutionContext))
        .expects(newWebHook.organisationName, newWebHook.repositoryName, executionContext)
        .returning(Future.successful(Set(existingWebHook)))

      (gitHubClient
        .deleteHook(_: OrganisationName, _: RepositoryName, _: HookId)(_: ExecutionContext))
        .expects(newWebHook.organisationName, newWebHook.repositoryName, existingWebHook.id, executionContext)
        .returning(Future.successful(()))

      val exception = new RuntimeException("some error")
      (gitHubClient
        .createWebHook(_: OrganisationName, _: RepositoryName, _: HookConfig, _: Set[HookEvent], _: Boolean)(
          _: ExecutionContext))
        .expects(
          newWebHook.organisationName,
          newWebHook.repositoryName,
          HookConfig(newWebHook.webHookUrl, Some(newWebHook.contentType), newWebHook.secret),
          newWebHook.events,
          true,
          executionContext
        )
        .returning(Future.failed(exception))

      (logger
        .error(_: String, _: Throwable))
        .expects(s"WebHook with url: ${newWebHook.webHookUrl} not created", exception)

      intercept[Exception] {
        hooksProcessor.createWebHooks(Set(newWebHook)).await()
      } shouldBe exception
    }
  }

  private trait Setup {
    val newWebHook = NewWebHook(
      OrganisationName("org"),
      RepositoryName("repo1"),
      HookContentType.Form,
      Url("hook-url"),
      Some(HookSecret("S3CR3T")),
      Set(Push, PullRequestReviewComment)
    )

    val existingWebHook = Hook(
      HookId(1),
      Url("http://github.com/url"),
      HookName.Web,
      active = true,
      HookConfig(newWebHook.webHookUrl)
    )

    val otherNewWebHook = NewWebHook(
      OrganisationName("org"),
      RepositoryName("repo2"),
      HookContentType.Form,
      Url("hook-url2"),
      Some(HookSecret("S3CR3T2")),
      Set(Push)
    )

    val otherExistingWebHook = Hook(
      HookId(2),
      Url("http://github.com/url/2"),
      HookName.Web,
      active = true,
      HookConfig(otherNewWebHook.webHookUrl)
    )

    val existingNonWebHook = Hook(
      HookId(1),
      Url("http://github.com/url"),
      HookName("other hook type"),
      active = true,
      HookConfig(newWebHook.webHookUrl)
    )

    val gitHubClient: GithubApiClient = mock[GithubApiClient]
    val logger: Logger                = mock[Logger]
    val hooksProcessor                = new HooksProcessor(gitHubClient, logger)
  }

  private implicit class FutureOps[T](future: Future[T]) {

    import scala.concurrent.Await
    import scala.concurrent.duration._
    import scala.language.postfixOps

    def await(): T = Await.result(future, 1 second)
  }

}
