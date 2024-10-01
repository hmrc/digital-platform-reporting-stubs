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

import generated.DPISubmissionRequest_Type
import play.api.Logging
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.dprs.stubs.services.SubmissionResultService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.xml.NodeSeq

@Singleton
class SubmissionController @Inject()(
                                      cc: ControllerComponents,
                                      submissionResultService: SubmissionResultService
                                    )(implicit val ec: ExecutionContext) extends BackendController(cc) with Logging {

  def submit(): Action[NodeSeq] = Action(parse.xml) { implicit request =>

    val submissionRequest = scalaxb.fromXML[DPISubmissionRequest_Type](request.body)
    val submissionId = submissionRequest.requestCommon.conversationID

    if (submissionRequest.requestDetail.DPI_OECD.MessageSpec.MessageRefId == "fail") {
      submissionResultService.scheduleFailure(submissionId)
    } else {
      submissionResultService.scheduleSuccess(submissionId)
    }

    NoContent
  }
}
