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

package uk.gov.hmrc.dprs.stubs.models.operator

import play.api.libs.json._
import play.api.libs.functional.syntax._
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

final case class NotificationDetails(notificationType: NotificationType,
                                     isActiveSeller: Option[Boolean],
                                     isDueDiligence: Option[Boolean],
                                     firstPeriod: Int,
                                     receivedDateTime: Instant)

object NotificationDetails {

  lazy val writes: OWrites[NotificationDetails] = (
    (__ \ "NotificationType").write[NotificationType] and
    (__ \ "IsActiveSeller").writeNullable[Boolean] and
    (__ \ "IsDueDiligence").writeNullable[Boolean] and
    (__ \ "FirstNotifiedReportingPeriod").write[Int] and
    (__ \ "ReceivedDateTime").write[Instant]
  )(o => (o.notificationType, o.isActiveSeller, o.isDueDiligence, o.firstPeriod, o.receivedDateTime))

  lazy val reads: Reads[NotificationDetails] = (
    (__ \ "NotificationType").read[NotificationType] and
    (__ \ "IsActiveSeller").readNullable[Boolean] and
    (__ \ "IsDueDiligence").readNullable[Boolean] and
    (__ \ "FirstNotifiedReportingPeriod").read[Int] and
    (__ \ "ReceivedDateTime").read[Instant]
  )(NotificationDetails.apply _)

  lazy val mongoWrites: OWrites[NotificationDetails] = (
    (__ \ "NotificationType").write[NotificationType] and
    (__ \ "IsActiveSeller").writeNullable[Boolean] and
    (__ \ "IsDueDiligence").writeNullable[Boolean] and
    (__ \ "FirstNotifiedReportingPeriod").write[Int] and
    (__ \ "ReceivedDateTime").write[Instant](MongoJavatimeFormats.instantWrites)
  )(o => (o.notificationType, o.isActiveSeller, o.isDueDiligence, o.firstPeriod, o.receivedDateTime))

  lazy val mongoReads: Reads[NotificationDetails] = (
    (__ \ "NotificationType").read[NotificationType] and
    (__ \ "IsActiveSeller").readNullable[Boolean] and
    (__ \ "IsDueDiligence").readNullable[Boolean] and
    (__ \ "FirstNotifiedReportingPeriod").read[Int] and
    (__ \ "ReceivedDateTime").read[Instant](MongoJavatimeFormats.instantReads)
  )(NotificationDetails.apply _)

  lazy val mongoFormats: OFormat[NotificationDetails] = OFormat(mongoReads, mongoWrites)
}