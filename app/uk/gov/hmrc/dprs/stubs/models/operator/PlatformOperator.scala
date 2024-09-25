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

import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

final case class PlatformOperator(subscriptionId: String,
                                  operatorId: String,
                                  operatorName: String,
                                  tinDetails: Seq[TinDetails],
                                  tradingName: Option[String],
                                  primaryContactDetails: ContactDetails,
                                  secondaryContactDetails: Option[ContactDetails],
                                  addressDetails: AddressDetails,
                                  notifications: Seq[NotificationDetails],
                                  created: Instant,
                                  updated: Instant)

object PlatformOperator extends {

  lazy val mongoFormats: OFormat[PlatformOperator] = {
     implicit val notificationDetailsFormat: OFormat[NotificationDetails] = NotificationDetails.mongoFormats
     implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat
     Json.format
  }

   lazy val writes: OWrites[PlatformOperator] = {

      implicit lazy val notificationDetailsWrites: OWrites[NotificationDetails] =
         NotificationDetails.writes

      (
        (__ \ "POID").write[String] and
        (__ \ "POName").write[String] and
        (__ \ "TINDetails").writeNullable[Seq[TinDetails]] and
        (__ \ "TradingName").writeNullable[String] and
        (__ \ "PrimaryContactDetails").write[ContactDetails] and
        (__ \ "SecondaryContactDetails").writeNullable[ContactDetails] and
        (__ \ "AddressDetails").write[AddressDetails] and
        (__ \ "NotificationsList").writeNullable[Seq[NotificationDetails]]
      )(o => (o.operatorId, o.operatorName, if(o.tinDetails.isEmpty) None else Some(o.tinDetails), o.tradingName, o.primaryContactDetails, o.secondaryContactDetails, o.addressDetails, if(o.notifications.isEmpty) None else Some(o.notifications)))
   }
}
