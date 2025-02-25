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

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.dprs.stubs.config.Service
import uk.gov.hmrc.dprs.stubs.models.{MetadataValue, SdesFile}
import uk.gov.hmrc.dprs.stubs.repositories.ResultFileRepository
import uk.gov.hmrc.objectstore.client.Path
import uk.gov.hmrc.objectstore.client.play.Implicits._
import uk.gov.hmrc.objectstore.client.play.PlayObjectStoreClient
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ResultFilesController @Inject()(
                                     cc: ControllerComponents,
                                     repository: ResultFileRepository,
                                     configuration: Configuration,
                                     objectStoreClient: PlayObjectStoreClient
                                   )(implicit ec: ExecutionContext) extends BackendController(cc) {

  private val stubsUrl = configuration.get[Service]("microservice.services.digital-platform-reporting-stubs")

  // Just ignoring information type as we don't need it at the moment
  def list(informationType: String): Action[AnyContent] = Action.async { implicit _ =>
    repository.list.map { files =>

      val sdesFiles = files.map { file =>
        SdesFile(
          fileName = file.fileName,
          fileSize = file.size,
          downloadUrl = new URL(s"$stubsUrl${routes.ResultFilesController.get(file.fileName).url}"),
          metadata = file.metadata.map { case (key, value) =>
            MetadataValue(key, value)
          }.toSeq
        )
      }

      Ok(Json.toJson(sdesFiles))
    }
  }

  def get(fileName: String): Action[AnyContent] = Action.async { implicit request =>
    objectStoreClient.getObject[Source[ByteString, NotUsed]](Path.Directory("results").file(fileName)).map {
      _.map { file =>
        Ok.chunked(file.content)
      }.getOrElse(NotFound)
    }
  }
}
