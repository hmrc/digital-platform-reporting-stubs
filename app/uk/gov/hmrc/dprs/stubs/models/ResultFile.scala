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

package uk.gov.hmrc.dprs.stubs.models

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.mongo.play.json.formats.{MongoBinaryFormats, MongoJavatimeFormats}

import java.time.Instant

final case class ResultFile(
                             fileName: String,
                             bytes: Array[Byte],
                             size: Long,
                             metadata: Map[String, String],
                             createdOn: Instant
                           )

object ResultFile extends MongoJavatimeFormats.Implicits with MongoBinaryFormats.Implicits {

  final case class WithoutContents(
                                    fileName: String,
                                    size: Long,
                                    metadata: Map[String, String],
                                    createdOn: Instant
                                  )

  object WithoutContents {

    lazy val mongoFormat: OFormat[WithoutContents] = Json.format[WithoutContents]
  }

  lazy val mongoFormat: OFormat[ResultFile] = Json.format[ResultFile]
}