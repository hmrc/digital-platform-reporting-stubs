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
import play.api.libs.json.{JsSuccess, JsValue}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.dprs.stubs.actions.AuthActionFilter
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.ResourceHelper.resourceAsString

import javax.inject.{Inject, Singleton}

@Singleton()
class RegistrationController @Inject() (authFilter: AuthActionFilter, cc: ControllerComponents) extends BackendController(cc) with Logging {

  private val withIdResponsePath         = "/resources/register/withid/organisation"
  private val withId_200_ResponsePath    = s"$withIdResponsePath/200-response.json"
  private val withId_409_ResponsePath    = s"$withIdResponsePath/409-response.json"
  private val withId_404_ResponsePath    = s"$withIdResponsePath/404-response.json"
  private val withoutIdResponsePath      = "/resources/register/withoutid/organisation"
  private val withoutId_200_ResponsePath = s"$withoutIdResponsePath/200-response.json"

  def registerWithId(): Action[JsValue] = (Action(parse.json) andThen authFilter) { implicit request =>
    logger.info(s"RegisterWithId Request received: \n ${request.body} \n")

    (request.body \ "registerWithIDRequest" \ "requestDetail" \ "IDNumber").validate[String] match {
      case JsSuccess("0000004090409", _) => Conflict(resourceAsString(withId_409_ResponsePath))
      case JsSuccess("0000000000404", _) => NotFound(resourceAsString(withId_404_ResponsePath))
      case _                             => Ok(resourceAsString(withId_200_ResponsePath))
    }
  }

  def registerWithoutId(): Action[JsValue] = (Action(parse.json) andThen authFilter) { implicit request =>
    logger.info(s"RegisterWithoutId Request received: \n ${request.body} \n")

    (request.body \ "registerWithIDRequest" \ "requestDetail" \ "IDNumber").validate[String] match {
      case _ => Ok(resourceAsString(withoutId_200_ResponsePath))
    }
  }
}
