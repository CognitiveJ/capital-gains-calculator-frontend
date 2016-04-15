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

import connectors.CalculatorConnector
import forms.CustomerTypeForm._
import forms.AnnualExemptAmountForm._
import forms.DisposalValueForm._
import models.{DisposalValueModel, AnnualExemptAmountModel, CustomerTypeModel}
import play.api.mvc.Action
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future
import views.html._

object CalculationController extends CalculationController {
  val calcConnector = CalculatorConnector
}

trait CalculationController extends FrontendController {

  val calcConnector: CalculatorConnector

  //################### Customer Type methods #######################
  val customerType = Action.async { implicit request =>
    calcConnector.fetchAndGetFormData[CustomerTypeModel]("customerType").map {
      case Some(data) => Ok(calculation.customerType(customerTypeForm.fill(data)))
      case None => Ok(calculation.customerType(customerTypeForm))
    }
  }

  //################### Disabled Trustee methods #######################
  val disabledTrustee = Action.async { implicit request =>
    Future.successful(Ok(calculation.disabledTrustee()))
  }

  //################### Current Income methods #######################
  val currentIncome = TODO

  //################### Personal Allowance methods #######################
  val personalAllowance = Action.async { implicit request =>
    Future.successful(Ok(calculation.personalAllowance()))
  }

  //################### Other Properties methods #######################
  val otherProperties = Action.async { implicit request =>
    Future.successful(Ok(calculation.otherProperties()))
  }

  //################### Annual Exempt Amount methods #######################
  val annualExemptAmount = Action.async { implicit request =>
    calcConnector.fetchAndGetFormData[AnnualExemptAmountModel]("annualExemptAmount").map {
      case Some(data) => Ok(calculation.annualExemptAmount(annualExemptAmountForm.fill(data)))
      case None => Ok(calculation.annualExemptAmount(annualExemptAmountForm))
    }
  }

  //################### Acquisition Value methods #######################
  val acquisitionValue = Action.async { implicit request =>
    Future.successful(Ok(calculation.acquisitionValue()))
  }

  //################### Improvements methods #######################
  val improvements = Action.async { implicit request =>
    Future.successful(Ok(calculation.improvements()))
  }

  //################### Disposal Date methods #######################
  val disposalDate = Action.async { implicit request =>
    Future.successful(Ok(calculation.disposalDate()))
  }

  //################### Disposal Value methods #######################
  val disposalValue = Action.async { implicit request =>
    calcConnector.fetchAndGetFormData[DisposalValueModel]("disposalValue").map {
      case Some(data) => Ok(calculation.disposalValue(disposalValueForm.fill(data)))
      case None => Ok(calculation.disposalValue(disposalValueForm))
    }
  }

  //################### Acquisition Costs methods #######################
  val acquisitionCosts = Action.async { implicit request =>
    Future.successful(Ok(calculation.acquisitionCosts()))
  }

  //################### Disposal Costs methods #######################
  val disposalCosts = Action.async { implicit request =>
    Future.successful(Ok(calculation.disposalCosts()))
  }

  //################### Entrepreneurs Relief methods #######################
  val entrepreneursRelief = Action.async { implicit request =>
    Future.successful(Ok(calculation.entrepreneursRelief()))
  }

  //################### Allowable Losses methods #######################
  val allowableLosses = Action.async { implicit request =>
    Future.successful(Ok(calculation.allowableLosses()))
  }

  //################### Other Reliefs methods #######################
  val otherReliefs = Action.async { implicit request =>
    Future.successful(Ok(calculation.otherReliefs()))
  }

  //################### Summary Methods ##########################
  val summary = Action.async { implicit request =>
    Future.successful(Ok(calculation.summary()))
  }

}
