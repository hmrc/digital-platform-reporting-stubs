package uk.gov.hmrc.dprs.stubs.controllers

import play.api.Logging
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.dprs.stubs.actions.AuthActionFilter
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.ResourceHelper

import javax.inject.Inject

class PlatformOperatorController @Inject() (resourceHelper: ResourceHelper,
                                            authFilter: AuthActionFilter,
                                            cc: ControllerComponents)
  extends BackendController(cc)
    with Logging {

  private val operatorResponsePath = "/resources/operator"
  private val create200ResponsePath = s"$operatorResponsePath/create/200-response.json"
  private val delete200ResponsePath = s"$operatorResponsePath/delete/200-response.json"
  private val update200ResponsePath = s"$operatorResponsePath/update/200-response.json"
  private val view200SparsePath = s"$operatorResponsePath/view/200-response-sparse.json"
  private val view200FullPath = s"$operatorResponsePath/view/200-response-full.json"
  private val view200InternationalPath = s"$operatorResponsePath/view/200-response-international.json"

  def create(): Action[JsValue] = (Action(parse.json) andThen authFilter) { implicit request =>
    logger.info(s"Create Platform Operator request received:\n${request.body}\n")

    (request.body \ "POManagement" \ "RequestCommon" \ "RequestType").validate[String] match {
      case "CREATE" => Ok(resourceHelper.resourceAsString(create200ResponsePath))
      case "UPDATE" => Ok(resourceHelper.resourceAsString(update200ResponsePath))
      case "DELETE" => Ok(resourceHelper.resourceAsString(delete200ResponsePath))
    }
  }

  def view(subscriptionId: String): Action[AnyContent] = (Action andThen authFilter) { implicit request =>
    logger.info(s"View Platform Operator details request received for subscriptionId: $subscriptionId")

    subscriptionId match {
      case id if id.startsWith("404") => NotFound
      case id if id.startsWith("I")   => Ok(resourceHelper.resourceAsString(view200InternationalPath))
      case id if id.startsWith("S")   => Ok(resourceHelper.resourceAsString(view200SparsePath))
      case _                          => Ok(resourceHelper.resourceAsString(view200FullPath))
    }
  }
}
