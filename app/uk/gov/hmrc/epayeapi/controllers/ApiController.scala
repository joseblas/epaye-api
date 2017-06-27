/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.epayeapi.controllers

import play.api.libs.json.Json
import play.api.libs.streams.Accumulator
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.auth.core.Retrievals._
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.epayeapi.models.ApiError.InvalidEmpRef
import uk.gov.hmrc.epayeapi.models.Formats._
import uk.gov.hmrc.epayeapi.models.{ApiError, EmpRefsResponse}

import scala.concurrent.{ExecutionContext, Future}

trait ApiController extends BaseController with AuthorisedFunctions {
  val epayeEnrolment = Enrolment("IR-PAYE")
  def authConnector: AuthConnector
  implicit def ec: ExecutionContext

  def EnrolmentsAction(fn: Enrolments => RequestHeader => Future[Result]): Action[AnyContent] =
    Action.async { implicit request =>
      authorised(epayeEnrolment).retrieve(authorisedEnrolments) { enrolments =>
        fn(enrolments)(request)
      } recoverWith {
        case ex: InsufficientEnrolments =>
          insufficientEnrolments
        case ex: MissingBearerToken =>
          missingBearerToken
      }
    }

  def EmpRefsAction(fn: Set[EmpRef] => RequestHeader => Future[Result]): Action[AnyContent] =
    EnrolmentsAction { enrolments => request =>
      fn(enrolments.enrolments.flatMap(enrolmentToEmpRef))(request)
    }

  def EmpRefAction(urlEmpRef: EmpRef)(fn: RequestHeader => Future[Result]): Action[AnyContent] = {
    EmpRefsAction { empRefs => request =>
      empRefs.find(_ == urlEmpRef) match {
        case Some(empRef) => fn(request)
        case None => invalidEmpRef
      }
    }
  }

  def missingBearerToken: Future[Result] =
    Future.successful(Unauthorized(Json.toJson(ApiError.AuthorizationHeaderInvalid)))
  def insufficientEnrolments: Future[Result] =
    Future.successful(Unauthorized(Json.toJson(ApiError.InsufficientEnrolments)))
  def invalidEmpRef: Future[Result] =
    Future.successful(Unauthorized(Json.toJson(ApiError.InvalidEmpRef)))

  private def enrolmentToEmpRef(enrolment: Enrolment): Option[EmpRef] = {
    for {
      "IR-PAYE" <- Option(enrolment.key)
      tn <- enrolment.identifiers.find(_.key == "TaxOfficeNumber")
      tr <- enrolment.identifiers.find(_.key == "TaxOfficeReference")
      if enrolment.isActivated
    } yield EmpRef(tn.value, tr.value)
  }
}

object ApiController {
  implicit val empRefPathBinder: PathBindable[EmpRef] = new PathBindable[EmpRef] {
    def bind(key: String, value: String): Either[String, EmpRef] = {
      value.split("/") match {
        case Array(taxOfficeNumber, taxOfficeReference) => Right(EmpRef(taxOfficeNumber, taxOfficeReference))
        case other => Left("Could not URL part into EmpRef")
      }
    }

    def unbind(key: String, value: EmpRef): String = {
      s"${value.taxOfficeNumber}/${value.taxOfficeReference}"
    }
  }
}
