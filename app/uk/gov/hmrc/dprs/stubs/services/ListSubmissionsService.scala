/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.dprs.stubs.services

import uk.gov.hmrc.dprs.stubs.models.submission.SortOrder.Ascending
import uk.gov.hmrc.dprs.stubs.models.submission.SubmissionSortBy.{PlatformOperator, ReportingPeriod, SubmissionDate}
import uk.gov.hmrc.dprs.stubs.models.submission.{SubmissionResponse, SubmissionSummary, ViewSubmissionsRequest}
import uk.gov.hmrc.dprs.stubs.repositories.SubmissionRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ListSubmissionsService @Inject()(repository: SubmissionRepository)
                                      (implicit ec: ExecutionContext) {

  def list(request: ViewSubmissionsRequest): Future[SubmissionResponse] =
    repository.list(request.subscriptionId).map { submissions =>

      val reportingPeriodFilter: SubmissionSummary => Boolean = { submission =>
        request.reportingPeriod.forall(_ == submission.reportingPeriod)
      }

      val operatorIdFilter: SubmissionSummary => Boolean = { submission =>
        request.operatorId.forall(_ == submission.operatorId)
      }

      val fileNameFilter: SubmissionSummary => Boolean = { submission =>
        request.fileName.forall(_ == submission.fileName)
      }

      val statusFilter: SubmissionSummary => Boolean = { submission =>
        if (request.statuses.nonEmpty) request.statuses.contains(submission.submissionStatus) else true
      }

      val filteredSubmissions =
        submissions
          .filter(_.assumingReporterName.isDefined == request.assumedReporting)
          .filter(reportingPeriodFilter)
          .filter(operatorIdFilter)
          .filter(fileNameFilter)
          .filter(statusFilter)

      val preSortedSubmissions = request.sortBy match {
        case SubmissionDate   => filteredSubmissions.sortBy(_.submissionDateTime)
        case PlatformOperator => filteredSubmissions.sortBy(_.operatorId)
        case ReportingPeriod  => filteredSubmissions.sortBy(_.reportingPeriod)
      }

      val sortedSubmissions = if (request.sortOrder == Ascending) preSortedSubmissions else preSortedSubmissions.reverse
      val resultsCount = sortedSubmissions.size
//      val paginatedResults = sortedSubmissions.slice((request.pageNumber - 1) * 10, (request.pageNumber) * 10)
//
//      SubmissionResponse(resultsCount, paginatedResults)
      SubmissionResponse(resultsCount, sortedSubmissions)
    }
}