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
import org.mongodb.scala.model._
import play.api.Configuration
import uk.gov.hmrc.dprs.stubs.models.operator.PlatformOperator
import uk.gov.hmrc.dprs.stubs.repositories.PlatformOperatorRepository.indexes
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PlatformOperatorRepository @Inject()(
                                            mongoComponent: MongoComponent,
                                            configuration: Configuration
                                          )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[PlatformOperator](
    collectionName = "platform-operators",
    mongoComponent = mongoComponent,
    domainFormat   = PlatformOperator.mongoFormats,
    indexes        = indexes(configuration),
    replaceIndexes = true
  ) {

  private def byIds(subscriptionId: String, operatorId: String) =
    Filters.and(
      Filters.eq("subscriptionId", subscriptionId),
      Filters.eq("operatorId", operatorId)
    )

  private def bySubscriptionId(id: String) =
    Filters.eq("subscriptionId", id)

  def create(operator: PlatformOperator): Future[Done] =
    collection.insertOne(operator)
      .toFuture()
      .map(_ => Done)

  def update(operator: PlatformOperator): Future[Done] =
    collection.replaceOne(
      filter      = byIds(operator.subscriptionId, operator.operatorId),
      replacement = operator,
      options     = ReplaceOptions().upsert(true)
    )
    .toFuture()
    .map(_ => Done)

  def get(subscriptionId: String): Future[Seq[PlatformOperator]] =
    collection.find(bySubscriptionId(subscriptionId)).toFuture()

  def get(subscriptionId: String, operatorId: String): Future[Option[PlatformOperator]] =
    collection.find(byIds(subscriptionId, operatorId)).headOption()

  def delete(subscriptionId: String, operatorId: String): Future[Done] =
    collection.deleteOne(byIds(subscriptionId, operatorId))
      .toFuture()
      .map(_ => Done)
}

object PlatformOperatorRepository {

  def indexes(configuration: Configuration): Seq[IndexModel] =
    Seq(
      IndexModel(
        Indexes.ascending("subscriptionId", "operatorId"),
        IndexOptions()
          .name("subscriptionId_operatorId_idx")
          .unique(true)
      ),
      IndexModel(
        Indexes.ascending("created"),
        IndexOptions()
          .name("created_ttl_idx")
          .expireAfter(configuration.get[Duration]("mongodb.ttl").toMinutes, TimeUnit.MINUTES)
      )
    )
}
