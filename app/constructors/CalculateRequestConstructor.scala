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

import connectors.CalculatorConnector
import models.SummaryModel

object CalculateRequestConstructor {

  def baseCalcUrl(input: SummaryModel): String = {
    s"customerType=${
      input.customerTypeModel.customerType}&priorDisposal=${
      input.otherPropertiesModel.otherProperties}${
      input.annualExemptAmountModel match {
        case Some(data) => "&annualExemptAmount=" + Some(data.annualExemptAmount)
        case None => ""
      }
    }${
      input.disabledTrusteeModel match {
        case Some(data) => "&isVulnerable=" + Some(data.isVulnerable)
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
    }&acquisitionValueAmt=${
      input.acquisitionValueModel.acquisitionValueAmt
    }&acquisitionCostsAmt=${
      input.acquisitionCostsModel.acquisitionCostsAmt.getOrElse(0)
    }&reliefs=${
      input.otherReliefsModel.otherReliefs.getOrElse(0)
    }&allowableLossesAmt=${
      input.allowableLossesModel.allowableLossesAmt.getOrElse(0)
    }&entReliefClaimed=${
      input.entrepreneursReliefModel.entReliefClaimed
    }"
  }

  def flatCalcUrlExtra(input: SummaryModel): String = {
    s"&improvementsAmt=${
      input.improvementsModelFlat.improvementsAmt.getOrElse(0)
    }"
  }

  def taCalcUrlExtra(input: SummaryModel): String = {
    s"&improvementsAmt=${
      input.improvementsModel.improvementsAmt.getOrElse(0)
    }&disposalDate=${
      input.disposalDateModel.day}${input.disposalDateModel.month}${input.disposalDateModel.year
    }&acquisitionDate=${
      input.acquisitionDateModel.day}${input.acquisitionDateModel.month}${input.acquisitionDateModel.year
    }"
  }
}
