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
import play.api.libs.json.JsValue
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.dprs.stubs.actions.AuthActionFilter
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.ResourceHelper.resourceAsString

import javax.inject.{Inject, Singleton}

@Singleton()
class SubscribeController @Inject() (authFilter: AuthActionFilter, cc: ControllerComponents) extends BackendController(cc) with Logging {

  private val subscriptionResponsePath          = "/resources/subscription"
  private val create_200_ResponsePath           = s"$subscriptionResponsePath/create/200-response.json"
  private val update_200_ResponsePath           = s"$subscriptionResponsePath/update/200-response.json"
  private val view_200_OrganisationResponsePath = s"$subscriptionResponsePath/view/200-organisation-response.json"
  private val view_200_IndividualResponsePath   = s"$subscriptionResponsePath/view/200-individual-response.json"
  private val view_404_ResponsePath             = s"$subscriptionResponsePath/view/404-response.json"

  def create(): Action[JsValue] = (Action(parse.json) andThen authFilter) { implicit request =>
    logger.info(s"Create User Subscription Request received: \n ${request.body} \n")

    (request.body \ "registerWithIDRequest" \ "requestDetail" \ "IDNumber").validate[String] match {
      case _ => Created(resourceAsString(create_200_ResponsePath))
    }
  }

  def update(): Action[JsValue] = (Action(parse.json) andThen authFilter) { implicit request =>
    logger.info(s"Update User Subscription Request received: \n ${request.body} \n")

    (request.body \ "registerWithIDRequest" \ "requestDetail" \ "IDNumber").validate[String] match {
      case _ => Ok(resourceAsString(update_200_ResponsePath))
    }
  }

  def view(idValue: String): Action[JsValue] = (Action(parse.json) andThen authFilter) { implicit request =>
    logger.info(s"View User Subscription Request received: \n ${request.body} \n")

    idValue match {
      case _ if idValue.contains("404") => InternalServerError(resourceAsString(view_404_ResponsePath))
      case _ if idValue.startsWith("I") => Ok(resourceAsString(view_200_IndividualResponsePath))
      case _ if idValue.startsWith("O") => Ok(resourceAsString(view_200_OrganisationResponsePath))
      case _                            => BadRequest("Unexpected idValue.")
    }
  }
}
