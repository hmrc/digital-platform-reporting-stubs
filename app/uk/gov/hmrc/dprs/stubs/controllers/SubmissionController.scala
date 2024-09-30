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

import generated.{AEOI, BREResponse_Type, DPISubmissionRequest_Type, ErrorDetail_Type, FileError_Type, GenericStatusMessage_Type, RecordError_Type, RequestCommon_Type, RequestDetail_Type, ValidationErrors_Type, ValidationResult_Type}
import org.apache.pekko.actor.{ActorSystem, Scheduler}
import play.api.mvc.{Action, ControllerComponents}
import play.api.{Configuration, Logging}
import uk.gov.hmrc.dprs.stubs.services.SubmissionResultService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}
import scala.xml.NodeSeq

@Singleton
class SubmissionController @Inject()(
                                      cc: ControllerComponents,
                                      configuration: Configuration,
                                      actorSystem: ActorSystem,
                                      submissionResultService: SubmissionResultService
                                    )(implicit val ec: ExecutionContext) extends BackendController(cc) with Logging {

  private val resultDelay: FiniteDuration = configuration.get[FiniteDuration]("submission-result-delay")
  private val scheduler: Scheduler = actorSystem.scheduler

  def submit(): Action[NodeSeq] = Action(parse.xml) { implicit request =>

    val submissionRequest = scalaxb.fromXML[DPISubmissionRequest_Type](request.body)
    val submissionId = submissionRequest.requestCommon.conversationID

    val response = if (submissionRequest.requestDetail.DPI_OECD.MessageSpec.MessageRefId == "fail") {
      failedResponse(submissionId)
    } else {
      successfulResponse(submissionId)
    }

    scheduler.scheduleOnce(resultDelay) {

      logger.info(s"Returning callback from submission $submissionId")

      submissionResultService.returnResult(submissionId, response).onComplete {
        case Success(_) =>
          logger.info("Successfully sent callback")
        case Failure(e) =>
          logger.error("Problem sending callback", e)
      }
    }

    NoContent
  }

  private def successfulResponse(submissionId: String): BREResponse_Type =
    BREResponse_Type(
      requestCommon = RequestCommon_Type(
        receiptDate = scalaxb.Helper.toCalendar(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now())),
        regime = AEOI,
        conversationID = submissionId,
        schemaVersion = "1.0.0"
      ),
      requestDetail = RequestDetail_Type(
        GenericStatusMessage = GenericStatusMessage_Type(
          ValidationErrors = ValidationErrors_Type(
            FileError = Seq.empty,
            RecordError = Seq.empty
          ),
          ValidationResult = ValidationResult_Type(
            Status = generated.Accepted
          )
        )
      )
    )

  private def failedResponse(submissionId: String): BREResponse_Type =
    BREResponse_Type(
      requestCommon = RequestCommon_Type(
        receiptDate = scalaxb.Helper.toCalendar(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now())),
        regime = AEOI,
        conversationID = submissionId,
        schemaVersion = "1.0.0"
      ),
      requestDetail = RequestDetail_Type(
        GenericStatusMessage = GenericStatusMessage_Type(
          ValidationErrors = ValidationErrors_Type(
            FileError = Seq(FileError_Type(
              Code = "001",
              Details = Some(ErrorDetail_Type("detail"))
            )),
            RecordError = Seq(RecordError_Type(
              Code = "002",
              Details = Some(ErrorDetail_Type("detail 2")),
              DocRefIDInError = Seq("1", "2")
            ))
          ),
          ValidationResult = ValidationResult_Type(
            Status = generated.Rejected
          )
        )
      )
    )
}
