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

import java.text.SimpleDateFormat

import connectors.CalculatorConnector
import common.{Dates, KeystoreKeys}
import constructors.CalculationElectionConstructor
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
import forms.CalculationElectionForm._
import forms.AcquisitionDateForm._
import forms.RebasedValueForm._
import forms.RebasedCostsForm._
import forms.PrivateResidenceReliefForm._
import models._
import java.util.{Calendar, Date}
import play.api.mvc.{Result, AnyContent, Action}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.HeaderCarrier
import scala.concurrent.{Await, Future}
import views.html._
import common.DefaultRoutes._
import scala.concurrent.duration.Duration

object CalculationController extends CalculationController {
  val calcConnector = CalculatorConnector
  val calcElectionConstructor = CalculationElectionConstructor
}

trait CalculationController extends FrontendController {

  val calcConnector: CalculatorConnector
  val calcElectionConstructor: CalculationElectionConstructor

  //################### Customer Type methods #######################
  val customerType = Action.async { implicit request =>
    calcConnector.fetchAndGetFormData[CustomerTypeModel](KeystoreKeys.customerType).map {
      case Some(data) => Ok(calculation.customerType(customerTypeForm.fill(data)))
      case None => Ok(calculation.customerType(customerTypeForm))
    }
  }

  val submitCustomerType = Action.async { implicit request =>
    customerTypeForm.bindFromRequest.fold(
      errors => Future.successful(BadRequest(calculation.customerType(errors))),
      success => {
        calcConnector.saveFormData(KeystoreKeys.customerType, success)
        success.customerType match {
          case "individual" => Future.successful(Redirect(routes.CalculationController.currentIncome()))
          case "trustee" => Future.successful(Redirect(routes.CalculationController.disabledTrustee()))
          case "personalRep" => Future.successful(Redirect(routes.CalculationController.otherProperties()))
        }
      }
    )
  }

  //################### Disabled Trustee methods #######################
  val disabledTrustee = Action.async { implicit request =>
    calcConnector.fetchAndGetFormData[DisabledTrusteeModel](KeystoreKeys.disabledTrustee).map {
      case Some(data) => Ok(calculation.disabledTrustee(disabledTrusteeForm.fill(data)))
      case None => Ok(calculation.disabledTrustee(disabledTrusteeForm))
    }
  }

  val submitDisabledTrustee = Action.async { implicit request =>
    disabledTrusteeForm.bindFromRequest.fold(
      errors => Future.successful(BadRequest(calculation.disabledTrustee(errors))),
      success => {
        calcConnector.saveFormData(KeystoreKeys.disabledTrustee,success)
        Future.successful(Redirect(routes.CalculationController.otherProperties()))
      }
    )
  }

  //################### Current Income methods #######################
  val currentIncome = Action.async { implicit request =>
    calcConnector.fetchAndGetFormData[CurrentIncomeModel](KeystoreKeys.currentIncome).map {
      case Some(data) => Ok(calculation.currentIncome(currentIncomeForm.fill(data)))
      case None => Ok(calculation.currentIncome(currentIncomeForm))
    }
  }

  val submitCurrentIncome = Action.async { implicit request =>
   currentIncomeForm.bindFromRequest.fold(
     errors => Future.successful(BadRequest(calculation.currentIncome(errors))),
     success => {
       calcConnector.saveFormData(KeystoreKeys.currentIncome, success)
       Future.successful(Redirect(routes.CalculationController.personalAllowance()))
     }
   )
  }

  //################### Personal Allowance methods #######################
  val personalAllowance = Action.async { implicit request =>
    calcConnector.fetchAndGetFormData[PersonalAllowanceModel](KeystoreKeys.personalAllowance).map {
      case Some(data) => Ok(calculation.personalAllowance(personalAllowanceForm.fill(data)))
      case None => Ok(calculation.personalAllowance(personalAllowanceForm))
    }
  }

  val submitPersonalAllowance = Action.async { implicit request =>
    personalAllowanceForm.bindFromRequest.fold(
      errors => Future.successful(BadRequest(calculation.personalAllowance(errors))),
      success => {
        calcConnector.saveFormData(KeystoreKeys.personalAllowance, success)
        Future.successful(Redirect(routes.CalculationController.otherProperties()))
      }
    )
  }

  //################### Other Properties methods #######################
  def otherPropertiesBackUrl(implicit hc: HeaderCarrier): Future[String] = calcConnector.fetchAndGetFormData[CustomerTypeModel](KeystoreKeys.customerType).map {
    case Some(data) => data.customerType match {
      case "individual" => routes.CalculationController.personalAllowance().url
      case "trustee" => routes.CalculationController.disabledTrustee().url
      case _ => routes.CalculationController.customerType().url
    }
    case _ => missingDataRoute
  }

  val otherProperties = Action.async { implicit request =>

    def routeRequest(backUrl: String): Future[Result] = {
      calcConnector.fetchAndGetFormData[OtherPropertiesModel](KeystoreKeys.otherProperties).map {
        case Some(data) => Ok(calculation.otherProperties(otherPropertiesForm.fill(data), backUrl))
        case _ => Ok(calculation.otherProperties(otherPropertiesForm, backUrl))
      }
    }

    for {
      backUrl <- otherPropertiesBackUrl
      finalResult <- routeRequest(backUrl)
    } yield finalResult
  }

