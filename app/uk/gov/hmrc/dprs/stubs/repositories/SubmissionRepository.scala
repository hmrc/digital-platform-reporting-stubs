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
import play.api.Configuration
import uk.gov.hmrc.dprs.stubs.models.submission.{SubmissionStatus, SubmissionSummary}
import uk.gov.hmrc.dprs.stubs.repositories.SubmissionRepository.indexes
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration

class SubmissionRepository @Inject()(
                                      mongoComponent: MongoComponent,
                                      configuration: Configuration
                                    )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[SubmissionSummary](
    collectionName = "submissions",
    mongoComponent = mongoComponent,
    domainFormat   = SubmissionSummary.format,
    indexes        = indexes(configuration)
  ) {

  def create(submission: SubmissionSummary): Future[Done] =
    collection.insertOne(submission)
      .toFuture()
      .map(_ => Done)

  def setStatus(submissionId: String, status: SubmissionStatus): Future[Done] =
    collection.updateOne(
      filter = Filters.eq("submissionId", submissionId),
      update = Updates.set("status", status)
    )
      .toFuture()
      .map(_ => Done)

  def list(subscriptionId: String): Future[Seq[SubmissionSummary]] =
    collection.find(Filters.eq("subscriptionId", subscriptionId)).toFuture()
}

object SubmissionRepository {

  def indexes(configuration: Configuration): Seq[IndexModel] =
    Seq(
      IndexModel(
        Indexes.ascending("subscriptionId", "submissionId"),
        IndexOptions()
          .name("subscriptionId_subscriptionId_idx")
          .unique(true)
      ),
      IndexModel(
        Indexes.ascending("submissionDateTime"),
        IndexOptions()
          .name("submissionDateTime_ttl_idx")
          .expireAfter(configuration.get[Duration]("mongodb.ttl").toMinutes, TimeUnit.MINUTES)
      )
    )
}