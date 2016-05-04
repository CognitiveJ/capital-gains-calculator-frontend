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

import models.{SummaryModel, CalculationResultModel}
import org.apache.commons.lang3.text.WordUtils
import play.api.i18n.Messages
import views.html.helpers._
import common._

object SummaryConstructor {

  def calculationDetails(result: CalculationResultModel, summary: SummaryModel) = summaryPageSection("calcDetails", Messages("calc.summary.calculation.details.title"),
    result.upperTaxGain match {
      case Some(data) => Array(
        Map(
          "question" -> Messages("calc.summary.calculation.details.calculationElection"),
          "answer" -> (summary.calculationElectionModel.calculationType match {
            case "flat" => Messages("calc.summary.calculation.details.flatCalculation")
            case "time" => Messages("calc.summary.calculation.details.timeCalculation")
          })
        ),
        Map(
          "question" -> Messages("calc.summary.calculation.details.totalGain"),
          "answer" -> ("&pound;" + result.totalGain.setScale(2).toString)
        ),
        Map(
          "question" -> Messages("calc.summary.calculation.details.taxableGain"),
          "answer" -> ("&pound;" + result.taxableGain.setScale(2).toString)
        ),
        Map(
          "question" -> Messages("calc.summary.calculation.details.taxRate"),
          "answer" -> ("&pound;" + result.baseTaxGain.setScale(2).toString + " at " + result.baseTaxRate + "%")
        ),
        Map(
          "question" -> "",
          "answer" -> ("&pound;" + result.upperTaxGain.get.setScale(2).toString + " at " + result.upperTaxRate.get.toString + "%")
        )
      )
      case None => Array(
        Map(
          "question" -> Messages("calc.summary.calculation.details.calculationElection"),
          "answer" -> (summary.calculationElectionModel.calculationType match {
            case "flat" => Messages("calc.summary.calculation.details.flatCalculation")
            case "time" => Messages("calc.summary.calculation.details.timeCalculation")
          })
        ),
        Map(
          "question" -> Messages("calc.summary.calculation.details.totalGain"),
          "answer" -> ("&pound;" + result.totalGain.setScale(2).toString)
        ),
        Map(
          "question" -> Messages("calc.summary.calculation.details.taxableGain"),
          "answer" -> ("&pound;" + (result.baseTaxGain + result.upperTaxGain.getOrElse(0)).setScale(2).toString)
        ),
        Map(
          "question" -> Messages("calc.summary.calculation.details.taxRate"),
          "answer" -> (result.baseTaxRate + "%"))
      )
    }
  )

  def personalDetails(result: CalculationResultModel, summary: SummaryModel) = {
    summaryPageSection("personalDetails", Messages("calc.summary.personal.details.title"),
      summary.customerTypeModel.customerType match {
        case "trustee" => Array(
          Map(
            "question" -> Messages("calc.customerType.question"),
            "answer" -> WordUtils.capitalize(summary.customerTypeModel.customerType)
          ),
          Map(
            "question" -> Messages("calc.disabledTrustee.question"),
            "answer" -> summary.disabledTrusteeModel.get.isVulnerable
          ),
          Map(
            "question" -> Messages("calc.annualExemptAmount.question"),
            "answer" -> ("&pound;" + (summary.otherPropertiesModel.otherProperties match {
              case "Yes" => summary.annualExemptAmountModel.get.annualExemptAmount.setScale(2).toString
              case "No" => summary.disabledTrusteeModel.get.isVulnerable match {
                case "Yes" => "11100.00"
                case _ => "5050.00"
              }
            }))
          )
        )
        case "individual" => Array(
          Map(
            "question" -> Messages("calc.customerType.question"),
            "answer" -> WordUtils.capitalize(summary.customerTypeModel.customerType)
          ),
          Map(
            "question" -> Messages("calc.currentIncome.question"),
            "answer" -> ("&pound;" + summary.currentIncomeModel.get.currentIncome.setScale(2))
          ),
          Map(
            "question" -> Messages("calc.personalAllowance.question"),
            "answer" -> ("&pound;" + summary.personalAllowanceModel.get.personalAllowanceAmt.setScale(2))
          ),
          Map(
            "question" -> Messages("calc.annualExemptAmount.question"),
            "answer" -> ("&pound;" + (summary.otherPropertiesModel.otherProperties match {
              case "Yes" => summary.annualExemptAmountModel.get.annualExemptAmount.setScale(2).toString
              case "No" => "11100.00"
            }))
          )
        )

        case "personalRep" => Array(
          Map(
            "question" -> Messages("calc.customerType.question"),
            "answer" -> "Personal Representative"
          ),
          Map(
            "question" -> Messages("calc.annualExemptAmount.question"),
            "answer" -> ("&pound;" + (summary.otherPropertiesModel.otherProperties match {
              case "Yes" => summary.annualExemptAmountModel.get.annualExemptAmount.setScale(2).toString
              case "No" => "11100.00"
            }))
          )
        )
      }
    )
  }

