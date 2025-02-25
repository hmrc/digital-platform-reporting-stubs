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

import com.google.common.base.Charsets
import generated.{AEOI, BREResponse_Type, ErrorDetail_Type, FileError_Type, GenericStatusMessage_Type, RecordError_Type, RequestCommon_Type, RequestDetail_Type, ValidationErrors_Type, ValidationResult_Type}
import org.apache.pekko.Done
import org.apache.pekko.actor.{ActorSystem, Cancellable, Scheduler}
import play.api.http.Status.NO_CONTENT
import play.api.libs.json.Json
import play.api.libs.ws.writeableOf_JsValue
import play.api.{Configuration, Logging}
import uk.gov.hmrc.dprs.stubs.config.Service
import uk.gov.hmrc.dprs.stubs.models.ResultFile
import uk.gov.hmrc.dprs.stubs.models.sdes.{NotificationCallback, NotificationType}
import uk.gov.hmrc.dprs.stubs.models.submission.{SubmissionStatus, SubmissionSummary}
import uk.gov.hmrc.dprs.stubs.repositories.{ResultFileRepository, SubmissionRepository}
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.objectstore.client.play.Implicits._
import uk.gov.hmrc.objectstore.client.play.PlayObjectStoreClient
import uk.gov.hmrc.objectstore.client.{Path, RetentionPeriod}

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime}
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class SubmissionResultService @Inject()(
                                         configuration: Configuration,
                                         httpClient: HttpClientV2,
                                         actorSystem: ActorSystem,
                                         submissionRepository: SubmissionRepository,
                                         resultFilesRepository: ResultFileRepository,
                                         objectStoreClient: PlayObjectStoreClient
                                       )(implicit ec: ExecutionContext) extends Logging {

  private val digitalPlatformReporting: Service        = configuration.get[Service]("microservice.services.digital-platform-reporting")
  private val callbackAuthToken:        String         = configuration.get[String]("result-callback-auth-token")
  private val resultDelay:              FiniteDuration = configuration.get[FiniteDuration]("submission-result-delay")

  private val scheduler: Scheduler = actorSystem.scheduler

  def scheduleSuccess(submissionId: String)(implicit hc: HeaderCarrier): Cancellable =
    scheduleResponse(submissionId, successfulResponse(submissionId))

  private def scheduleResponse(submissionId: String, response: BREResponse_Type)(implicit hc: HeaderCarrier): Cancellable =
    scheduler.scheduleOnce(resultDelay) {

      logger.info(s"Returning callback from submission $submissionId")

      returnResult(submissionId, response).onComplete {
        case Success(_) =>
          logger.info("Successfully sent callback")
        case Failure(e) =>
          logger.error("Problem sending callback", e)
      }
    }

  def scheduleSaveAndRespondSuccess(submission: SubmissionSummary)(implicit hc: HeaderCarrier): Cancellable =
    scheduleSaveAndRespond(submission, SubmissionStatus.Success, successfulResponse(submission.submissionId))

  def scheduleSaveAndRespondFailure(submission: SubmissionSummary, numberOfFileErrors: Int, numberOfRowErrors: Int)(implicit
    hc:                                         HeaderCarrier
  ): Cancellable =
    scheduleSaveAndRespond(submission, SubmissionStatus.Rejected, failedResponse(submission.submissionId, numberOfFileErrors, numberOfRowErrors))

  private def scheduleSaveAndRespond(submission: SubmissionSummary, endStatus: SubmissionStatus, response: BREResponse_Type)(implicit
    hc:                                          HeaderCarrier
  ): Cancellable =
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

  private def returnResult(submissionId: String, result: BREResponse_Type)(implicit hc: HeaderCarrier): Future[Done] = {

    val xml   = scalaxb.toXML(result, "BREResponse", generated.defaultScope)
    val bytes = xml.toString.getBytes(Charsets.UTF_8)

    logger.info(s"Result response is ${bytes.size} long")
    if (bytes.size <= 3_000_000) {
      httpClient
        .post(url"$digitalPlatformReporting/dprs/validation-result")(HeaderCarrier())
        .setHeader(
          "X-Correlation-ID"  -> UUID.randomUUID().toString,
          "X-Conversation-ID" -> submissionId,
          "Authorization"     -> s"Bearer $callbackAuthToken"
        )
        .withBody(xml)
        .execute[HttpResponse]
        .flatMap { response =>
          response.status match {
            case NO_CONTENT =>
              Future.successful(Done)
            case _ =>
              Future.failed(UpstreamErrorResponse("Unexpected response", response.status))
          }
        }
    } else {

      val resultFile = ResultFile(
        fileName = UUID.randomUUID().toString,
        size = bytes.size,
        metadata = Map.empty,
        createdOn = Instant.now()
      )

      val notification = NotificationCallback(
        notification = NotificationType.FileReady,
        filename = resultFile.fileName,
        correlationID = UUID.randomUUID().toString,
        failureReason = None
      )

      for {
        _ <- resultFilesRepository.save(resultFile)
        _ <- objectStoreClient.putObject(Path.File(Path.Directory("results"), resultFile.fileName), bytes, RetentionPeriod.OneDay)
        _ <- sendNotification(notification)(HeaderCarrier())
      } yield Done
    }
  }

  private def sendNotification(notification: NotificationCallback)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    httpClient
      .post(url"$digitalPlatformReporting/sdes/submission/callback")
      .withBody(Json.toJson(notification))
      .execute

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

  private def failedResponse(submissionId: String, numberOfFileErrors: Int, numberOfRowErrors: Int): BREResponse_Type =
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
            FileError = (0 until numberOfFileErrors).map { _ =>
              FileError_Type(
                Code = "001",
                Details = Some(ErrorDetail_Type("detail"))
              )
            },
            RecordError = (0 until numberOfRowErrors).map { _ =>
              RecordError_Type(
                Code = "002",
                Details = Some(ErrorDetail_Type("detail 2")),
                DocRefIDInError = Seq("1", "2")
              )
            }
          ),
          ValidationResult = ValidationResult_Type(
            Status = generated.Rejected
          )
        )
      )
    )
}
