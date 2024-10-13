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
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.dprs.stubs.actions.AuthActionFilter
import uk.gov.hmrc.dprs.stubs.models.subscription.{CreateSubscriptionRequest, CreateSubscriptionResponse, Subscription, SubscriptionInfo, UpdateSubscriptionRequest}
import uk.gov.hmrc.dprs.stubs.repositories.{SubscriptionIdRepository, SubscriptionRepository}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.ResourceHelper

import java.time.Clock
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class SubscribeController @Inject()(resourceHelper: ResourceHelper,
                                    authFilter: AuthActionFilter,
                                    cc: ControllerComponents,
                                    subscriptionRepository: SubscriptionRepository,
                                    subscriptionIdRepository: SubscriptionIdRepository,
                                    clock: Clock)(implicit ec: ExecutionContext)
  extends BackendController(cc) {

  private val subscriptionResponsePath          = "/resources/subscription"
  private val create_422_007_ResponsePath       = s"$subscriptionResponsePath/create/422-007-duplicate-submission.json"

  def create(): Action[CreateSubscriptionRequest] = (Action(parse.json[CreateSubscriptionRequest]) andThen authFilter).async { implicit request =>
    request.body.safeId match {
      case "XE00000422007" => Future.successful(UnprocessableEntity(resourceHelper.resourceAsString(create_422_007_ResponsePath)))
      case _ =>
        for {
          subscriptionId <- subscriptionIdRepository.nextSubscriptionId
          subscription   = Subscription(request.body, subscriptionId.id.toString, clock)
          _              <- subscriptionRepository.create(subscription)
        } yield Created(Json.toJson(CreateSubscriptionResponse(subscriptionId.id.toString, clock.instant)))
    }
  }

  def update(): Action[UpdateSubscriptionRequest] = (Action(parse.json[UpdateSubscriptionRequest]) andThen authFilter).async { implicit request =>
    val subscription = Subscription(request.body, clock)
    subscriptionRepository.update(subscription).map(_ => Accepted)
  }

  def view(idValue: String): Action[AnyContent] = (Action andThen authFilter).async { _ =>
    subscriptionRepository.get(idValue).map {
      case Some(subscription) => Ok(Json.toJson(SubscriptionInfo(subscription)))
      case None               => NotFound
    }
  }
}
