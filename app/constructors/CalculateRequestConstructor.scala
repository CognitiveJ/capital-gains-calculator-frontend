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

import models.SummaryModel

object CalculateRequestConstructor {

  def baseCalcUrl(input: SummaryModel): String = {
    s"customerType=${
      input.customerTypeModel.customerType}&priorDisposal=${
      input.otherPropertiesModel.otherProperties}${
      input.otherPropertiesModel.otherProperties match {
        case "Yes" => "&annualExemptAmount=" + input.annualExemptAmountModel.get.annualExemptAmount
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
    }"
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
    }"
  }

  def rebasedCalcUrlExtra(input: SummaryModel): String = {
    s"&improvementsAmt=${input.improvementsModel.isClaimingImprovements match {
      case "Yes" => input.improvementsModel.improvementsAmtAfter.get
      case "No" => 0
    }
    }&rebasedValue=${input.rebasedValueModel.get.rebasedValueAmt.get
    }&revaluationCost=${input.rebasedCostsModel.get.hasRebasedCosts match {
      case "Yes" => input.rebasedCostsModel.get.rebasedCosts.get
      case "No" => 0
    }
    }&reliefs=${
      input.otherReliefsModelRebased.otherReliefs.getOrElse(0)
    }"
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

  def acquisition (input: SummaryModel) = s"&acquisitionValueAmt=${
    input.acquisitionValueModel.acquisitionValueAmt
  }&acquisitionCostsAmt=${
    input.acquisitionCostsModel.acquisitionCostsAmt.getOrElse(0)
  }"
}
