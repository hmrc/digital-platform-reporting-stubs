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

package uk.gov.hmrc.dprs.stubs.models.operator.responses

import play.api.libs.functional.syntax._
import play.api.libs.json.{OWrites, __}

final case class CreatePlatformOperatorResponse(operatorId: String)

object CreatePlatformOperatorResponse {

  implicit lazy val writes: OWrites[CreatePlatformOperatorResponse] =
    (
      (__ \ "success" \ "ReturnParameters" \ "Value").write[String] and
      (__ \ "success" \ "ReturnParameters" \ "Key").write[String]
    )(o => (o.operatorId, "POID"))
}
