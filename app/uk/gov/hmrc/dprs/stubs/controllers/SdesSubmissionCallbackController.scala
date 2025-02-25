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

import play.api.libs.json.Json
import play.api.libs.ws.writeableOf_JsValue
import play.api.mvc.{Action, ControllerComponents}
import play.api.{Configuration, Logging}
import uk.gov.hmrc.dprs.stubs.config.Service
import uk.gov.hmrc.dprs.stubs.models.sdes.NotificationCallback
import uk.gov.hmrc.dprs.stubs.services.SubmissionResultService
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SdesSubmissionCallbackController @Inject()(
                                                  cc: ControllerComponents,
                                                  httpClient: HttpClientV2,
                                                  configuration: Configuration,
                                                  submissionResultService: SubmissionResultService
                                                )(implicit ec: ExecutionContext) extends BackendController(cc) with Logging {

  private val digitalPlatformReporting: Service = configuration.get[Service]("microservice.services.digital-platform-reporting")

  def callback(): Action[NotificationCallback] = Action.async(parse.json[NotificationCallback]) { implicit request =>
    forwardNotification(request.body).map { _ =>
      submissionResultService.scheduleSuccess(request.body.correlationID)
      Ok
    }
  }

  private def forwardNotification(notification: NotificationCallback)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    httpClient
      .post(url"$digitalPlatformReporting/sdes/submission/callback")
      .withBody(Json.toJson(notification))
      .execute
}
