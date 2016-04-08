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

object CalculationController extends CalculationController

trait CalculationController extends FrontendController {

  //################### Customer Type methods #######################
  val customerType = Action.async { implicit request =>
    Future.successful(Ok(cgts.customerType()))
  }

  //################### Disabled Trustee methods #######################
  val disabledTrustee = Action.async { implicit request =>
    Future.successful(Ok(cgts.disabledTrustee()))
  }

  //################### Current Income methods #######################
  val currentIncome = TODO

  //################### Personal Allowance methods #######################
  val personalAllowance = Action.async { implicit request =>
    Future.successful(Ok(cgts.personalAllowance()))
  }

  //################### Other Properties methods #######################
  val otherProperties = Action.async { implicit request =>
    Future.successful(Ok(cgts.otherProperties()))
  }

  //################### Annual Exempt Amount methods #######################
  val annualExemptAmount = Action.async { implicit request =>
    Future.successful(Ok(cgts.annualExemptAmount()))
  }

  //################### Acquisition Value methods #######################
  val acquisitionValue = Action.async { implicit request =>
    Future.successful(Ok(cgts.acquisitionValue()))
  }

  //################### Improvements methods #######################
  val improvements = Action.async { implicit request =>
    Future.successful(Ok(cgts.improvements()))
  }

  //################### Disposal Date methods #######################
  val disposalDate = Action.async { implicit request =>
    Future.successful(Ok(cgts.disposalDate()))
  }

  //################### Disposal Value methods #######################
  val disposalValue = Action.async { implicit request =>
    Future.successful(Ok(cgts.disposalValue()))
  }

  //################### Acquisition Costs methods #######################
  val acquisitionCosts = Action.async { implicit request =>
    Future.successful(Ok(cgts.acquisitionCosts()))
  }

  //################### Disposal Costs methods #######################
  val disposalCosts = Action.async { implicit request =>
    Future.successful(Ok(cgts.disposalCosts()))
  }

  //################### Entrepreneurs Relief methods #######################
  val entrepreneursRelief = Action.async { implicit request =>
    Future.successful(Ok(cgts.entrepreneursRelief()))
  }

  //################### Allowable Losses methods #######################
  val allowableLosses = Action.async { implicit request =>
    Future.successful(Ok(cgts.allowableLosses()))
  }

  //################### Other Reliefs methods #######################
  val otherReliefs = TODO

}