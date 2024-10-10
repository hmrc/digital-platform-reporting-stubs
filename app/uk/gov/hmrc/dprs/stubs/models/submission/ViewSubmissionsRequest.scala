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

package uk.gov.hmrc.dprs.stubs.models.submission

import play.api.libs.functional.syntax._
import play.api.libs.json._

final case class ViewSubmissionsRequest(subscriptionId: String,
                                        assumedReporting: Boolean,
                                        pageNumber: Int,
                                        sortBy: SubmissionSortBy,
                                        sortOrder: SortOrder,
                                        reportingPeriod: Option[String],
                                        operatorId: Option[String],
                                        fileName: Option[String],
                                        statuses: Seq[SubmissionStatus])

object ViewSubmissionsRequest {
  
  implicit lazy val reads: Reads[ViewSubmissionsRequest] = (
    (__ \ "submissionsListRequest" \ "requestDetails" \ "subscriptionId").read[String] and
    (__ \ "submissionsListRequest" \ "requestDetails" \ "isManual").read[Boolean] and
    (__ \ "submissionsListRequest" \ "requestDetails" \ "pageNumber").read[Int] and
    (__ \ "submissionsListRequest" \ "requestDetails" \ "sortBy").read[SubmissionSortBy] and
    (__ \ "submissionsListRequest" \ "requestDetails" \ "sortOrder").read[SortOrder] and
    (__ \ "submissionsListRequest" \ "requestDetails" \ "reportingYear").readNullable[String] and
    (__ \ "submissionsListRequest" \ "requestDetails" \ "pOId").readNullable[String] and
    (__ \ "submissionsListRequest" \ "requestDetails" \ "fileName").readNullable[String] and
    (__ \ "submissionsListRequest" \ "requestDetails" \ "submissionStatus").readNullable[String]
  )((subscriptionId, isManual, pageNumber, sortBy, sortOrder, reportingYear, poId, fileName, submissionStatus) =>
    ViewSubmissionsRequest(
      subscriptionId,
      isManual,
      pageNumber,
      sortBy,
      sortOrder,
      reportingYear,
      poId,
      fileName,
      submissionStatus.map { status =>
        status.split(',').map(x => SubmissionStatus.values.find(_.entryName == x).get).toSeq
      }.getOrElse(Nil)
    ))
}