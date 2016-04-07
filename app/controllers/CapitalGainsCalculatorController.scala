/*
 * Copyright 2016 HM Revenue & Customs
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

package controllers

import play.api.mvc.Action
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future
import views.html._

object CapitalGainsCalculatorController extends CapitalGainsCalculatorController

trait CapitalGainsCalculatorController extends FrontendController {

  val customerType = Action.async { implicit request =>
    Future.successful(Ok(cgts.customerType()))
  }

  val disabledTrustee = Action.async { implicit request =>
    Future.successful(Ok(cgts.disabledTrustee()))
  }

  val currentIncome = TODO
  val personalAllowance = TODO

  val otherProperties = Action.async { implicit request =>
    Future.successful(Ok(cgts.otherProperties()))
  }

  val annualExemptAmount = Action.async { implicit request =>
    Future.successful(Ok(cgts.annualExemptAmount()))
  }

  val acquisitionValue = TODO

  val improvements = Action.async { implicit request =>
    Future.successful(Ok(cgts.improvements()))
  }
  
  val disposalDate = Action.async { implicit request =>
    Future.successful(Ok(cgts.disposalDate()))
  }

  val disposalValue = Action.async { implicit request =>
    Future.successful(Ok(cgts.disposalValue()))
  }

  val acquisitionCosts = TODO
  val disposalCosts = TODO
  val entrepreneursRelief = TODO
  val allowableLosses = TODO
  val otherReliefs = TODO

}