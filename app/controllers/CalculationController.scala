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
import forms.OtherPropertiesForm._
import forms.AcquisitionValueForm._
import forms.CustomerTypeForm._
import forms.DisabledTrusteeForm._
import forms.AnnualExemptAmountForm._
import forms.DisposalDateForm._
import forms.DisposalValueForm._
import forms.AllowableLossesForm._
import forms.EntrepreneursReliefForm._
import forms.DisposalCostsForm._
import models._
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
    calcConnector.fetchAndGetFormData[DisabledTrusteeModel]("isVulnerable").map {
      case Some(data) => Ok(calculation.disabledTrustee(disabledTrusteeForm.fill(data)))
      case None => Ok(calculation.disabledTrustee(disabledTrusteeForm))
    }
  }

  //################### Current Income methods #######################
  val currentIncome = TODO

  //################### Personal Allowance methods #######################
  val personalAllowance = Action.async { implicit request =>
    Future.successful(Ok(calculation.personalAllowance()))
  }

  //################### Other Properties methods #######################
  val otherProperties = Action.async { implicit request =>

    calcConnector.fetchAndGetFormData[OtherPropertiesModel]("otherProperties").map {
      case Some(data) => Ok(calculation.otherProperties(otherPropertiesForm.fill(data)))
      case None => Ok(calculation.otherProperties(otherPropertiesForm))
    }
  }

  val submitOtherProperties = Action { implicit request =>
    otherPropertiesForm.bindFromRequest.fold(
      errors => BadRequest(calculation.otherProperties(errors)),
      success => {
        calcConnector.saveFormData("otherProperties", success)
        success.otherProperties match {
          case "Yes" => Redirect(routes.CalculationController.annualExemptAmount())
          case "No" => Redirect(routes.CalculationController.acquisitionValue())
       }
      }
    )
  }

  //################### Annual Exempt Amount methods #######################
  val annualExemptAmount = Action.async { implicit request =>
    calcConnector.fetchAndGetFormData[AnnualExemptAmountModel]("annualExemptAmount").map {
      case Some(data) => Ok(calculation.annualExemptAmount(annualExemptAmountForm.fill(data)))
      case None => Ok(calculation.annualExemptAmount(annualExemptAmountForm))
    }
  }

  val submitAnnualExemptAmount =  Action { implicit request =>
    annualExemptAmountForm.bindFromRequest.fold(
      errors => BadRequest(calculation.annualExemptAmount(errors)),
      success => {
        calcConnector.saveFormData("annualExemptAmount", success)
        Redirect(routes.CalculationController.acquisitionValue())
      }
    )
  }

  //################### Acquisition Value methods #######################
  val acquisitionValue = Action.async { implicit request =>
    calcConnector.fetchAndGetFormData[AcquisitionValueModel]("acquisitionValue").map {
      case Some(data) => Ok(calculation.acquisitionValue(acquisitionValueForm.fill(data)))
      case None => Ok(calculation.acquisitionValue(acquisitionValueForm))
    }
  }

  //################### Improvements methods #######################
  val improvements = Action.async { implicit request =>
    Future.successful(Ok(calculation.improvements()))
  }

  //################### Disposal Date methods #######################
  val disposalDate = Action.async { implicit request =>
    calcConnector.fetchAndGetFormData[DisposalDateModel]("disposalDate").map {
      case Some(data) => Ok(calculation.disposalDate(disposalDateForm.fill(data)))
      case None => Ok(calculation.disposalDate(disposalDateForm))
    }
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
    calcConnector.fetchAndGetFormData[DisposalCostsModel]("disposalCosts").map {
      case Some(data) => Ok(calculation.disposalCosts(disposalCostsForm.fill(data)))
      case None => Ok(calculation.disposalCosts(disposalCostsForm))
    }
  }

  //################### Entrepreneurs Relief methods #######################
  val entrepreneursRelief = Action.async { implicit request =>
    calcConnector.fetchAndGetFormData[EntrepreneursReliefModel]("entrepreneursRelief").map {
      case Some(data) => Ok(calculation.entrepreneursRelief(entrepreneursReliefForm.fill(data)))
      case None => Ok(calculation.entrepreneursRelief(entrepreneursReliefForm))
    }
  }

  //################### Allowable Losses methods #######################
  val allowableLosses = Action.async { implicit request =>
    calcConnector.fetchAndGetFormData[AllowableLossesModel]("allowableLossesAmt").map {
      case Some(data) => Ok(calculation.allowableLosses(allowableLossesForm.fill(data)))
      case None => Ok(calculation.allowableLosses(allowableLossesForm))
    }
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
