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

package uk.gov.hmrc.dprs.stubs.repositories

import com.mongodb.client.model.ReturnDocument
import org.mongodb.scala.model.{Filters, FindOneAndUpdateOptions, Updates}
import uk.gov.hmrc.dprs.stubs.models.subscription.SubscriptionId
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionIdRepository @Inject()(mongoComponent: MongoComponent)
                                        (implicit ec: ExecutionContext)
  extends PlayMongoRepository[SubscriptionId](
    collectionName = "subscription-ids",
    mongoComponent = mongoComponent,
    domainFormat   = SubscriptionId.format,
    indexes = Nil
  ) {

  override lazy val requiresTtlIndex: Boolean = false

  def nextSubscriptionId: Future[SubscriptionId] =
    collection.findOneAndUpdate(
        filter  = Filters.eq("_id", SubscriptionId.collectionId),
        update  = Updates.inc("nextId", 1),
        options = FindOneAndUpdateOptions()
          .upsert(true)
          .returnDocument(ReturnDocument.AFTER)
      )
      .toFuture()
}
