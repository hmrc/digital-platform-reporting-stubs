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
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

final case class SubmissionSummary(subscriptionId: String,
                                   submissionId: String,
                                   fileName: String,
                                   operatorId: Option[String],
                                   operatorName: Option[String],
                                   reportingPeriod: Option[String],
                                   submissionDateTime: Instant,
                                   submissionCaseId: String,
                                   submissionStatus: SubmissionStatus,
                                   assumingReporterName: Option[String],
                                   body: String)

object SubmissionSummary {

  lazy val mongoFormat: OFormat[SubmissionSummary] = {
    implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat
    Json.format
  }
  
  lazy val downstreamWrites: OWrites[SubmissionSummary] = (
    (__ \ "conversationId").write[String] and
    (__ \ "fileName").write[String] and
    (__ \ "pOId").writeNullable[String] and
    (__ \ "pOName").writeNullable[String] and
    (__ \ "reportingYear").writeNullable[String] and
    (__ \ "submissionDateTime").write[Instant] and
    (__ \ "submissionCaseId").write[String] and
    (__ \ "submissionStatus").write[SubmissionStatus] and
    (__ \ "assumingReporterName").writeNullable[String]
  )(o => (o.submissionId, o.fileName, o.operatorId, o.operatorName, o.reportingPeriod, o.submissionDateTime, o.submissionCaseId, o.submissionStatus, o.assumingReporterName))
}
