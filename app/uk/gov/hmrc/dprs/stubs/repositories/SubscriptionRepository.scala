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

import org.apache.pekko.Done
import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.model._
import uk.gov.hmrc.dprs.stubs.models.subscription.Subscription
import play.api.Configuration
import uk.gov.hmrc.dprs.stubs.repositories.SubscriptionRepository.indexes
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration

@Singleton
class SubscriptionRepository @Inject()(mongoComponent: MongoComponent,
                                       configuration: Configuration)
                                      (implicit ec: ExecutionContext)
  extends PlayMongoRepository[Subscription](
    collectionName = "subscriptions",
    mongoComponent = mongoComponent,
    domainFormat   = Subscription.format,
    indexes        = indexes(configuration),
    replaceIndexes = true
  ) {

  def create(subscription: Subscription): Future[Done] =
    collection.insertOne(subscription)
      .toFuture()
      .map(_ => Done)

  def update(subscription: Subscription): Future[Done] =
    collection.replaceOne(
      filter = Filters.eq("_id", subscription._id),
      replacement = subscription
    )
      .toFuture()
      .map(_ => Done)

  def get(subscriptionId: String): Future[Option[Subscription]] =
    collection.find(Filters.eq("_id", subscriptionId)).headOption()
}

object SubscriptionRepository {

  def indexes(configuration: Configuration): Seq[IndexModel] =
    Seq(
      IndexModel(
        Indexes.ascending("updated"),
        IndexOptions()
          .name("updated")
          .expireAfter(configuration.get[Duration]("mongodb.ttl").toMinutes, TimeUnit.MINUTES)
      )
    )
}