  val submitOtherProperties = Action.async { implicit request =>

    def routeRequest(backUrl: String): Future[Result] = {
      otherPropertiesForm.bindFromRequest.fold(
        errors =>
          Future.successful(BadRequest(calculation.otherProperties(errors, backUrl))),
        success => {
          calcConnector.saveFormData(KeystoreKeys.otherProperties, success)
          success.otherProperties match {
            case "Yes" =>
              success.otherPropertiesAmt match {
                case Some(data) if data.equals(BigDecimal(0)) => Future.successful(Redirect(routes.CalculationController.annualExemptAmount()))
                case _ => calcConnector.saveFormData("annualExemptAmount", AnnualExemptAmountModel(0))
                  Future.successful(Redirect(routes.CalculationController.acquisitionDate()))
              }
            case "No" => Future.successful(Redirect(routes.CalculationController.acquisitionDate()))
          }
        }
      )
    }
    for {
      backUrl <- otherPropertiesBackUrl
      finalResult <- routeRequest(backUrl)
    } yield finalResult
  }

  //################### Annual Exempt Amount methods #######################
  val annualExemptAmount = Action.async { implicit request =>
    calcConnector.fetchAndGetFormData[AnnualExemptAmountModel](KeystoreKeys.annualExemptAmount).map {
      case Some(data) => Ok(calculation.annualExemptAmount(annualExemptAmountForm(true).fill(data)))
      case None => Ok(calculation.annualExemptAmount(annualExemptAmountForm(true)))
    }
  }

  val submitAnnualExemptAmount =  Action.async { implicit request =>

    def customerType(implicit hc: HeaderCarrier): Future[String] = {
      calcConnector.fetchAndGetFormData[CustomerTypeModel](KeystoreKeys.customerType).map {
        customerTypeModel => customerTypeModel.get.customerType
      }
    }

    def trusteeAEA(customerTypeVal: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
      customerTypeVal match {
        case "trustee" =>
          calcConnector.fetchAndGetFormData[DisabledTrusteeModel](KeystoreKeys.disabledTrustee).map {
            disabledTrusteeModel => if (disabledTrusteeModel.get.isVulnerable == "No") false else true
          }
        case _ => Future.successful(true)
      }
    }

    def routeRequest(isDisabledTrustee: Boolean)(implicit hc: HeaderCarrier): Future[Result] = {
      annualExemptAmountForm(isDisabledTrustee).bindFromRequest.fold(
        errors => Future.successful(BadRequest(calculation.annualExemptAmount(errors))),
        success => {
          calcConnector.saveFormData(KeystoreKeys.annualExemptAmount, success)
          Future.successful(Redirect(routes.CalculationController.acquisitionDate()))
        }
      )
    }

    for {
      customerTypeVal <- customerType
      isDisabledTrustee <- trusteeAEA(customerTypeVal)
      finalResult <- routeRequest(isDisabledTrustee)
    } yield finalResult
  }

  //################### Acquisition Date methods #######################
  def acquisitionDateBackUrl(implicit hc: HeaderCarrier): Future[String] = {
    calcConnector.fetchAndGetFormData[OtherPropertiesModel](KeystoreKeys.otherProperties).map {
      case Some(data) if data.otherProperties == "Yes" =>
        if (data.otherPropertiesAmt.get == 0) {
          routes.CalculationController.annualExemptAmount().url
        } else {
          routes.CalculationController.otherProperties().url
        }
      case Some(data) if data.otherProperties == "No" => routes.CalculationController.otherProperties().url
      case _ => missingDataRoute
    }
  }


  val acquisitionDate = Action.async { implicit request =>

    def routeRequest(backUrl: String): Future[Result] = {
      calcConnector.fetchAndGetFormData[AcquisitionDateModel](KeystoreKeys.acquisitionDate).map {
        case Some(data) => Ok(calculation.acquisitionDate(acquisitionDateForm.fill(data), backUrl))
        case None => Ok(calculation.acquisitionDate(acquisitionDateForm, backUrl))
      }
    }

    for {
      backUrl <- acquisitionDateBackUrl
      finalResult <- routeRequest(backUrl)
    } yield finalResult
  }

  val submitAcquisitionDate = Action.async { implicit request =>

    def routeRequest(backUrl: String): Future[Result] = {
      acquisitionDateForm.bindFromRequest.fold(
        errors => Future.successful(BadRequest(calculation.acquisitionDate(errors, backUrl))),
        success => {
          calcConnector.saveFormData(KeystoreKeys.acquisitionDate, success)
          Future.successful(Redirect(routes.CalculationController.acquisitionValue()))
        }
      )
    }

    for {
      backUrl <- acquisitionDateBackUrl
      route <- routeRequest(backUrl)
    } yield route
  }

