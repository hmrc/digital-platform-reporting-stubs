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
import uk.gov.hmrc.dprs.stubs.models.{ResultFile, SdesFile}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ResultFileRepository @Inject()(
                                      mongoComponent: MongoComponent
                                    )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[ResultFile](
    collectionName = "result-files",
    mongoComponent = mongoComponent,
    domainFormat = ResultFile.mongoFormat,
    indexes = ResultFileRepository.indexes,
    replaceIndexes = true
  ) {

  def save(file: ResultFile): Future[Done] =
    collection.insertOne(file)
      .toFuture()
      .map(_ => Done)

  def list: Future[Seq[ResultFile]] =
    collection.find[ResultFile]()
      .toFuture()

  def get(fileName: String): Future[Option[ResultFile]] =
    collection.find(Filters.equal("fileName", fileName))
      .headOption()
}

object ResultFileRepository {

  val indexes: Seq[IndexModel] =
    Seq(
      IndexModel(
        Indexes.ascending("createdOn"),
        IndexOptions()
          .name("createdOn_ttl_idx")
          .expireAfter(7, TimeUnit.DAYS)
      ),
      IndexModel(
        Indexes.ascending("fileName"),
        IndexOptions()
          .name("fileName_idx")
          .unique(true)
      )
    )
}