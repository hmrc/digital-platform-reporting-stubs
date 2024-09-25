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

import uk.gov.hmrc.dprs.stubs.models.operator.{Notification, NotificationDetails, PlatformOperator}
import uk.gov.hmrc.dprs.stubs.models.operator.requests.{CreatePlatformOperatorRequest, UpdatePlatformOperatorRequest}
import uk.gov.hmrc.dprs.stubs.repositories.{OperatorIdRepository, PlatformOperatorRepository}
import uk.gov.hmrc.dprs.stubs.services.PlatformOperatorService.PlatformOperatorNotFoundException

import java.time.Clock
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PlatformOperatorService @Inject()(operatorIdRepository: OperatorIdRepository,
                                        repository: PlatformOperatorRepository,
                                        clock: Clock)
                                       (implicit ec: ExecutionContext) {

  def fromCreateRequest(request: CreatePlatformOperatorRequest): Future[PlatformOperator] =
    operatorIdRepository.nextOperatorId.map { operatorId =>
      PlatformOperator(
        subscriptionId          = request.subscriptionId,
        operatorId              = operatorId.id.toString,
        operatorName            = request.operatorName,
        tinDetails              = request.tinDetails,
        tradingName             = request.tradingName,
        primaryContactDetails   = request.primaryContactDetails,
        secondaryContactDetails = request.secondaryContactDetails,
        addressDetails          = request.addressDetails,
        notifications           = Nil,
        created                 = clock.instant,
        updated                 = clock.instant
      )
    }

  def fromUpdateRequest(request: UpdatePlatformOperatorRequest): Future[PlatformOperator] =
    repository.get(request.subscriptionId, request.operatorId)
      .flatMap(_.map { existingOperator =>
          request.notification.map { notification =>
            Future.successful(addNotification(existingOperator, notification))
          }.getOrElse {
            Future.successful(updatePlatformOperator(existingOperator, request))
          }
        }.getOrElse {
          Future.failed(PlatformOperatorNotFoundException(request.subscriptionId, request.operatorId))
        }
      )

  private def addNotification(existingOperator: PlatformOperator, notification: Notification): PlatformOperator = {
    val newNotification = NotificationDetails(
      notificationType = notification.notificationType,
      isActiveSeller   = notification.isActiveSeller,
      isDueDiligence   = notification.isDueDiligence,
      firstPeriod      = notification.firstPeriod,
      receivedDateTime = clock.instant
    )

    existingOperator.copy(
      notifications = existingOperator.notifications :+ newNotification,
      updated       = clock.instant
    )
  }

  private def updatePlatformOperator(existingOperator: PlatformOperator, request: UpdatePlatformOperatorRequest): PlatformOperator =
    existingOperator.copy (
      operatorName            = request.operatorName,
      tinDetails              = request.tinDetails,
      tradingName             = request.tradingName,
      primaryContactDetails   = request.primaryContactDetails,
      secondaryContactDetails = request.secondaryContactDetails,
      addressDetails          = request.addressDetails,
      updated                 = clock.instant
    )
}

object PlatformOperatorService {

  final case class PlatformOperatorNotFoundException(subscriptionId: String, operatorId: String) extends Throwable {

    override def getMessage: String = s"Could not find platform operator. Subscription id: $subscriptionId, Operator id: $operatorId"
  }
}
