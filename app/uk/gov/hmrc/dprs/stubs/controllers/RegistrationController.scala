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
import utils.ResourceHelper

import javax.inject.{Inject, Singleton}

@Singleton()
class RegistrationController @Inject() (resourceHelper: ResourceHelper, authFilter: AuthActionFilter, cc: ControllerComponents)
    extends BackendController(cc)
    with Logging {

  private val organisationWithIdResponsePath      = "/resources/register/withid/organisation"
  private val organisationWithId_200_ResponsePath = s"$organisationWithIdResponsePath/200-response.json"
  private val organisationWithId_409_ResponsePath = s"$organisationWithIdResponsePath/409-response.json"
  private val organisationWithId_404_ResponsePath = s"$organisationWithIdResponsePath/404-response.json"
  private val individualWithIdResponsePath        = "/resources/register/withid/individual"
  private val individualWithId_200_ResponsePath   = s"$individualWithIdResponsePath/200-response.json"

  private val organisationWithoutIdResponsePath      = "/resources/register/withoutid/organisation"
  private val organisationWithoutId_200_ResponsePath = s"$organisationWithoutIdResponsePath/200-response.json"
  private val individualWithId_409_ResponsePath      = s"$individualWithIdResponsePath/409-response.json"
  private val individualWithId_404_ResponsePath      = s"$individualWithIdResponsePath/404-response.json"

  def registerWithId(): Action[JsValue] = (Action(parse.json) andThen authFilter) { implicit request =>
    logger.info(s"RegisterWithId Request received: \n ${request.body} \n")

    (request.body \ "registerWithIDRequest" \ "requestDetail" \ "IDNumber").validate[String] match {
      case JsSuccess("0000004090409", _)          => Conflict(resourceHelper.resourceAsString(organisationWithId_409_ResponsePath))
      case JsSuccess("0000000000404", _)          => NotFound(resourceHelper.resourceAsString(organisationWithId_404_ResponsePath))
      case JsSuccess("AA000409C", _)              => Conflict(resourceHelper.resourceAsString(individualWithId_409_ResponsePath))
      case JsSuccess("AA000404C", _)              => NotFound(resourceHelper.resourceAsString(individualWithId_404_ResponsePath))
      case _ if isIndividualRequest(request.body) => Ok(resourceHelper.resourceAsString(individualWithId_200_ResponsePath))
      case _                                      => Ok(resourceHelper.resourceAsString(organisationWithId_200_ResponsePath))
    }
  }

  def registerWithoutId(): Action[JsValue] = (Action(parse.json) andThen authFilter) { implicit request =>
    logger.info(s"RegisterWithoutId Request received: \n ${request.body} \n")

    (request.body \ "registerWithIDRequest" \ "requestDetail" \ "IDNumber").validate[String] match {
      case _ => Ok(resourceHelper.resourceAsString(organisationWithoutId_200_ResponsePath))
    }
  }

  private def isIndividualRequest(jsValue: JsValue): Boolean =
    (jsValue \ "registerWithIDRequest" \ "requestDetail" \ "individual").asOpt[String].isDefined
}
