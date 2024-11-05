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

import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.dprs.stubs.models.submission.SortOrder._
import uk.gov.hmrc.dprs.stubs.models.submission.SubmissionSortBy._
import uk.gov.hmrc.dprs.stubs.models.submission.SubmissionStatus._
import uk.gov.hmrc.dprs.stubs.models.submission.{SubmissionSummary, ViewSubmissionsRequest}
import uk.gov.hmrc.dprs.stubs.repositories.SubmissionRepository

import java.time.Instant
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ListSubmissionsServiceSpec extends AnyFreeSpec with Matchers with MockitoSugar with BeforeAndAfterEach with ScalaFutures {

  private val mockRepository = mock[SubmissionRepository]

  override def beforeEach(): Unit = {
    Mockito.reset(mockRepository)
    super.beforeEach()
  }

  private val service = new ListSubmissionsService(mockRepository)

  "list" - {

    val instant = Instant.ofEpochSecond(1)

    val submission = SubmissionSummary(
      subscriptionId = "subscriptionId",
      submissionId = "1",
      fileName = "file1",
      operatorId = "1",
      operatorName = "name",
      reportingPeriod = "2024",
      submissionDateTime = instant,
      submissionCaseId = "id",
      submissionStatus = Success,
      assumingReporterName = None,
      body = "body"
    )

    "must filter requests by whether or not we want assumed reports" in {

      val submission1 = submission
      val submission2 = submission.copy(assumingReporterName = Some("name"))

      when(mockRepository.list(eqTo("subscriptionId"))) thenReturn Future.successful(Seq(submission1, submission2))

      val request1 = ViewSubmissionsRequest("subscriptionId", false, 1, SubmissionDate, Ascending, None, None, None, Nil)
      val request2 = ViewSubmissionsRequest("subscriptionId", true, 1, SubmissionDate, Ascending, None, None, None, Nil)

      val result1 = service.list(request1).futureValue
      val result2 = service.list(request2).futureValue
      result1.matchingResultsCount mustEqual 1
      result2.matchingResultsCount mustEqual 1
      result1.submissions must contain only submission1
      result2.submissions must contain only submission2
      result1.totalResultsCount mustEqual 2
      result2.totalResultsCount mustEqual 2
    }

    "must filter results by reporting period" in {

      val submission1 = submission
      val submission2 = submission.copy(reportingPeriod = "2025")

      when(mockRepository.list(eqTo("subscriptionId"))) thenReturn Future.successful(Seq(submission1, submission2))

      val request = ViewSubmissionsRequest("subscriptionId", false, 1, SubmissionDate, Ascending, Some("2024"), None, None, Nil)

      val result = service.list(request).futureValue
      result.matchingResultsCount mustEqual 1
      result.submissions must contain only submission1
      result.totalResultsCount mustEqual 2
    }

    "must filter results by operatorId" in {

      val submission1 = submission
      val submission2 = submission.copy(operatorId = "2")

      when(mockRepository.list(eqTo("subscriptionId"))) thenReturn Future.successful(Seq(submission1, submission2))

      val request = ViewSubmissionsRequest("subscriptionId", false, 1, SubmissionDate, Ascending, None, Some("1"), None, Nil)

      val result = service.list(request).futureValue
      result.matchingResultsCount mustEqual 1
      result.submissions must contain only submission1
      result.totalResultsCount mustEqual 2
    }

    "must filter results by filename" in {

      val submission1 = submission
      val submission2 = submission.copy(fileName = "file2")

      when(mockRepository.list(eqTo("subscriptionId"))) thenReturn Future.successful(Seq(submission1, submission2))

      val request = ViewSubmissionsRequest("subscriptionId", false, 1, SubmissionDate, Ascending, None, None, Some("file1"), Nil)

      val result = service.list(request).futureValue
      result.matchingResultsCount mustEqual 1
      result.submissions must contain only submission1
      result.totalResultsCount mustEqual 2
    }

    "must filter requests by statuses" in {

      val submission1 = submission
      val submission2 = submission.copy(submissionStatus = Rejected)
      val submission3 = submission.copy(submissionStatus = Pending)

      when(mockRepository.list(eqTo("subscriptionId"))) thenReturn Future.successful(Seq(submission1, submission2, submission3))

      val request1 = ViewSubmissionsRequest("subscriptionId", false, 1, SubmissionDate, Ascending, None, None, None, Seq(Success))
      val request2 = ViewSubmissionsRequest("subscriptionId", false, 1, SubmissionDate, Ascending, None, None, None, Seq(Success, Pending, Rejected))

      val result1 = service.list(request1).futureValue
      val result2 = service.list(request2).futureValue
      result1.matchingResultsCount mustEqual 1
      result2.matchingResultsCount mustEqual 3
      result1.submissions must contain only submission1
      result2.submissions must contain theSameElementsAs Seq(submission1, submission2, submission3)
      result1.totalResultsCount mustEqual 3
      result2.totalResultsCount mustEqual 3
    }

    "must sort by submission date" in {

      val submission1 = submission
      val submission2 = submission.copy(submissionDateTime = instant.plusSeconds(1))
      val submission3 = submission.copy(submissionDateTime = instant.plusSeconds(2))

      when(mockRepository.list(eqTo("subscriptionId"))) thenReturn Future.successful(Seq(submission1, submission2, submission3))

      val request1 = ViewSubmissionsRequest("subscriptionId", false, 1, SubmissionDate, Ascending, None, None, None, Nil)
      val request2 = ViewSubmissionsRequest("subscriptionId", false, 1, SubmissionDate, Descending, None, None, None, Nil)

      val result1 = service.list(request1).futureValue
      val result2 = service.list(request2).futureValue
      result1.matchingResultsCount mustEqual 3
      result2.matchingResultsCount mustEqual 3
      result1.submissions must contain theSameElementsInOrderAs Seq(submission1, submission2, submission3)
      result2.submissions must contain theSameElementsInOrderAs Seq(submission3, submission2, submission1)
      result1.totalResultsCount mustEqual 3
      result2.totalResultsCount mustEqual 3
    }

    "must sort by operator id" in {

      val submission1 = submission
      val submission2 = submission.copy(operatorId = "2")
      val submission3 = submission.copy(operatorId = "3")

      when(mockRepository.list(eqTo("subscriptionId"))) thenReturn Future.successful(Seq(submission1, submission2, submission3))

      val request1 = ViewSubmissionsRequest("subscriptionId", false, 1, PlatformOperator, Ascending, None, None, None, Nil)
      val request2 = ViewSubmissionsRequest("subscriptionId", false, 1, PlatformOperator, Descending, None, None, None, Nil)

      val result1 = service.list(request1).futureValue
      val result2 = service.list(request2).futureValue
      result1.matchingResultsCount mustEqual 3
      result2.matchingResultsCount mustEqual 3
      result1.submissions must contain theSameElementsInOrderAs Seq(submission1, submission2, submission3)
      result2.submissions must contain theSameElementsInOrderAs Seq(submission3, submission2, submission1)
      result1.totalResultsCount mustEqual 3
      result2.totalResultsCount mustEqual 3
    }

    "must sort by reporting period" in {

      val submission1 = submission
      val submission2 = submission.copy(reportingPeriod = "2025")
      val submission3 = submission.copy(reportingPeriod = "2026")

      when(mockRepository.list(eqTo("subscriptionId"))) thenReturn Future.successful(Seq(submission1, submission2, submission3))

      val request1 = ViewSubmissionsRequest("subscriptionId", false, 1, ReportingPeriod, Ascending, None, None, None, Nil)
      val request2 = ViewSubmissionsRequest("subscriptionId", false, 1, ReportingPeriod, Descending, None, None, None, Nil)

      val result1 = service.list(request1).futureValue
      val result2 = service.list(request2).futureValue
      result1.matchingResultsCount mustEqual 3
      result2.matchingResultsCount mustEqual 3
      result1.submissions must contain theSameElementsInOrderAs Seq(submission1, submission2, submission3)
      result2.submissions must contain theSameElementsInOrderAs Seq(submission3, submission2, submission1)
      result1.totalResultsCount mustEqual 3
      result2.totalResultsCount mustEqual 3
    }

    "must return results in pages of 10" in {

      val submissions = (1 to 95).map { i =>
        submission.copy(submissionDateTime = instant.plusSeconds(i), submissionId = i.toString)
      }

      when(mockRepository.list(eqTo("subscriptionId"))) thenReturn Future.successful(submissions)

      val request1 = ViewSubmissionsRequest("subscriptionId", false, 1, SubmissionDate, Ascending, None, None, None, Nil)
      val request2 = ViewSubmissionsRequest("subscriptionId", false, 2, SubmissionDate, Ascending, None, None, None, Nil)
      val request3 = ViewSubmissionsRequest("subscriptionId", false, 10, SubmissionDate, Ascending, None, None, None, Nil)

      val result1 = service.list(request1).futureValue
      val result2 = service.list(request2).futureValue
      val result3 = service.list(request3).futureValue
      result1.matchingResultsCount mustEqual 95
      result2.matchingResultsCount mustEqual 95
      result3.matchingResultsCount mustEqual 95
      result1.submissions.size mustEqual 10
      result2.submissions.size mustEqual 10
      result3.submissions.size mustEqual 5
      result1.submissions must contain theSameElementsInOrderAs (1 to 10).map(i => submission.copy(submissionDateTime = instant.plusSeconds(i), submissionId = i.toString))
      result2.submissions must contain theSameElementsInOrderAs (11 to 20).map(i => submission.copy(submissionDateTime = instant.plusSeconds(i), submissionId = i.toString))
      result3.submissions must contain theSameElementsInOrderAs (91 to 95).map(i => submission.copy(submissionDateTime = instant.plusSeconds(i), submissionId = i.toString))
      result1.totalResultsCount mustEqual 95
      result2.totalResultsCount mustEqual 95
      result3.totalResultsCount mustEqual 95
    }

    "must return an empty list when no records exist" in {

      when(mockRepository.list(eqTo("subscriptionId"))) thenReturn Future.successful(Nil)

      val request = ViewSubmissionsRequest("subscriptionId", false, 1, SubmissionDate, Ascending, None, None, None, Nil)

      val result = service.list(request).futureValue
      result.matchingResultsCount mustEqual 0
      result.totalResultsCount mustEqual 0
      result.submissions mustBe empty
    }

    "must return an empty list when records exist but none match the filter" in {

      when(mockRepository.list(eqTo("subscriptionId"))) thenReturn Future.successful(Seq(submission))

      val request = ViewSubmissionsRequest("subscriptionId", false, 1, SubmissionDate, Ascending, None, Some("other operator id"), None, Nil)

      val result = service.list(request).futureValue
      result.matchingResultsCount mustEqual 0
      result.totalResultsCount mustEqual 1
      result.submissions mustBe empty
    }
  }
}