  //################### Acquisition Value methods #######################
  val acquisitionValue = Action.async { implicit request =>
    calcConnector.fetchAndGetFormData[AcquisitionValueModel](KeystoreKeys.acquisitionValue).map {
      case Some(data) => Ok(calculation.acquisitionValue(acquisitionValueForm.fill(data)))
      case None => Ok(calculation.acquisitionValue(acquisitionValueForm))
    }
  }

  val submitAcquisitionValue = Action.async { implicit request =>
    acquisitionValueForm.bindFromRequest.fold(
      errors => Future.successful(BadRequest(calculation.acquisitionValue(errors))),
      success => {
        calcConnector.saveFormData(KeystoreKeys.acquisitionValue, success)
        calcConnector.fetchAndGetFormData[AcquisitionDateModel](KeystoreKeys.acquisitionDate).flatMap (connection =>
        if (!Dates.dateAfterStart(
            connection.get.day.getOrElse(0),
            connection.get.month.getOrElse(0),
            connection.get.year.getOrElse(0))
          )
        {
          Future.successful(Redirect(routes.CalculationController.rebasedValue()))
        }
        else {
          Future.successful(Redirect(routes.CalculationController.improvements()))
        }
        )
      }
    )
  }

  //################### Rebased value methods #######################
  val rebasedValue = Action.async {implicit request =>
    calcConnector.fetchAndGetFormData[RebasedValueModel](KeystoreKeys.rebasedValue).map {
      case Some(data) => Ok(calculation.rebasedValue(rebasedValueForm.fill(data)))
      case None => Ok(calculation.rebasedValue(rebasedValueForm))
    }
  }

  val submitRebasedValue = Action.async { implicit request =>
    rebasedValueForm.bindFromRequest.fold(
      errors => Future.successful(BadRequest(calculation.rebasedValue(errors))),
      success => {
        calcConnector.saveFormData(KeystoreKeys.rebasedValue, success)
        success.hasRebasedValue match {
          case "Yes" => Future.successful(Redirect(routes.CalculationController.rebasedCosts()))
          case "No" => Future.successful(Redirect(routes.CalculationController.improvements()))
        }
      }
    )
  }

  //################### Rebased costs methods #######################
  val rebasedCosts = Action.async {implicit request =>
    calcConnector.fetchAndGetFormData[RebasedCostsModel](KeystoreKeys.rebasedCosts).map {
      case Some(data) => Ok(calculation.rebasedCosts(rebasedCostsForm.fill(data)))
      case None => Ok(calculation.rebasedCosts(rebasedCostsForm))
    }
  }

  val submitRebasedCosts = Action.async {implicit request =>
    rebasedCostsForm.bindFromRequest.fold(
      errors => Future.successful(BadRequest(calculation.rebasedCosts(errors))),
      success => {
        calcConnector.saveFormData(KeystoreKeys.rebasedCosts, success)
        Future.successful(Redirect(routes.CalculationController.improvements()))
      }
    )
  }

  //################### Improvements methods #######################
  def improvementsBackUrl(implicit hc: HeaderCarrier): Future[String] = {

    def checkRebasedValue = {
      calcConnector.fetchAndGetFormData[RebasedValueModel](KeystoreKeys.rebasedValue).flatMap {
        case Some(rebasedData) if rebasedData.hasRebasedValue == "Yes" =>
          Future.successful(routes.CalculationController.rebasedCosts().url)
        case Some(rebasedData) if rebasedData.hasRebasedValue == "No" =>
          Future.successful(routes.CalculationController.rebasedValue().url)
        case _ => Future.successful(missingDataRoute)
      }
    }

    calcConnector.fetchAndGetFormData[AcquisitionDateModel](KeystoreKeys.acquisitionDate).flatMap {
      case Some(acquisitionData) if acquisitionData.hasAcquisitionDate == "Yes" =>
        if (Dates.dateAfterStart(acquisitionData.day.get, acquisitionData.month.get, acquisitionData.year.get))
          Future.successful(routes.CalculationController.acquisitionValue().url)
        else
          checkRebasedValue
      case Some(acquisitionData) if acquisitionData.hasAcquisitionDate == "No" =>
        checkRebasedValue
      case _ => Future.successful(missingDataRoute)
    }
  }

  val improvements = Action.async { implicit request =>

    def routeRequest(backUrl: String): Future[Result] = {
      calcConnector.fetchAndGetFormData[RebasedValueModel](KeystoreKeys.rebasedValue).flatMap(rebasedValueModel =>
        calcConnector.fetchAndGetFormData[ImprovementsModel](KeystoreKeys.improvements).map {
          case Some(data) =>
            Ok(calculation.improvements(improvementsForm.fill(data), rebasedValueModel.getOrElse(RebasedValueModel("No", None)).hasRebasedValue, backUrl))
          case None =>
            Ok(calculation.improvements(improvementsForm, rebasedValueModel.getOrElse(RebasedValueModel("No", None)).hasRebasedValue, backUrl))
        }
      )
    }

    for {
      backUrl <- improvementsBackUrl
      route <- routeRequest(backUrl)
    } yield route
  }

