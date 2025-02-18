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

package uk.gov.hmrc.dprs.stubs.models.upscan

import play.api.libs.json._

import java.net.URL
import java.time.Instant
import uk.gov.hmrc.dprs.stubs.models.urlFormat

sealed trait UpscanCallbackRequest {
  def reference: String
}

object UpscanCallbackRequest {

  case class Ready(
    reference:     String,
    downloadUrl:   URL,
    uploadDetails: UploadDetails
  ) extends UpscanCallbackRequest

  case class Failed(
    reference:      String,
    failureDetails: ErrorDetails
  ) extends UpscanCallbackRequest

  case class UploadDetails(
    uploadTimestamp: Instant,
    checksum:        String,
    fileMimeType:    String,
    fileName:        String,
    size:            Long
  )

  case class ErrorDetails(
    failureReason: UpscanFailureReason,
    message:       String
  )

  sealed trait UpscanFailureReason {
    def name: String = this.toString.toUpperCase
  }

  object UpscanFailureReason {
    case object Quarantine extends UpscanFailureReason
    case object Rejected extends UpscanFailureReason
    case object Unknown extends UpscanFailureReason
    case object Duplicate extends UpscanFailureReason

    val values: Seq[UpscanFailureReason] = Seq(Quarantine, Rejected, Unknown, Duplicate)

    implicit val format: Format[UpscanFailureReason] = new Format[UpscanFailureReason] {
      override def reads(json: JsValue): JsResult[UpscanFailureReason] = json match {
        case JsString("QUARANTINE") => JsSuccess(Quarantine)
        case JsString("REJECTED")   => JsSuccess(Rejected)
        case JsString("UNKNOWN")    => JsSuccess(Unknown)
        case JsString("DUPLICATE")  => JsSuccess(Duplicate)
        case _                      => JsError("Unknown failure reason")
      }

      override def writes(o: UpscanFailureReason): JsValue = JsString(o.name)
    }
  }

  implicit val uploadDetailsFormat: Format[UploadDetails] = Json.format[UploadDetails]
  implicit val errorDetailsFormat:  Format[ErrorDetails]  = Json.format[ErrorDetails]
  implicit val readyFormat:         Format[Ready]         = Json.format[Ready]
  implicit val failedFormat:        Format[Failed]        = Json.format[Failed]

  implicit val upscanCallbackRequestFormat: Format[UpscanCallbackRequest] = new Format[UpscanCallbackRequest] {
    private val discriminator = "fileStatus"

    override def reads(json: JsValue): JsResult[UpscanCallbackRequest] =
      (json \ discriminator).validate[String].flatMap { fileStatus =>
        fileStatus.toUpperCase match {
          case "READY"  => Json.fromJson[Ready](json)(readyFormat).map(_.asInstanceOf[UpscanCallbackRequest])
          case "FAILED" => Json.fromJson[Failed](json)(failedFormat).map(_.asInstanceOf[UpscanCallbackRequest])
          case _        => JsError(s"Unknown fileStatus: $fileStatus")
        }
      }

    override def writes(o: UpscanCallbackRequest): JsValue = {
      val baseJson = o match {
        case ready:  Ready  => Json.toJson(ready)(readyFormat)
        case failed: Failed => Json.toJson(failed)(failedFormat)
      }
      val status = o match {
        case _: Ready  => "READY"
        case _: Failed => "FAILED"
      }
      baseJson.as[JsObject] + (discriminator -> JsString(status))
    }
  }
}
