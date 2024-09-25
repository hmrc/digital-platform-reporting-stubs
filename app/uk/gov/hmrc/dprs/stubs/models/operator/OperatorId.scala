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

final case class OperatorId(id: Int)

object OperatorId {

  val collectionId = "operatorId"

  private lazy val writes: OWrites[OperatorId] = (
    (__ \ "_id").write[String] and
    (__ \ "nextId").write[Int]
  )(o => (collectionId, o.id))

  private lazy val reads: Reads[OperatorId] =
    (__ \ "nextId").read[Int].map(OperatorId(_))

  implicit lazy val format: OFormat[OperatorId] = OFormat(reads, writes)
}