  val submitImprovements = Action.async { implicit request =>

    def routeRequest(backUrl: String): Future[Result] = {
      improvementsForm.bindFromRequest.fold(
        errors => {
          calcConnector.fetchAndGetFormData[RebasedValueModel](KeystoreKeys.rebasedValue).flatMap(rebasedValueModel =>
            Future.successful(BadRequest(calculation.improvements(errors, rebasedValueModel.getOrElse(RebasedValueModel("No", None)).hasRebasedValue, backUrl))))
        },
        success => {
          calcConnector.saveFormData(KeystoreKeys.improvements, success)
          Future.successful(Redirect(routes.CalculationController.disposalDate()))
        }
      )
    }

    for {
      backUrl <- improvementsBackUrl
      route <- routeRequest(backUrl)
    } yield route
  }

  //################### Disposal Date methods #######################
  val disposalDate = Action.async { implicit request =>
    calcConnector.fetchAndGetFormData[DisposalDateModel](KeystoreKeys.disposalDate).map {
      case Some(data) => Ok(calculation.disposalDate(disposalDateForm.fill(data)))
      case None => Ok(calculation.disposalDate(disposalDateForm))
    }
  }

  val submitDisposalDate = Action.async { implicit request =>
    disposalDateForm.bindFromRequest.fold(
      errors => Future.successful(BadRequest(calculation.disposalDate(errors))),
      success => {
        calcConnector.saveFormData(KeystoreKeys.disposalDate, success)
        Future.successful(Redirect(routes.CalculationController.disposalValue()))
      }
    )
  }

  //################### Disposal Value methods #######################
  val disposalValue = Action.async { implicit request =>
    calcConnector.fetchAndGetFormData[DisposalValueModel](KeystoreKeys.disposalValue).map {
      case Some(data) => Ok(calculation.disposalValue(disposalValueForm.fill(data)))
      case None => Ok(calculation.disposalValue(disposalValueForm))
    }
  }

  val submitDisposalValue = Action.async { implicit request =>
    disposalValueForm.bindFromRequest.fold(
      errors => Future.successful(BadRequest(calculation.disposalValue(errors))),
      success => {
        calcConnector.saveFormData(KeystoreKeys.disposalValue, success)
        Future.successful(Redirect(routes.CalculationController.acquisitionCosts()))
      }
    )
  }

  //################### Acquisition Costs methods #######################
  val acquisitionCosts = Action.async { implicit request =>
    calcConnector.fetchAndGetFormData[AcquisitionCostsModel](KeystoreKeys.acquisitionCosts).map {
      case Some(data) => Ok(calculation.acquisitionCosts(acquisitionCostsForm.fill(data)))
      case None => Ok(calculation.acquisitionCosts(acquisitionCostsForm))
    }
  }

  val submitAcquisitionCosts = Action.async { implicit request =>
    acquisitionCostsForm.bindFromRequest.fold(
      errors => Future.successful(BadRequest(calculation.acquisitionCosts(errors))),
      success => {
        calcConnector.saveFormData(KeystoreKeys.acquisitionCosts, success)
        Future.successful(Redirect(routes.CalculationController.disposalCosts()))
      }
    )
  }

  //################### Disposal Costs methods #######################
  val disposalCosts = Action.async { implicit request =>
    calcConnector.fetchAndGetFormData[DisposalCostsModel](KeystoreKeys.disposalCosts).map {
      case Some(data) => Ok(calculation.disposalCosts(disposalCostsForm.fill(data)))
      case None => Ok(calculation.disposalCosts(disposalCostsForm))
    }
  }

  val submitDisposalCosts = Action.async { implicit request =>
    disposalCostsForm.bindFromRequest.fold(
      errors => Future.successful(BadRequest(calculation.disposalCosts(errors))),
      success => {
        calcConnector.saveFormData(KeystoreKeys.disposalCosts, success)
        calcConnector.fetchAndGetFormData[AcquisitionDateModel](KeystoreKeys.acquisitionDate).flatMap {
          case Some(data) if data.hasAcquisitionDate == "Yes" =>
            Future.successful(Redirect(routes.CalculationController.privateResidenceRelief()))
          case _ => {
            calcConnector.fetchAndGetFormData[RebasedValueModel](KeystoreKeys.rebasedValue).flatMap {
              case Some(rebasedData) if rebasedData.hasRebasedValue == "Yes" => {
                Future.successful(Redirect(routes.CalculationController.privateResidenceRelief()))
              }
              case _ => {
                Future.successful(Redirect(routes.CalculationController.entrepreneursRelief()))
              }
            }
          }
        }
      }
    )
  }

  //################### Private Residence Relief methods #######################
  def getDisposalDate(implicit hc: HeaderCarrier): Future[Option[Date]] =
    calcConnector.fetchAndGetFormData[DisposalDateModel](KeystoreKeys.disposalDate).map {
      case Some(data) => Some(Dates.constructDate(data.day, data.month, data.year))
      case _ => None
  }

