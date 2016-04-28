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

import java.util.concurrent.TimeUnit

import connectors.CalculatorConnector

import forms.OtherPropertiesForm._
import forms.AcquisitionValueForm._
import forms.CustomerTypeForm._
import forms.DisabledTrusteeForm._
import forms.AnnualExemptAmountForm._
import forms.DisposalDateForm._
import forms.DisposalValueForm._
import forms.OtherReliefsForm._
import forms.AllowableLossesForm._
import forms.EntrepreneursReliefForm._
import forms.DisposalCostsForm._
import forms.ImprovementsForm._
import forms.PersonalAllowanceForm._
import forms.AcquisitionCostsForm._
import forms.CurrentIncomeForm._

import models._
import play.api.mvc.Action
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{Await, Future}
import views.html._

import scala.concurrent.duration.Duration

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

  val submitCustomerType = Action { implicit request =>
    customerTypeForm.bindFromRequest.fold(
      errors => BadRequest(calculation.customerType(errors)),
      success => {
        calcConnector.saveFormData("customerType", success)
        success.customerType match {
          case "individual" => Redirect(routes.CalculationController.currentIncome())
          case "trustee" => Redirect(routes.CalculationController.disabledTrustee())
          case "personalRep" => Redirect(routes.CalculationController.otherProperties())
        }
      }
    )
  }

  //################### Disabled Trustee methods #######################
  val disabledTrustee = Action.async { implicit request =>
    calcConnector.fetchAndGetFormData[DisabledTrusteeModel]("isVulnerable").map {
      case Some(data) => Ok(calculation.disabledTrustee(disabledTrusteeForm.fill(data)))
      case None => Ok(calculation.disabledTrustee(disabledTrusteeForm))
    }
  }

  val submitDisabledTrustee = Action { implicit request =>
    disabledTrusteeForm.bindFromRequest.fold(
      errors => BadRequest(calculation.disabledTrustee(errors)),
      success => {
        calcConnector.saveFormData("isVulnerable",success)
        Redirect(routes.CalculationController.otherProperties())
      }
    )
  }

  //################### Current Income methods #######################
  val currentIncome = Action.async { implicit request =>
    calcConnector.fetchAndGetFormData[CurrentIncomeModel]("currentIncome").map {
      case Some(data) => Ok(calculation.currentIncome(currentIncomeForm.fill(data)))
      case None => Ok(calculation.currentIncome(currentIncomeForm))
    }
  }

  val submitCurrentIncome = Action { implicit request =>
   currentIncomeForm.bindFromRequest.fold(
     errors => BadRequest(calculation.currentIncome(errors)),
     success => {
       calcConnector.saveFormData("currentIncome", success)
       Redirect(routes.CalculationController.personalAllowance())
     }
   )
  }

  //################### Personal Allowance methods #######################
  val personalAllowance = Action.async { implicit request =>
    calcConnector.fetchAndGetFormData[PersonalAllowanceModel]("personalAllowance").map {
      case Some(data) => Ok(calculation.personalAllowance(personalAllowanceForm.fill(data)))
      case None => Ok(calculation.personalAllowance(personalAllowanceForm))
    }
  }

  val submitPersonalAllowance = Action { implicit request =>
    personalAllowanceForm.bindFromRequest.fold(
      errors => BadRequest(calculation.personalAllowance(errors)),
      success => {
        calcConnector.saveFormData("personalAllowance", success)
        Redirect(routes.CalculationController.otherProperties())
      }
    )
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

  val submitAcquisitionValue = Action { implicit request =>
    acquisitionValueForm.bindFromRequest.fold(
      errors => BadRequest(calculation.acquisitionValue(errors)),
      success => {
        calcConnector.saveFormData("acquisitionValue", success)
        Redirect(routes.CalculationController.improvements())
      }
    )
  }

  //################### Improvements methods #######################
  val improvements = Action.async { implicit request =>
    calcConnector.fetchAndGetFormData[ImprovementsModel]("improvements").map {
      case Some(data) => Ok(calculation.improvements(improvementsForm.fill(data)))
      case None => Ok(calculation.improvements(improvementsForm))
    }
  }

  val submitImprovements = Action { implicit request =>
    improvementsForm.bindFromRequest.fold(
      errors => BadRequest(calculation.improvements(errors)),
      success => {
        calcConnector.saveFormData("improvements", success)
        Redirect(routes.CalculationController.disposalDate())
      }
    )
  }

  //################### Disposal Date methods #######################
  val disposalDate = Action.async { implicit request =>
    calcConnector.fetchAndGetFormData[DisposalDateModel]("disposalDate").map {
      case Some(data) => Ok(calculation.disposalDate(disposalDateForm.fill(data)))
      case None => Ok(calculation.disposalDate(disposalDateForm))
    }
  }

  val submitDisposalDate = Action { implicit request =>
    disposalDateForm.bindFromRequest.fold(
      errors => BadRequest(calculation.disposalDate(errors)),
      success => {
        calcConnector.saveFormData("disposalDate", success)
        Redirect(routes.CalculationController.disposalValue())
      }
    )
  }

  //################### Disposal Value methods #######################
  val disposalValue = Action.async { implicit request =>
    calcConnector.fetchAndGetFormData[DisposalValueModel]("disposalValue").map {
      case Some(data) => Ok(calculation.disposalValue(disposalValueForm.fill(data)))
      case None => Ok(calculation.disposalValue(disposalValueForm))
    }
  }

  val submitDisposalValue = Action { implicit request =>
    disposalValueForm.bindFromRequest.fold(
      errors => BadRequest(calculation.disposalValue(errors)),
      success => {
        calcConnector.saveFormData("disposalValue", success)
        Redirect(routes.CalculationController.acquisitionCosts())
      }
    )
  }

  //################### Acquisition Costs methods #######################
  val acquisitionCosts = Action.async { implicit request =>
    calcConnector.fetchAndGetFormData[AcquisitionCostsModel]("acquisitionCosts").map {
      case Some(data) => Ok(calculation.acquisitionCosts(acquisitionCostsForm.fill(data)))
      case None => Ok(calculation.acquisitionCosts(acquisitionCostsForm))
    }
  }

  val submitAcquisitionCosts = Action { implicit request =>
    acquisitionCostsForm.bindFromRequest.fold(
      errors => BadRequest(calculation.acquisitionCosts(errors)),
      success => {
        calcConnector.saveFormData("acquisitionCosts", success)
        Redirect(routes.CalculationController.disposalCosts())
      }
    )
  }

  //################### Disposal Costs methods #######################
  val disposalCosts = Action.async { implicit request =>
    calcConnector.fetchAndGetFormData[DisposalCostsModel]("disposalCosts").map {
      case Some(data) => Ok(calculation.disposalCosts(disposalCostsForm.fill(data)))
      case None => Ok(calculation.disposalCosts(disposalCostsForm))
    }
  }

  val submitDisposalCosts = Action { implicit request =>
    disposalCostsForm.bindFromRequest.fold(
      errors => BadRequest(calculation.disposalCosts(errors)),
      success => {
        calcConnector.saveFormData("disposalCosts", success)
        Redirect(routes.CalculationController.entrepreneursRelief())
      }
    )
  }

  //################### Entrepreneurs Relief methods #######################
  val entrepreneursRelief = Action.async { implicit request =>
    calcConnector.fetchAndGetFormData[EntrepreneursReliefModel]("entrepreneursRelief").map {
      case Some(data) => Ok(calculation.entrepreneursRelief(entrepreneursReliefForm.fill(data)))
      case None => Ok(calculation.entrepreneursRelief(entrepreneursReliefForm))
    }
  }

  val submitEntrepreneursRelief = Action { implicit request =>
    entrepreneursReliefForm.bindFromRequest.fold(
      errors => BadRequest(calculation.entrepreneursRelief(errors)),
      success => {
        calcConnector.saveFormData("entrepreneursRelief", success)
        Redirect(routes.CalculationController.allowableLosses())
      }
    )
  }

  //################### Allowable Losses methods #######################
  val allowableLosses = Action.async { implicit request =>
    calcConnector.fetchAndGetFormData[AllowableLossesModel]("allowableLosses").map {
      case Some(data) => Ok(calculation.allowableLosses(allowableLossesForm.fill(data)))
      case None => Ok(calculation.allowableLosses(allowableLossesForm))
    }
  }

  val submitAllowableLosses = Action { implicit request =>
    allowableLossesForm.bindFromRequest.fold(
      errors => BadRequest(calculation.allowableLosses(errors)),
      success => {
        calcConnector.saveFormData("allowableLosses", success)
        Redirect(routes.CalculationController.otherReliefs())
      }
    )
  }
  //################### Calculation Election methods #######################
  val calculationElection = Action.async { implicit request =>
    Future.successful(Ok(calculation.calculationElection()))
  }

  //################### Other Reliefs methods #######################
  val otherReliefs = Action.async { implicit request =>
    val construct = calcConnector.createSummary(hc)
    calcConnector.calculate(construct).map {
        case Some(dataResult) => {
          Await.result(calcConnector.fetchAndGetFormData[OtherReliefsModel]("otherReliefs").map {
            case Some(data) => Ok(calculation.otherReliefs(otherReliefsForm.fill(data), dataResult))
            case None => Ok(calculation.otherReliefs(otherReliefsForm, dataResult))
          }, Duration("5s"))
        }
        case None => {
          Await.result(calcConnector.fetchAndGetFormData[OtherReliefsModel]("otherReliefs").map {
            case Some(data) => Ok(calculation.otherReliefs(otherReliefsForm.fill(data), CalculationResultModel(0.0, 0.0, 0.0, 0, None, None)))
            case None => Ok(calculation.otherReliefs(otherReliefsForm, CalculationResultModel(0.0, 0.0, 0.0, 0, None, None)))
          }, Duration("5s"))
        }
      }
  }

  val submitOtherReliefs = Action { implicit request =>
    otherReliefsForm.bindFromRequest.fold(
      errors => BadRequest(calculation.otherReliefs(errors, CalculationResultModel(0.0, 0.0, 0.0, 0, None, None))),
      success => {
        calcConnector.saveFormData("otherReliefs", success)
        Redirect(routes.CalculationController.summary())
      }
    )
  }

  //################### Time Apportioned Other Reliefs methods #######################
  val otherReliefsTA = Action.async { implicit request =>
    Future.successful(Ok(calculation.otherReliefsTA()))
  }

  //################### Summary Methods ##########################
  def summary = Action.async { implicit request =>
    val construct = calcConnector.createSummary(hc)
    calcConnector.calculate(construct).map {
      case Some(data) => Ok(calculation.summary(construct, data))
      case None => BadRequest(calculation.summary(construct, CalculationResultModel(0.0, 0.0, 0.0, 0, None, None)))
    }
  }
}
