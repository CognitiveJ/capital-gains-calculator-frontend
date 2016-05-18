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

package constructors

import common.Dates
import models.{PrivateResidenceReliefModel, AcquisitionDateModel, RebasedValueModel, SummaryModel}

object CalculateRequestConstructor {

  def baseCalcUrl(input: SummaryModel): String = {
    s"customerType=${
      input.customerTypeModel.customerType}&priorDisposal=${
      input.otherPropertiesModel.otherProperties}${
      input.otherPropertiesModel.otherProperties match {
        case "Yes" => "&annualExemptAmount=" + input.annualExemptAmountModel.get.annualExemptAmount + "&otherPropertiesAmt=" + input.otherPropertiesModel.otherPropertiesAmt.get
        case "No" => ""
      }
    }${
      input.disabledTrusteeModel match {
        case Some(data) => "&isVulnerable=" + data.isVulnerable
        case None => ""
      }
    }${
      input.currentIncomeModel match {
        case Some(data) => "&currentIncome=" + data.currentIncome
        case None => ""
      }
    }${
      input.personalAllowanceModel match {
        case Some(data) => "&personalAllowanceAmt=" + data.personalAllowanceAmt
        case None => ""
      }
    }&disposalValue=${
      input.disposalValueModel.disposalValue
    }&disposalCosts=${
      input.disposalCostsModel.disposalCosts.getOrElse(0)
    }&allowableLossesAmt=${
      input.allowableLossesModel.isClaimingAllowableLosses match {
        case "Yes" => input.allowableLossesModel.allowableLossesAmt.get
        case "No" => 0
      }
    }&entReliefClaimed=${
      input.entrepreneursReliefModel.entReliefClaimed
    }"
  }

  def flatCalcUrlExtra(input: SummaryModel): String = {
    s"${improvements(input)
    }${acquisition(input)
    }&reliefs=${
      input.otherReliefsModelFlat.otherReliefs.getOrElse(0)
    }${privateResidenceReliefFlat(input)
    }${isClaimingPRR(input)}"
  }

  def taCalcUrlExtra(input: SummaryModel): String = {
    s"${improvements(input)
    }&disposalDate=${
      input.disposalDateModel.year}-${input.disposalDateModel.month}-${input.disposalDateModel.day
    }&acquisitionDate=${
      input.acquisitionDateModel.year.get}-${input.acquisitionDateModel.month.get}-${input.acquisitionDateModel.day.get
    }${acquisition(input)
    }&reliefs=${
      input.otherReliefsModelTA.otherReliefs.getOrElse(0)
    }${privateResidenceReliefTA(input)
    }${isClaimingPRR(input)}"
  }

  def rebasedCalcUrlExtra(input: SummaryModel): String = {
    s"&improvementsAmt=${input.improvementsModel.isClaimingImprovements match {
      case "Yes" => input.improvementsModel.improvementsAmtAfter.getOrElse(0)
      case "No" => 0
    }
    }&rebasedValue=${input.rebasedValueModel.get.rebasedValueAmt.get
    }&revaluationCost=${input.rebasedCostsModel.get.hasRebasedCosts match {
      case "Yes" => input.rebasedCostsModel.get.rebasedCosts.get
      case "No" => 0
    }
    }&reliefs=${
      input.otherReliefsModelRebased.otherReliefs.getOrElse(0)
    }${privateResidenceReliefRebased(input)
    }&isClaimingPRR=${input.privateResidenceReliefModel match {
      case Some(PrivateResidenceReliefModel("Yes", claimed, after)) => "Yes"
      case _ => "No"
    }}"
  }

  def improvements (input: SummaryModel) = s"&improvementsAmt=${
    input.improvementsModel.isClaimingImprovements match {
      case "Yes" => {
        input.rebasedValueModel match {
          case Some(data) => data.hasRebasedValue match {
            case "Yes" => input.improvementsModel.improvementsAmtAfter.getOrElse(BigDecimal(0)) + input.improvementsModel.improvementsAmt.getOrElse(BigDecimal(0))
            case "No" => input.improvementsModel.improvementsAmt.getOrElse(0)
          }
          case None => input.improvementsModel.improvementsAmt.getOrElse(0)
        }
      }
      case "No" => 0
    }
  }"

  def privateResidenceReliefFlat (input: SummaryModel) = s"${
    (input.acquisitionDateModel, input.privateResidenceReliefModel) match {
      case (AcquisitionDateModel("Yes", day, month, year), Some(PrivateResidenceReliefModel("Yes", claimed, after))) if claimed.isDefined =>
        s"&daysClaimed=${claimed.get}"
      case _ => ""
    }
  }"

  def privateResidenceReliefTA (input: SummaryModel) = s"${
    (input.acquisitionDateModel, input.privateResidenceReliefModel) match {
      case (AcquisitionDateModel("Yes", day, month, year), Some(PrivateResidenceReliefModel("Yes", claimed, after)))
        if Dates.dateAfter18Months(input.disposalDateModel.day, input.disposalDateModel.month, input.disposalDateModel.year) && after.isDefined =>
        s"&daysClaimedAfter=${after.get}"

      case _ => ""
    }
  }"

  def privateResidenceReliefRebased (input: SummaryModel) = s"${
    (input.rebasedValueModel, input.privateResidenceReliefModel) match {
      case (Some(RebasedValueModel("Yes", rebasedValue)), Some(PrivateResidenceReliefModel("Yes", claimed, after)))
        if Dates.dateAfter18Months(input.disposalDateModel.day, input.disposalDateModel.month, input.disposalDateModel.year) && after.isDefined =>
        s"&daysClaimedAfter=${after.get}"
      case _ => ""
    }
  }"

//  def daysClaimedAcquisitionBeforeStart(input: SummaryModel, claimed: Option[BigDecimal], after: Option[BigDecimal]) = {
//    if (input.rebasedValueModel.get.hasRebasedValue == "No") s"&daysClaimed=${claimed.get}"
//    else if (Dates.dateAfter18Months(input.disposalDateModel.day, input.disposalDateModel.month, input.disposalDateModel.year))
//      s"&daysClaimed=${claimed.getOrElse(BigDecimal(0)) + after.getOrElse(BigDecimal(0))}"
//    else s"&daysClaimed=${claimed.get}"
//  }
//
//  def daysClaimedDisposalAfter18Months(input: SummaryModel, claimed: Option[BigDecimal], after: Option[BigDecimal]) = {
//    if (input.acquisitionDateModel.hasAcquisitionDate == "No") s"&daysClaimedAfter=${claimed.get}"
//    else if (!Dates.dateAfterStart(input.acquisitionDateModel.day.get, input.acquisitionDateModel.month.get, input.acquisitionDateModel.year.get)) {
//      s"&daysClaimedAfter=${after.getOrElse(0)}"
//    }
//    else ""
//  }

  def isClaimingPRR (input: SummaryModel) = s"&isClaimingPRR=${
    (input.acquisitionDateModel, input.privateResidenceReliefModel) match {
      case (AcquisitionDateModel("Yes", day, month, year), Some(PrivateResidenceReliefModel("Yes", claimed, after))) => "Yes"
      case _ => "No"
    }
  }"

  def acquisition (input: SummaryModel) = s"&acquisitionValueAmt=${
    input.acquisitionValueModel.acquisitionValueAmt
  }&acquisitionCostsAmt=${
    input.acquisitionCostsModel.acquisitionCostsAmt.getOrElse(0)
  }"
}