  def getAcquisitionDate(implicit hc: HeaderCarrier): Future[Option[Date]] =
    calcConnector.fetchAndGetFormData[AcquisitionDateModel](KeystoreKeys.acquisitionDate).map {
      case Some(data) => data.hasAcquisitionDate match {
        case "Yes" => Some(Dates.constructDate(data.day.get, data.month.get, data.year.get))
        case _ => None
    }
    case _ => None
  }

  def getRebasedAmount(implicit hc: HeaderCarrier): Future[Boolean] =
    calcConnector.fetchAndGetFormData[RebasedValueModel](KeystoreKeys.rebasedValue).map {
      case Some(data) if data.hasRebasedValue == "Yes" => true
      case _ => false
  }

  def displayBetweenQuestion(disposalDate: Option[Date], acquisitionDate: Option[Date], hasRebasedValue: Boolean): Boolean =
    disposalDate match {
      case Some(date) =>
        if (Dates.dateAfterOctober(date)) {
          acquisitionDate match {
            case Some(acquisitionDateValue) if !Dates.dateAfterStart(acquisitionDateValue) => true
            case _ => if (hasRebasedValue) true else false
          }
        } else false
      case _ => false
  }

  def displayBeforeQuestion(disposalDate: Option[Date], acquisitionDate: Option[Date], hasRebasedValue: Boolean): Boolean = disposalDate match {
    case Some(disposalDateValue) =>
      if (Dates.dateAfterOctober(disposalDateValue)) {
        acquisitionDate match {
          case Some(acquisitionDateValue) => true
          case _ => false
        }
      } else {
        acquisitionDate match {
          case Some(acquisitionDateValue) if !Dates.dateAfterStart(acquisitionDateValue) => true
          case _ => false
        }
      }
    case _ => false
  }

  val privateResidenceRelief = Action.async { implicit request =>

    def action(disposalDate: Option[Date], acquisitionDate: Option[Date], hasRebasedValue: Boolean) = {

      val showBetweenQuestion = displayBetweenQuestion(disposalDate, acquisitionDate, hasRebasedValue)
      val showBeforeQuestion = displayBeforeQuestion(disposalDate, acquisitionDate, hasRebasedValue)
      val disposalDateLess18Months = Dates.dateMinusMonths(disposalDate,18)

      calcConnector.fetchAndGetFormData[PrivateResidenceReliefModel](KeystoreKeys.privateResidenceRelief).map {
        case Some(data) => Ok(calculation.privateResidenceRelief(privateResidenceReliefForm.fill(data), showBetweenQuestion, showBeforeQuestion, disposalDateLess18Months))
        case None => Ok(calculation.privateResidenceRelief(privateResidenceReliefForm, showBetweenQuestion, showBeforeQuestion, disposalDateLess18Months))
      }

    }

    for {
      disposalDate <- getDisposalDate
      acquisitionDate <- getAcquisitionDate
      hasRebasedValue <- getRebasedAmount
      finalResult <- action(disposalDate, acquisitionDate, hasRebasedValue)
    } yield finalResult
  }

  val submitPrivateResidenceRelief = Action.async { implicit request =>

    def action(disposalDate: Option[Date], acquisitionDate: Option[Date], hasRebasedValue: Boolean) = {

      privateResidenceReliefForm.bindFromRequest.fold(
        errors => {
          val showBetweenQuestion = displayBetweenQuestion(disposalDate, acquisitionDate, hasRebasedValue)
          val showBeforeQuestion = displayBeforeQuestion(disposalDate, acquisitionDate, hasRebasedValue)
          val disposalDateLess18Months = Dates.dateMinusMonths(disposalDate,18)
          Future.successful(BadRequest(calculation.privateResidenceRelief(errors,showBetweenQuestion, showBeforeQuestion, disposalDateLess18Months)))
        },
        success => {
          calcConnector.saveFormData(KeystoreKeys.privateResidenceRelief, success)
          Future.successful(Redirect(routes.CalculationController.entrepreneursRelief()))
        }
      )
    }

    for {
      disposalDate <- getDisposalDate
      acquisitionDate <- getAcquisitionDate
      hasRebasedValue <- getRebasedAmount
      finalResult <- action(disposalDate, acquisitionDate, hasRebasedValue)
    } yield finalResult
  }

  //################### Entrepreneurs Relief methods #######################
  def entrepreneursReliefBackUrl(implicit hc: HeaderCarrier): Future[String] = {
    calcConnector.fetchAndGetFormData[AcquisitionDateModel](KeystoreKeys.acquisitionDate).flatMap {
      case Some(acquisitionData) if acquisitionData.hasAcquisitionDate == "Yes" =>
        Future.successful(routes.CalculationController.privateResidenceRelief().url)
      case _ => calcConnector.fetchAndGetFormData[RebasedValueModel](KeystoreKeys.rebasedValue).flatMap {
        case Some(rebasedData) if rebasedData.hasRebasedValue == "Yes" =>
          Future.successful(routes.CalculationController.privateResidenceRelief().url)
        case _ => Future.successful(routes.CalculationController.disposalCosts().url)
      }
    }
  }

