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

package uk.gov.hmrc.dprs.stubs.services

import generated.{AEOI, BREResponse_Type, ErrorDetail_Type, FileError_Type, GenericStatusMessage_Type, RecordError_Type, RequestCommon_Type, RequestDetail_Type, ValidationErrors_Type, ValidationResult_Type}
import org.apache.pekko.Done
import org.apache.pekko.actor.{ActorSystem, Cancellable, Scheduler}
import play.api.{Configuration, Logging}
import play.api.http.Status.NO_CONTENT
import uk.gov.hmrc.dprs.stubs.config.Service
import uk.gov.hmrc.dprs.stubs.models.submission.{SubmissionStatus, SubmissionSummary}
import uk.gov.hmrc.dprs.stubs.repositories.SubmissionRepository
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class SubmissionResultService @Inject() (
                                          configuration: Configuration,
                                          httpClient: HttpClientV2,
                                          actorSystem: ActorSystem,
                                          submissionRepository: SubmissionRepository
                                        )(implicit ec: ExecutionContext) extends Logging {

  private val digitalPlatformReporting: Service = configuration.get[Service]("microservice.services.digital-platform-reporting")
  private val callbackAuthToken: String = configuration.get[String]("result-callback-auth-token")
  private val resultDelay: FiniteDuration = configuration.get[FiniteDuration]("submission-result-delay")

  private val scheduler: Scheduler = actorSystem.scheduler

  def scheduleSuccess(submissionId: String): Cancellable =
    scheduleResponse(submissionId, successfulResponse(submissionId))

  def scheduleFailure(submissionId: String): Cancellable =
    scheduleResponse(submissionId, failedResponse(submissionId))

  private def scheduleResponse(submissionId: String, response: BREResponse_Type): Cancellable = {
    scheduler.scheduleOnce(resultDelay) {

      logger.info(s"Returning callback from submission $submissionId")

      returnResult(submissionId, response).onComplete {
        case Success(_) =>
          logger.info("Successfully sent callback")
        case Failure(e) =>
          logger.error("Problem sending callback", e)
      }
    }
  }

  def scheduleSaveAndRespondSuccess(submission: SubmissionSummary): Cancellable =
    scheduleSaveAndRespond(submission, SubmissionStatus.Success, successfulResponse(submission.submissionId))

  def scheduleSaveAndRespondFailure(submission: SubmissionSummary): Cancellable =
    scheduleSaveAndRespond(submission, SubmissionStatus.Rejected, failedResponse(submission.submissionId))

  private def scheduleSaveAndRespond(submission: SubmissionSummary, endStatus: SubmissionStatus, response: BREResponse_Type): Cancellable = {
    scheduler.scheduleOnce(resultDelay) {

      logger.info(s"Saving submission to repository")

      submissionRepository.create(submission).onComplete {
        case Success(_) =>
          scheduler.scheduleOnce(resultDelay) {

            logger.info(s"Updating status and returning callback from submission ${submission.submissionId}")

            submissionRepository.setStatus(submission.submissionId, endStatus).onComplete {
              case Success(_) =>
                logger.info(s"Updated status of submission id ${submission.submissionId} to $endStatus")

                returnResult(submission.submissionId, response).onComplete {
                  case Success(_) =>
                    logger.info("Successfully sent callback")
                  case Failure(e) =>
                    logger.error("Problem sending callback", e)
                }

              case Failure(e) =>
                logger.error("Problem updating status in repository", e)
            }
          }

        case Failure(e) =>
          logger.error("Problem saving submission to repository", e)
      }
    }
  }

  private def returnResult(submissionId: String, result: BREResponse_Type): Future[Done] = {
    httpClient.post(url"$digitalPlatformReporting/dprs/validation-result")(HeaderCarrier())
      .setHeader(
        "X-Correlation-ID" -> UUID.randomUUID().toString,
        "X-Conversation-ID" -> submissionId,
        "Authorization" -> s"Bearer $callbackAuthToken",
      )
      .withBody(scalaxb.toXML(result, "BREResponse", generated.defaultScope))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case NO_CONTENT =>
            Future.successful(Done)
          case _ =>
            Future.failed(UpstreamErrorResponse("Unexpected response", response.status))
        }
      }
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
