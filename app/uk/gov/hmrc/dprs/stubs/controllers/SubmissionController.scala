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
import play.api.libs.json.Json
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.dprs.stubs.models.submission.{SubmissionResponse, SubmissionStatus, SubmissionSummary, ViewSubmissionsRequest}
import uk.gov.hmrc.dprs.stubs.repositories.SubmissionRepository
import uk.gov.hmrc.dprs.stubs.services.SubmissionResultService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.Clock
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.xml.NodeSeq

@Singleton
class SubmissionController @Inject()(
                                      cc: ControllerComponents,
                                      submissionResultService: SubmissionResultService,
                                      submissionRepository: SubmissionRepository,
                                      clock: Clock
                                    )(implicit val ec: ExecutionContext) extends BackendController(cc) with Logging {

  def submit(): Action[NodeSeq] = Action(parse.xml) { implicit request =>

    val submissionRequest = scalaxb.fromXML[DPISubmissionRequest_Type](request.body)
    val submissionSummary = buildSubmissionSummary(submissionRequest)

    if (submissionRequest.requestDetail.DPI_OECD.MessageSpec.MessageRefId == "fail") {
      submissionResultService.scheduleSaveAndRespondFailure(submissionSummary)
    } else {
      submissionResultService.scheduleSaveAndRespondSuccess(submissionSummary)
    }

    NoContent
  }

  def list(): Action[ViewSubmissionsRequest] = Action(parse.json[ViewSubmissionsRequest]).async { implicit request =>
    submissionRepository.list(request.body.subscriptionId).map { submissions =>

      if (submissions.nonEmpty) {
        Ok(Json.toJson(SubmissionResponse(submissions)))
      } else {
        UnprocessableEntity
      }
    }
  }
  
  private def buildSubmissionSummary(request: DPISubmissionRequest_Type): SubmissionSummary =
    SubmissionSummary(
      subscriptionId       = request.requestAdditionalDetail.subscriptionID,
      submissionId         = request.requestCommon.conversationID,
      fileName             = request.requestAdditionalDetail.fileName,
      operatorId           = request.requestDetail.DPI_OECD.MessageSpec.SendingEntityIN.get,
      operatorName         = request.requestDetail.DPI_OECD.DPIBody.head.PlatformOperator.Name.head.value,
      reportingPeriod      = request.requestDetail.DPI_OECD.MessageSpec.ReportingPeriod.getYear.toString,
      submissionDateTime   = clock.instant(),
      submissionStatus     = SubmissionStatus.Pending,
      assumingReporterName = None // TODO
    )
}