  val entrepreneursRelief = Action.async { implicit request =>
    def routeRequest(backUrl: String) = {
      calcConnector.fetchAndGetFormData[EntrepreneursReliefModel](KeystoreKeys.entrepreneursRelief).map {
        case Some(data) => Ok(calculation.entrepreneursRelief(entrepreneursReliefForm.fill(data), backUrl))
        case _ => Ok(calculation.entrepreneursRelief(entrepreneursReliefForm, backUrl))
      }
    }

    for {
      backUrl <- entrepreneursReliefBackUrl
      route <- routeRequest(backUrl)
    } yield route
  }

  val submitEntrepreneursRelief = Action.async { implicit request =>

    def routeRequest(backUrl: String) = {
      entrepreneursReliefForm.bindFromRequest.fold(
        errors => Future.successful(BadRequest(calculation.entrepreneursRelief(errors, backUrl))),
        success => {
          calcConnector.saveFormData(KeystoreKeys.entrepreneursRelief, success)
          Future.successful(Redirect(routes.CalculationController.allowableLosses()))
        }
      )
    }

    for {
      backUrl <- entrepreneursReliefBackUrl
      route <- routeRequest(backUrl)
    } yield route
  }

  //################### Allowable Losses methods #######################
  val allowableLosses = Action.async { implicit request =>
    calcConnector.fetchAndGetFormData[AllowableLossesModel](KeystoreKeys.allowableLosses).map {
      case Some(data) => Ok(calculation.allowableLosses(allowableLossesForm.fill(data)))
      case None => Ok(calculation.allowableLosses(allowableLossesForm))
    }
  }

  val submitAllowableLosses = Action.async { implicit request =>
    allowableLossesForm.bindFromRequest.fold(
      errors => Future.successful(BadRequest(calculation.allowableLosses(errors))),
      success => {
        calcConnector.saveFormData(KeystoreKeys.allowableLosses, success)
        calcConnector.fetchAndGetFormData[AcquisitionDateModel](KeystoreKeys.acquisitionDate).flatMap {
          case Some(data) if data.hasAcquisitionDate == "Yes" =>
              if (Dates.dateAfterStart(data.day.get, data.month.get, data.year.get)) {
                calcConnector.saveFormData(KeystoreKeys.calculationElection, CalculationElectionModel("flat"))
                Future.successful(Redirect(routes.CalculationController.otherReliefs()))
              }
              else Future.successful(Redirect(routes.CalculationController.calculationElection()))
          case _ => {
            calcConnector.fetchAndGetFormData[RebasedValueModel](KeystoreKeys.rebasedValue).flatMap {
              case Some(rebasedData) if rebasedData.hasRebasedValue == "Yes" => {
                Future.successful(Redirect(routes.CalculationController.calculationElection()))
              }
              case _ => {
                calcConnector.saveFormData(KeystoreKeys.calculationElection, CalculationElectionModel("flat"))
                Future.successful(Redirect(routes.CalculationController.otherReliefs()))
              }
            }
          }
        }
      }
    )
  }

  //################### Calculation Election methods #######################
  val calculationElection = Action.async { implicit request =>

    def action (construct: SummaryModel, content: Seq[(String, String, String, Option[String], String)]) =
    calcConnector.fetchAndGetFormData[CalculationElectionModel](KeystoreKeys.calculationElection).map {
      case Some(data) => Ok(calculation.calculationElection(calculationElectionForm.fill(data), construct, content))
      case None => Ok(calculation.calculationElection(calculationElectionForm, construct, content))
    }

    def calcTimeCall(summary: SummaryModel): Future[Option[CalculationResultModel]] = {
      summary.acquisitionDateModel.hasAcquisitionDate match {
        case "Yes" => if (Dates.dateAfterStart(summary.acquisitionDateModel.day.get, summary.acquisitionDateModel.month.get, summary.acquisitionDateModel.year.get)) {
          Future(None)
        }
        else {
          calcConnector.calculateTA(summary)
        }
        case "No" => Future(None)
      }
    }

    def calcRebasedCall(summary: SummaryModel): Future[Option[CalculationResultModel]] = {
      summary.rebasedValueModel.getOrElse(RebasedValueModel("No", None)).hasRebasedValue match {
        case "Yes" => summary.acquisitionDateModel.hasAcquisitionDate match {
          case "Yes" => if (Dates.dateAfterStart(summary.acquisitionDateModel.day.get, summary.acquisitionDateModel.month.get, summary.acquisitionDateModel.year.get)) {
            Future(None)
          }
          else {
            calcConnector.calculateRebased(summary)
          }
          case "No" => calcConnector.calculateRebased(summary)
        }
        case "No" => Future(None)
      }
    }

    for {
      construct <- calcConnector.createSummary(hc)
      calcFlat <- calcConnector.calculateFlat(construct)
      calcTA <- calcTimeCall(construct)
      calcRebased <- calcRebasedCall(construct)
      finalResult <- action(construct, calcElectionConstructor.generateElection(construct, hc, calcFlat, calcTA, calcRebased))
    } yield finalResult
  }

