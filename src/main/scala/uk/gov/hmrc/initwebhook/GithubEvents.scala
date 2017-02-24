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


object GithubEvents extends Enumeration {
  val commit_comment,
  create,
  delete,
  deployment,
  deployment_status,
  download,
  follow,
  fork,
  fork_apply,
  gist,
  gollum,
  issue_comment,
  issues,
  label,
  member,
  membership,
  milestone,
  organization,
  org_block,
  page_build,
  project_card,
  project_column,
  project,
  public,
  pull_request,
  pull_request_review,
  pull_request_review_comment,
  push,
  release,
  repository,
  status,
  team,
  team_add,
  watch = Value


  val defaultEvents = Seq(issues, pull_request, pull_request_review_comment, release, status).map(_.toString)

  def withNames(names: Seq[String]) = names.map(GithubEvents.withName)

}
