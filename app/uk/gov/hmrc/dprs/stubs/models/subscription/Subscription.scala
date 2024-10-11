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

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.{Clock, Instant}

final case class Subscription(_id: String,
                              gbUser: Boolean,
                              tradingName: Option[String],
                              primaryContact: Contact,
                              secondaryContact: Option[Contact],
                              updated: Instant)

object Subscription extends MongoJavatimeFormats.Implicits {

  implicit lazy val format: OFormat[Subscription] = Json.format

  def apply(request: CreateSubscriptionRequest, subscriptionId: String, clock: Clock): Subscription =
    Subscription(
      _id = subscriptionId,
      gbUser = request.gbUser,
      tradingName = request.tradingName,
      primaryContact = request.primaryContact,
      secondaryContact = request.secondaryContact,
      updated = clock.instant
    )

  def apply(request: UpdateSubscriptionRequest, clock: Clock): Subscription =
    Subscription(
      _id = request.id,
      gbUser = request.gbUser,
      tradingName = request.tradingName,
      primaryContact = request.primaryContact,
      secondaryContact = request.secondaryContact,
      updated = clock.instant
    )
}
