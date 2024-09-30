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

import generated.BREResponse_Type
import org.apache.pekko.Done
import play.api.Configuration
import play.api.http.Status.NO_CONTENT
import uk.gov.hmrc.dprs.stubs.config.Service
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionResultService @Inject() (
                                          configuration: Configuration,
                                          httpClient: HttpClientV2
                                        )(implicit ec: ExecutionContext) {

  private val digitalPlatformReporting: Service = configuration.get[Service]("microservice.services.digital-platform-reporting")
  private val callbackAuthToken: String = configuration.get[String]("result-callback-auth-token")

  def returnResult(submissionId: String, result: BREResponse_Type): Future[Done] = {
    httpClient.post(url"$digitalPlatformReporting/cadx/submission/callback")(HeaderCarrier())
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
}
