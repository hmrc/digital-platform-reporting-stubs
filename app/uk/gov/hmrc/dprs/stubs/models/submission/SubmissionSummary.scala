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
                                   operatorId: String,
                                   operatorName: String,
                                   reportingPeriod: String,
                                   submissionDateTime: Instant,
                                   submissionStatus: SubmissionStatus,
                                   assumingReporterName: Option[String])

object SubmissionSummary {

  lazy val mongoFormat: OFormat[SubmissionSummary] = {
    implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat
    Json.format
  }
  
  lazy val downstreamWrites: OWrites[SubmissionSummary] = (
    (__ \ "conversationId").write[String] and
    (__ \ "fileName").write[String] and
    (__ \ "pOId").write[String] and
    (__ \ "pOName").write[String] and
    (__ \ "reportingYear").write[String] and
    (__ \ "submissionDateTime").write[Instant] and
    (__ \ "submissionStatus").write[SubmissionStatus] and
    (__ \ "assumingReporterName").writeNullable[String]
  )(o => (o.submissionId, o.fileName, o.operatorId, o.operatorName, o.reportingPeriod, o.submissionDateTime, o.submissionStatus, o.assumingReporterName))
}
