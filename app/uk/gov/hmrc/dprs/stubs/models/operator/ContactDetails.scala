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

final case class ContactDetails(
                                 phoneNumber: Option[String],
                                 contactName: String,
                                 emailAddress: String
                               )

object ContactDetails {

  implicit lazy val writes: OWrites[ContactDetails] = (
    (__ \ "PhoneNumber").writeNullable[String] and
    (__ \ "ContactName").write[String] and
    (__ \ "EmailAddress").write[String]
  )(o => (o.phoneNumber, o.contactName, o.emailAddress))

  implicit lazy val reads: Reads[ContactDetails] = (
    (__ \ "PhoneNumber").readNullable[String] and
    (__ \ "ContactName").read[String] and
    (__ \ "EmailAddress").read[String]
  )(ContactDetails.apply _)
}
