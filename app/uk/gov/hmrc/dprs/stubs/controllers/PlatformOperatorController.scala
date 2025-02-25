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

package uk.gov.hmrc.dprs.stubs.controllers

import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.dprs.stubs.models.operator.RequestType
import uk.gov.hmrc.dprs.stubs.models.operator.requests.{CreatePlatformOperatorRequest, DeletePlatformOperatorRequest, UpdatePlatformOperatorRequest}
import uk.gov.hmrc.dprs.stubs.models.operator.responses.{CreatePlatformOperatorResponse, ViewPlatformOperatorsResponse}
import uk.gov.hmrc.dprs.stubs.repositories.PlatformOperatorRepository
import uk.gov.hmrc.dprs.stubs.services.PlatformOperatorService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PlatformOperatorController @Inject() (cc: ControllerComponents,
                                            platformOperatorService: PlatformOperatorService,
                                            repository: PlatformOperatorRepository)(implicit ec: ExecutionContext)
  extends BackendController(cc)
    with Logging {

  def create: Action[JsValue] = Action(parse.json).async { implicit request =>
    (request.body \ "POManagement" \ "RequestCommon" \ "RequestType").as[RequestType] match {
      case RequestType.Create =>
        val createRequest = request.body.as[CreatePlatformOperatorRequest]

        for {
          operator <- platformOperatorService.fromCreateRequest(createRequest)
          _        <- repository.create(operator)
        } yield Ok(Json.toJson(CreatePlatformOperatorResponse(operator.operatorId)))

      case RequestType.Update =>
        val updateRequest = request.body.as[UpdatePlatformOperatorRequest]

        for {
          operator <- platformOperatorService.fromUpdateRequest(updateRequest)
          _        <- repository.update(operator)
        } yield Ok

      case RequestType.Delete =>
        val deleteRequest = request.body.as[DeletePlatformOperatorRequest]

        repository
          .delete(deleteRequest.subscriptionId, deleteRequest.operatorId)
          .map(_ => Ok)
    }
  }

  def view(subscriptionId: String): Action[AnyContent] = Action.async { implicit _ =>
    repository.get(subscriptionId).map { operators =>
      Ok(Json.toJson(ViewPlatformOperatorsResponse(operators)))
    }
  }

  def viewOne(subscriptionId: String, operatorId: String): Action[AnyContent] = Action.async { implicit _ =>
    repository.get(subscriptionId, operatorId)
      .map(_.map { operator =>
        Ok(Json.toJson(ViewPlatformOperatorsResponse(Seq(operator))))
      }.getOrElse(NotFound))
  }
}
