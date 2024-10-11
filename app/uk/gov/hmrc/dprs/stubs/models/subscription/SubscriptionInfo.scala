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

package uk.gov.hmrc.dprs.stubs.models.subscription

import play.api.libs.functional.syntax._
import play.api.libs.json._

final case class SubscriptionInfo(id: String,
                                  gbUser: Boolean,
                                  tradingName: Option[String],
                                  primaryContact: Contact,
                                  secondaryContact: Option[Contact])

object SubscriptionInfo {
  
  implicit lazy val writes: OWrites[SubscriptionInfo] = (
    (__ \ "success" \ "customer" \ "id").write[String] and
    (__ \ "success" \ "customer" \ "gbUser").write[Boolean] and
    (__ \ "success" \ "customer" \ "tradingName").writeNullable[String] and
    (__ \ "success" \ "customer" \ "primaryContact").write[Contact] and
    (__ \ "success" \ "customer" \ "secondaryContact").writeNullable[Contact]
  )(o => (o.id, o.gbUser, o.tradingName, o.primaryContact, o.secondaryContact))

  def apply(subscription: Subscription): SubscriptionInfo =
    SubscriptionInfo(
      id = subscription._id,
      gbUser = subscription.gbUser,
      tradingName = subscription.tradingName,
      primaryContact = subscription.primaryContact,
      secondaryContact = subscription.secondaryContact
    )
}
