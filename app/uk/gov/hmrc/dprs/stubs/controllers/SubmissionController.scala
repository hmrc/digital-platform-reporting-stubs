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

import generated.{DPISubmissionRequest_Type, OtherPlatformOperators_TypeSequence1}
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.dprs.stubs.models.submission.{SubmissionStatus, SubmissionSummary, ViewSubmissionsRequest}
import uk.gov.hmrc.dprs.stubs.repositories.SubmissionRepository
import uk.gov.hmrc.dprs.stubs.services.{ListSubmissionsService, SubmissionResultService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.Clock
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.util.Try
import scala.xml.{NodeSeq, Utility}

@Singleton
class SubmissionController @Inject()(
                                      cc: ControllerComponents,
                                      submissionResultService: SubmissionResultService,
                                      submissionRepository: SubmissionRepository,
                                      listSubmissionsService: ListSubmissionsService,
                                      clock: Clock
                                    )(implicit val ec: ExecutionContext) extends BackendController(cc) with Logging {

  def submit(): Action[NodeSeq] = Action(parse.xml) { implicit request =>

    val submissionRequest = scalaxb.fromXML[DPISubmissionRequest_Type](request.body)
    val submissionSummary = buildSubmissionSummary(submissionRequest)

    if (submissionRequest.requestDetail.DPI_OECD.MessageSpec.MessageRefId == "fail") {
      val numberOfFileErrors = Try(submissionRequest.requestDetail.DPI_OECD.MessageSpec.Warning.get.toInt).getOrElse(1)
      val numberOfRowErrors = Try(submissionRequest.requestDetail.DPI_OECD.MessageSpec.Contact.get.toInt).getOrElse(1)
      submissionResultService.scheduleSaveAndRespondFailure(submissionSummary, numberOfFileErrors, numberOfRowErrors)
    } else {
      submissionResultService.scheduleSaveAndRespondSuccess(submissionSummary)
    }

    NoContent
  }

  def list(): Action[ViewSubmissionsRequest] = Action(parse.json[ViewSubmissionsRequest]).async { implicit request =>
    listSubmissionsService.list(request.body).map { response =>
      if (response.totalResultsCount > 0) {
        Ok(Json.toJson(response))
      } else {
        UnprocessableEntity
      }
    }
  }

  def getByCaseId(caseId: String): Action[AnyContent] = Action.async { implicit request =>
    submissionRepository.getByCaseId(caseId).map {
      _.map { submission =>
        Ok(submission.body)
      }.getOrElse {
        NotFound
      }
    }
  }
  
  private def buildSubmissionSummary(request: DPISubmissionRequest_Type): SubmissionSummary = {

    val assumingOperatorName =
      request.requestDetail.DPI_OECD.DPIBody.find(_.OtherPlatformOperators.isDefined)
        .flatMap { dpiBody =>
          dpiBody.OtherPlatformOperators.flatMap { otherOperators =>
            otherOperators.otherplatformoperators_typeoption.value match {
              case x: OtherPlatformOperators_TypeSequence1 => Some(x.AssumingPlatformOperator.Name.value)
              case _ => None
            }
          }
        }

    SubmissionSummary(
      subscriptionId = request.requestAdditionalDetail.subscriptionID,
      submissionId = request.requestCommon.conversationID,
      fileName = request.requestAdditionalDetail.fileName,
      operatorId = request.requestDetail.DPI_OECD.MessageSpec.SendingEntityIN,
      operatorName = request.requestDetail.DPI_OECD.DPIBody.head.PlatformOperator.Name.headOption.map(_.value),
      reportingPeriod = Some(request.requestDetail.DPI_OECD.MessageSpec.ReportingPeriod.getYear.toString),
      submissionDateTime = clock.instant(),
      submissionCaseId = UUID.randomUUID().toString,
      submissionStatus = SubmissionStatus.Pending,
      assumingReporterName = assumingOperatorName,
      body = Utility.trim(scalaxb.toXML(request.requestDetail.DPI_OECD, Some("urn:oecd:ties:dpi:v1"), Some("DPI_OECD"), generated.defaultScope).head).toString
    )
  }
}