  def acquisitionDetails(result: CalculationResultModel, summary: SummaryModel) = {
    summaryPageSection("purchaseDetails", Messages("calc.summary.purchase.details.title"),
      summary.acquisitionDateModel.hasAcquisitionDate match {
        case "Yes" => Array(
          Map(
            "question" -> Messages("calc.acquisitionDate.questionTwo"),
            "answer" -> Dates.datePageFormat.format(Dates.constructDate(summary.acquisitionDateModel.day.get, summary.acquisitionDateModel.month.get, summary.acquisitionDateModel.year.get))
          ),
          Map(
            "question" -> Messages("calc.acquisitionValue.question"),
            "answer" -> ("&pound;" + summary.acquisitionValueModel.acquisitionValueAmt.setScale(2).toString)
          ),
          Map(
            "question" -> Messages("calc.acquisitionCosts.question"),
            "answer" -> ("&pound;" + (summary.acquisitionCostsModel.acquisitionCostsAmt match {
              case Some(data) => data.setScale(2).toString
              case None => "0.00"
            }))
          )
        )
        case "No" => Array(
          Map(
            "question" -> Messages("calc.acquisitionValue.question"),
            "answer" -> ("&pound;" + summary.acquisitionValueModel.acquisitionValueAmt.setScale(2).toString)
          ),
          Map(
            "question" -> Messages("calc.acquisitionCosts.question"),
            "answer" -> ("&pound;" + (summary.acquisitionCostsModel.acquisitionCostsAmt match {
              case Some(data) => data.setScale(2).toString
              case None => "0.00"
            }))
          )
        )
      }
    )
  }

  def propertyDetails(result: CalculationResultModel, summary: SummaryModel) = {
    summaryPageSection("propertyDetails", Messages("calc.summary.property.details.title"),
      summary.improvementsModel.improvementsAmt match {
        case Some(data) => Array(
          Map(
            "question" -> Messages("calc.improvements.question"),
            "answer" -> summary.improvementsModel.isClaimingImprovements
          ),
          Map(
            "question" -> Messages("calc.improvements.questionTwo"),
            "answer" -> ("&pound;" + summary.improvementsModel.improvementsAmt.get.setScale(2))
          )
        )
        case None => Array(
          Map(
            "question" -> Messages("calc.improvements.question"),
            "answer" -> summary.improvementsModel.isClaimingImprovements)
        )
      }
    )
  }

  def saleDetails(result: CalculationResultModel, summary: SummaryModel) = {
    summaryPageSection("saleDetails", Messages("calc.summary.sale.details.title"),
      Array(
        Map(
          "question" -> Messages("calc.disposalDate.question"),
          "answer" -> Dates.datePageFormat.format(Dates.constructDate(summary.disposalDateModel.day, summary.disposalDateModel.month, summary.disposalDateModel.year))
        ),
        Map(
          "question" -> Messages("calc.disposalValue.question"),
          "answer" -> ("&pound;" + summary.disposalValueModel.disposalValue.setScale(2).toString)
        ),
        Map(
          "question" -> Messages("calc.disposalCosts.question"),
          "answer" -> ("&pound;" + (summary.disposalCostsModel.disposalCosts match {
            case Some(data) => data.setScale(2).toString
            case None => "0.00"
          }))
        )
      )
    )
  }

  def deductions(result: CalculationResultModel, summary: SummaryModel) = {
    summaryPageSection("deductions", Messages("calc.summary.deductions.title"),
      summary.calculationElectionModel.calculationType match {
        case "flat" => Array(
          Map(
            "question" -> Messages("calc.entrepreneursRelief.question"),
            "answer" -> summary.entrepreneursReliefModel.entReliefClaimed
          ),
          Map(
            "question" -> Messages("calc.allowableLosses.question.two"),
            "answer" -> ("&pound;" + (summary.allowableLossesModel.allowableLossesAmt match {
              case Some(data) => data.setScale(2).toString
              case None => "0.00"
            }))
          ),
          Map(
            "question" -> Messages("calc.otherReliefs.question"),
            "answer" -> ("&pound;" + (summary.otherReliefsModelFlat.otherReliefs match {
              case Some(data) => data.setScale(2).toString
              case None => "0.00"
            }))
          )
        )
        case "time" => Array(
          Map(
            "question" -> Messages("calc.entrepreneursRelief.question"),
            "answer" -> summary.entrepreneursReliefModel.entReliefClaimed
          ),
          Map(
            "question" -> Messages("calc.allowableLosses.question.two"),
            "answer" -> ("&pound;" + (summary.allowableLossesModel.allowableLossesAmt match {
              case Some(data) => data.setScale(2).toString
              case None => "0.00"
            }))
          ),
          Map(
            "question" -> Messages("calc.otherReliefs.question"),
            "answer" -> ("&pound;" + (summary.otherReliefsModelTA.otherReliefs match {
              case Some(data) => data.setScale(2).toString
              case None => "0.00"
            }))
          )
        )
      }
    )
  }

}