  val submitCalculationElection = Action.async { implicit request =>

    def calcTimeCall(summary: SummaryModel): Future[Option[CalculationResultModel]] = {
      summary.acquisitionDateModel.hasAcquisitionDate match {
        case "Yes" => if (Dates.dateAfterStart(summary.acquisitionDateModel.day.get, summary.acquisitionDateModel.month.get, summary.acquisitionDateModel.year.get)) {
          Future(None)
        }
        else {
          calcConnector.calculateTA(summary)
        }
        case "No" => Future(None)
      }
    }

    def calcRebasedCall(summary: SummaryModel): Future[Option[CalculationResultModel]] = {
      summary.rebasedValueModel.getOrElse(RebasedValueModel("No", None)).hasRebasedValue match {
        case "Yes" => summary.acquisitionDateModel.hasAcquisitionDate match {
          case "Yes" => if (Dates.dateAfterStart(summary.acquisitionDateModel.day.get, summary.acquisitionDateModel.month.get, summary.acquisitionDateModel.year.get)) {
            Future(None)
          }
          else {
            calcConnector.calculateRebased(summary)
          }
          case "No" => calcConnector.calculateRebased(summary)
        }
        case "No" => Future(None)
      }
    }

    calculationElectionForm.bindFromRequest.fold(
      errors => {
        for{
          construct <- calcConnector.createSummary(hc)
          calcFlat <- calcConnector.calculateFlat(construct)
          calcTA <- calcTimeCall(construct)
          calcRebased <- calcRebasedCall(construct)
        } yield {BadRequest(calculation.calculationElection(errors, construct, calcElectionConstructor.generateElection(construct, hc, calcFlat, calcTA, calcRebased)))}
      },
      success => {
        calcConnector.saveFormData(KeystoreKeys.calculationElection, success)
        Future.successful(Redirect(routes.CalculationController.summary()))
      }
    )
  }

  //################### Other Reliefs methods #######################
  def otherReliefsBackUrl(implicit hc: HeaderCarrier): Future[String] = {
    calcConnector.fetchAndGetFormData[AcquisitionDateModel](KeystoreKeys.acquisitionDate).flatMap {
      case Some(acquisitionData) if acquisitionData.hasAcquisitionDate == "Yes" =>
        if (Dates.dateAfterStart(acquisitionData.day.get, acquisitionData.month.get, acquisitionData.year.get)) {
          Future.successful(routes.CalculationController.allowableLosses().url)
        } else {
          Future.successful(routes.CalculationController.calculationElection().url)
        }
      case Some(acquisitionData) if acquisitionData.hasAcquisitionDate == "No" =>
        calcConnector.fetchAndGetFormData[RebasedValueModel](KeystoreKeys.rebasedValue).flatMap {
          case Some(rebasedData) =>
            if (rebasedData.hasRebasedValue == "Yes") {
              Future.successful(routes.CalculationController.calculationElection().url)
            } else {
              Future.successful(routes.CalculationController.allowableLosses().url)
            }
          case _ => Future.successful(missingDataRoute)
        }
      case _ => Future.successful(missingDataRoute)
    }
  }

  val otherReliefs = Action.async { implicit request =>

    def action (dataResult: Option[CalculationResultModel], backUrl: String) = {
      calcConnector.fetchAndGetFormData[OtherReliefsModel](KeystoreKeys.otherReliefsFlat).map {
        case Some(data) => Ok(calculation.otherReliefs(otherReliefsForm.fill(data), dataResult.get, backUrl))
        case None => Ok(calculation.otherReliefs(otherReliefsForm, dataResult.get, backUrl))
      }
    }

    for {
      construct <- calcConnector.createSummary(hc)
      calculation <- calcConnector.calculateFlat(construct)
      backUrl <- otherReliefsBackUrl
      finalResult <- action(calculation, backUrl)
    } yield finalResult
  }

  val submitOtherReliefs = Action.async { implicit request =>

    def action (dataResult: Option[CalculationResultModel], construct: SummaryModel, backUrl: String) = otherReliefsForm.bindFromRequest.fold(
      errors => Future.successful(BadRequest(calculation.otherReliefs(errors, dataResult.get, backUrl))),
      success => {
        calcConnector.saveFormData(KeystoreKeys.otherReliefsFlat, success)
        construct.acquisitionDateModel.hasAcquisitionDate match {
          case "Yes" if Dates.dateAfterStart(construct.acquisitionDateModel.day.get,
            construct.acquisitionDateModel.month.get, construct.acquisitionDateModel.year.get) => {
            Future.successful(Redirect(routes.CalculationController.summary()))
          }
          case "Yes" if !Dates.dateAfterStart(construct.acquisitionDateModel.day.get,
            construct.acquisitionDateModel.month.get, construct.acquisitionDateModel.year.get) => {
            Future.successful(Redirect(routes.CalculationController.calculationElection()))
          }
          case "No" => construct.rebasedValueModel.getOrElse(RebasedValueModel("No", None)).hasRebasedValue match {
            case "Yes" => Future.successful(Redirect(routes.CalculationController.calculationElection()))
            case "No" => Future.successful(Redirect(routes.CalculationController.summary()))
          }
        }
      }
    )

    for {
      construct <- calcConnector.createSummary(hc)
      calculation <- calcConnector.calculateFlat(construct)
      backUrl <- otherReliefsBackUrl
      finalResult <- action(calculation, construct, backUrl)
    } yield finalResult
  }

  //################### Time Apportioned Other Reliefs methods #######################
  def otherReliefsTA: Action[AnyContent] = Action.async { implicit request =>
    def action (dataResult: Option[CalculationResultModel]) = calcConnector.fetchAndGetFormData[OtherReliefsModel](KeystoreKeys.otherReliefsTA).map {
      case Some(data) => Ok(calculation.otherReliefsTA(otherReliefsForm.fill(data), dataResult.get))
      case None => Ok(calculation.otherReliefsTA(otherReliefsForm, dataResult.get))
    }

    for {
      construct <- calcConnector.createSummary(hc)
      calculation <- calcConnector.calculateTA(construct)
      finalResult <- action(calculation)
    } yield finalResult
  }

  val submitOtherReliefsTA = Action.async { implicit request =>
    def action(dataResult: Option[CalculationResultModel]) = otherReliefsForm.bindFromRequest.fold(
      errors => Future.successful(BadRequest(calculation.otherReliefsTA(errors, dataResult.get))),
      success => {
        calcConnector.saveFormData(KeystoreKeys.otherReliefsTA, success)
        Future.successful(Redirect(routes.CalculationController.calculationElection()))
      }
    )

    for {
      construct <- calcConnector.createSummary(hc)
      calculation <- calcConnector.calculateTA(construct)
      finalResult <- action(calculation)
    } yield finalResult
  }

  //################### Rebased Other Reliefs methods #######################
  val otherReliefsRebased = Action.async { implicit request =>
    def action (dataResult: Option[CalculationResultModel]) = calcConnector.fetchAndGetFormData[OtherReliefsModel](KeystoreKeys.otherReliefsRebased).map {
      case Some(data) => Ok(calculation.otherReliefsRebased(otherReliefsForm.fill(data), dataResult.get))
      case None => Ok(calculation.otherReliefsRebased(otherReliefsForm, dataResult.get))
    }

    for {
      construct <- calcConnector.createSummary(hc)
      calculation <- calcConnector.calculateRebased(construct)
      finalResult <- action(calculation)
    } yield finalResult
  }

  val submitOtherReliefsRebased = Action.async { implicit request =>
    def action(dataResult: Option[CalculationResultModel]) = otherReliefsForm.bindFromRequest.fold(
      errors => Future.successful(BadRequest(calculation.otherReliefsRebased(errors, dataResult.get))),
      success => {
        calcConnector.saveFormData(KeystoreKeys.otherReliefsRebased, success)
        Future.successful(Redirect(routes.CalculationController.calculationElection()))
      }
    )

    for {
      construct <- calcConnector.createSummary(hc)
      calculation <- calcConnector.calculateRebased(construct)
      finalResult <- action(calculation)
    } yield finalResult
  }

  //################### Summary Methods ##########################
  def summaryBackUrl(implicit hc: HeaderCarrier): Future[String] = {
    calcConnector.fetchAndGetFormData[AcquisitionDateModel](KeystoreKeys.acquisitionDate).flatMap {
      case Some(acquisitionData) if acquisitionData.hasAcquisitionDate == "Yes" =>
        if (Dates.dateAfterStart(acquisitionData.day.get, acquisitionData.month.get, acquisitionData.year.get)) {
          Future.successful(routes.CalculationController.otherReliefs().url)
        } else {
          Future.successful(routes.CalculationController.calculationElection().url)
        }
      case Some(acquisitionData) if acquisitionData.hasAcquisitionDate == "No" =>
        calcConnector.fetchAndGetFormData[RebasedValueModel](KeystoreKeys.rebasedValue).flatMap {
          case Some(rebasedData) =>
            if (rebasedData.hasRebasedValue == "Yes") {
              Future.successful(routes.CalculationController.calculationElection().url)
            } else {
              Future.successful(routes.CalculationController.otherReliefs().url)
            }
          case _ => Future.successful(missingDataRoute)
        }
      case _ => Future.successful(missingDataRoute)
    }
  }
  val summary = Action.async { implicit request =>

    def routeRequest(backUrl: String) = {
      calcConnector.createSummary(hc).flatMap(summaryData =>
        summaryData.calculationElectionModel.calculationType match {
          case "flat" =>
            calcConnector.calculateFlat(summaryData).map(result =>
              Ok(calculation.summary(summaryData, result.get, backUrl)))
          case "time" =>
            calcConnector.calculateTA(summaryData).map(result =>
              Ok(calculation.summary(summaryData, result.get, backUrl)))
          case "rebased" =>
            calcConnector.calculateRebased(summaryData).map(result =>
              Ok(calculation.summary(summaryData, result.get, backUrl)))
        })
    }

    for {
      backUrl <- summaryBackUrl
      route <- routeRequest(backUrl)
    } yield route
  }

  def restart() = Action.async { implicit request =>
    calcConnector.clearKeystore()
    Future.successful(Redirect(routes.IntroductionController.introduction()))
  }
}
