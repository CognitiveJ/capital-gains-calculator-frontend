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

import controllers.routes
import models.{SummaryDataItemModel, RebasedValueModel, SummaryModel, CalculationResultModel}
import org.apache.commons.lang3.text.WordUtils
import play.api.i18n.Messages
import views.html.helpers._
import common._

object SummaryConstructor {

  def lossOrGainWording(gain: BigDecimal): String = {
    if (gain < 0) {
      Messages("calc.summary.calculation.details.totalLoss")
    } else {
      Messages("calc.summary.calculation.details.totalGain")
    }
  }

  //scalastyle:off

  def calculationDetails(result: CalculationResultModel, summary: SummaryModel) = summaryPageSection("calcDetails", Messages("calc.summary.calculation.details.title"),
    result.upperTaxGain match {
      case Some(data) => Array(
        SummaryDataItemModel(
          Messages("calc.summary.calculation.details.calculationElection"),
          summary.calculationElectionModel.calculationType match {
            case "flat" => Messages("calc.summary.calculation.details.flatCalculation")
            case "time" => Messages("calc.summary.calculation.details.timeCalculation")
            case "rebased" => Messages("calc.summary.calculation.details.rebasedCalculation")
          },
          Some(routes.CalculationController.calculationElection().toString())
        ),
        SummaryDataItemModel(
          lossOrGainWording(result.totalGain),
          "&pound;" + result.totalGain.abs.setScale(2).toString,
          None
        ),
        SummaryDataItemModel(
          Messages("calc.summary.calculation.details.taxableGain"),
          "&pound;" + result.taxableGain.setScale(2).toString,
          None
        ),
        SummaryDataItemModel(
          Messages("calc.summary.calculation.details.taxRate"),
          "&pound;" + result.baseTaxGain.setScale(2).toString + " at " + result.baseTaxRate + "%",
          None
        ),
        SummaryDataItemModel(
          "",
          "&pound;" + result.upperTaxGain.get.setScale(2).toString + " at " + result.upperTaxRate.get.toString + "%",
          None
        )
      )
      case None => Array(
        SummaryDataItemModel(
          Messages("calc.summary.calculation.details.calculationElection"),
          summary.calculationElectionModel.calculationType match {
            case "flat" => Messages("calc.summary.calculation.details.flatCalculation")
            case "time" => Messages("calc.summary.calculation.details.timeCalculation")
            case "rebased" => Messages("calc.summary.calculation.details.rebasedCalculation")
          },
          Some(routes.CalculationController.calculationElection().toString())
        ),
        SummaryDataItemModel(
          lossOrGainWording(result.totalGain),
          "&pound;" + result.totalGain.abs.setScale(2).toString,
          None
        ),
        SummaryDataItemModel(
          Messages("calc.summary.calculation.details.taxableGain"),
          "&pound;" + (result.baseTaxGain + result.upperTaxGain.getOrElse(0)).setScale(2).toString,
          None
        ),
        SummaryDataItemModel(
          Messages("calc.summary.calculation.details.taxRate"),
          result.baseTaxRate + "%",
          None
        )
      )
    }
  )

  def personalDetails(result: CalculationResultModel, summary: SummaryModel) = {
    summaryPageSection("personalDetails", Messages("calc.summary.personal.details.title"),
      summary.customerTypeModel.customerType match {
        case "trustee" => summary.otherPropertiesModel.otherProperties match {
          case "Yes" => Array(
            SummaryDataItemModel(
              Messages("calc.customerType.question"),
              WordUtils.capitalize(summary.customerTypeModel.customerType),
              Some(routes.CalculationController.customerType().toString())
            ),
            SummaryDataItemModel(
              Messages("calc.disabledTrustee.question"),
              summary.disabledTrusteeModel.get.isVulnerable,
              Some(routes.CalculationController.disabledTrustee().toString())
            ),
            SummaryDataItemModel(
              Messages("calc.otherProperties.questionTwo"),
              "&pound;" + summary.otherPropertiesModel.otherPropertiesAmt.get.setScale(2).toString,
              Some(routes.CalculationController.otherProperties().toString())
            ),
            SummaryDataItemModel(
              Messages("calc.annualExemptAmount.question"),
              "&pound;" + summary.annualExemptAmountModel.get.annualExemptAmount.setScale(2).toString,
              Some(routes.CalculationController.annualExemptAmount().toString())
            )
          )
          case "No" => Array(
            SummaryDataItemModel(
              Messages("calc.customerType.question"),
              WordUtils.capitalize(summary.customerTypeModel.customerType),
              Some(routes.CalculationController.calculationElection().toString())
            ),
            SummaryDataItemModel(
              Messages("calc.disabledTrustee.question"),
              summary.disabledTrusteeModel.get.isVulnerable,
              Some(routes.CalculationController.disabledTrustee().toString())
            ),
            SummaryDataItemModel(
              Messages("calc.otherProperties.question"),
              summary.otherPropertiesModel.otherProperties.toString,
              Some(routes.CalculationController.otherProperties().toString())
            )
          )
        }
        case "individual" => summary.otherPropertiesModel.otherProperties match {
          case "Yes" => Array(
            SummaryDataItemModel(
              Messages("calc.customerType.question"),
              WordUtils.capitalize(summary.customerTypeModel.customerType),
              Some(routes.CalculationController.customerType().toString())
            ),
            SummaryDataItemModel(
              Messages("calc.currentIncome.question"),
              "&pound;" + summary.currentIncomeModel.get.currentIncome.setScale(2),
              Some(routes.CalculationController.currentIncome().toString())
            ),
            SummaryDataItemModel(
              Messages("calc.personalAllowance.question"),
              "&pound;" + summary.personalAllowanceModel.get.personalAllowanceAmt.setScale(2),
              Some(routes.CalculationController.personalAllowance().toString())
            ),
            SummaryDataItemModel(
              Messages("calc.otherProperties.questionTwo"),
              "&pound;" + summary.otherPropertiesModel.otherPropertiesAmt.get.setScale(2).toString,
              Some(routes.CalculationController.otherProperties().toString())
            ),
            SummaryDataItemModel(
              Messages("calc.annualExemptAmount.question"),
              "&pound;" + summary.annualExemptAmountModel.get.annualExemptAmount.setScale(2).toString,
              Some(routes.CalculationController.annualExemptAmount().toString())
            )
          )
          case "No" => Array(
            SummaryDataItemModel(
              Messages("calc.customerType.question"),
              WordUtils.capitalize(summary.customerTypeModel.customerType),
              Some(routes.CalculationController.customerType().toString())
            ),
            SummaryDataItemModel(
              Messages("calc.currentIncome.question"),
              "&pound;" + summary.currentIncomeModel.get.currentIncome.setScale(2),
              Some(routes.CalculationController.currentIncome().toString())
            ),
            SummaryDataItemModel(
              Messages("calc.personalAllowance.question"),
              "&pound;" + summary.personalAllowanceModel.get.personalAllowanceAmt.setScale(2),
              Some(routes.CalculationController.personalAllowance().toString())
            ),
            SummaryDataItemModel(
              Messages("calc.otherProperties.question"),
              summary.otherPropertiesModel.otherProperties.toString,
              Some(routes.CalculationController.otherProperties().toString())
            )
          )
        }
        case "personalRep" => summary.otherPropertiesModel.otherProperties match {
          case "Yes" => Array(
            SummaryDataItemModel(
              Messages("calc.customerType.question"),
              "Personal Representative",
              Some(routes.CalculationController.customerType().toString())
            ),
            SummaryDataItemModel(
              Messages("calc.otherProperties.questionTwo"),
              "&pound;" + summary.otherPropertiesModel.otherPropertiesAmt.get.setScale(2).toString,
              Some(routes.CalculationController.otherProperties().toString())
            ),
            SummaryDataItemModel(
              Messages("calc.annualExemptAmount.question"),
              "&pound;" + summary.annualExemptAmountModel.get.annualExemptAmount.setScale(2).toString,
              Some(routes.CalculationController.annualExemptAmount().toString())
            )
          )
          case "No" => Array(
            SummaryDataItemModel(
              Messages("calc.customerType.question"),
              "Personal Representative",
              Some(routes.CalculationController.customerType().toString())
            ),
            SummaryDataItemModel(
              Messages("calc.otherProperties.question"),
              summary.otherPropertiesModel.otherProperties.toString,
              Some(routes.CalculationController.otherProperties().toString())
            )
          )
        }
      }
    )
  }

  def acquisitionDetails(result: CalculationResultModel, summary: SummaryModel) = {
    summaryPageSection("purchaseDetails", Messages("calc.summary.purchase.details.title"),
      summary.calculationElectionModel.calculationType match {
        case "rebased" => summary.acquisitionDateModel.hasAcquisitionDate match {
          case "Yes" => Array(
            SummaryDataItemModel(
              Messages("calc.acquisitionDate.questionTwo"),
              Dates.datePageFormat.format(Dates.constructDate(summary.acquisitionDateModel.day.get, summary.acquisitionDateModel.month.get, summary.acquisitionDateModel.year.get)),
              Some(routes.CalculationController.acquisitionDate().toString())
            ),
            SummaryDataItemModel(
              Messages("calc.rebasedValue.questionTwo"),
              "&pound;" + summary.rebasedValueModel.get.rebasedValueAmt.get.setScale(2).toString,
              Some(routes.CalculationController.rebasedValue().toString())
            ),
            SummaryDataItemModel(
              Messages("calc.rebasedCosts.questionTwo"),
              "&pound;" + (summary.rebasedCostsModel.get.hasRebasedCosts match {
                case "Yes" => summary.rebasedCostsModel.get.rebasedCosts.get.setScale(2).toString
                case "No" => "0.00"
              }),
              Some(routes.CalculationController.rebasedCosts().toString())
            )
          )
          case "No" => Array(
            SummaryDataItemModel(
              Messages("calc.rebasedValue.questionTwo"),
              "&pound;" + summary.rebasedValueModel.get.rebasedValueAmt.get.setScale(2).toString,
              Some(routes.CalculationController.rebasedValue().toString())
            ),
            SummaryDataItemModel(
              Messages("calc.rebasedCosts.questionTwo"),
              "&pound;" + (summary.rebasedCostsModel.get.hasRebasedCosts match {
                case "Yes" => summary.rebasedCostsModel.get.rebasedCosts.get.setScale(2).toString
                case "No" => "0.00"
              }),
              Some(routes.CalculationController.rebasedCosts().toString())
            )
          )
        }
        case _ => summary.acquisitionDateModel.hasAcquisitionDate match {
          case "Yes" => Array(
            SummaryDataItemModel(
              Messages("calc.acquisitionDate.questionTwo"),
              Dates.datePageFormat.format(Dates.constructDate(summary.acquisitionDateModel.day.get, summary.acquisitionDateModel.month.get, summary.acquisitionDateModel.year.get)),
              Some(routes.CalculationController.acquisitionDate().toString())
            ),
            SummaryDataItemModel(
              Messages("calc.acquisitionValue.question"),
              "&pound;" + summary.acquisitionValueModel.acquisitionValueAmt.setScale(2).toString,
              Some(routes.CalculationController.acquisitionValue().toString())
            ),
            SummaryDataItemModel(
              Messages("calc.acquisitionCosts.question"),
              "&pound;" + (summary.acquisitionCostsModel.acquisitionCostsAmt match {
                case Some(data) => data.setScale(2).toString
                case None => "0.00"
              }),
              Some(routes.CalculationController.acquisitionCosts().toString())
            )
          )
          case "No" => Array(
            SummaryDataItemModel(
              Messages("calc.acquisitionValue.question"),
              "&pound;" + summary.acquisitionValueModel.acquisitionValueAmt.setScale(2).toString,
              Some(routes.CalculationController.acquisitionValue().toString())
            ),
            SummaryDataItemModel(
              Messages("calc.acquisitionCosts.question"),
              "&pound;" + (summary.acquisitionCostsModel.acquisitionCostsAmt match {
                case Some(data) => data.setScale(2).toString
                case None => "0.00"
              }),
              Some(routes.CalculationController.acquisitionCosts().toString())
            )
          )
        }
      }

    )
  }

  def propertyDetails(result: CalculationResultModel, summary: SummaryModel) = {
    summaryPageSection("propertyDetails", Messages("calc.summary.property.details.title"),
      summary.improvementsModel.isClaimingImprovements match {
        case "Yes" => summary.calculationElectionModel.calculationType match {
          case "rebased" => Array(
            SummaryDataItemModel(
              Messages("calc.improvements.question"),
              summary.improvementsModel.isClaimingImprovements,
              Some(routes.CalculationController.improvements().toString())
            ),
            SummaryDataItemModel(
              Messages("calc.improvements.questionThree"),
              "&pound;" + summary.improvementsModel.improvementsAmt.get.setScale(2),
              Some(routes.CalculationController.improvements().toString())
            ),
            SummaryDataItemModel(
              Messages("calc.improvements.questionFour"),
              "&pound;" + summary.improvementsModel.improvementsAmtAfter.getOrElse(BigDecimal(0)).setScale(2),
              Some(routes.CalculationController.improvements().toString())
            )
          )
          case _ => Array(
            SummaryDataItemModel(
              Messages("calc.improvements.question"),
              summary.improvementsModel.isClaimingImprovements,
              Some(routes.CalculationController.improvements().toString())
            ),
            SummaryDataItemModel(
              Messages("calc.improvements.questionTwo"),
              "&pound;" + {summary.improvementsModel.improvementsAmt.getOrElse(BigDecimal(0))
                .+(summary.improvementsModel.improvementsAmtAfter.getOrElse(BigDecimal(0))).setScale(2)
              },
              Some(routes.CalculationController.improvements().toString())
            )
          )
        }

        case "No" => Array(
          SummaryDataItemModel(
            Messages("calc.improvements.question"),
            summary.improvementsModel.isClaimingImprovements,
            Some(routes.CalculationController.improvements().toString())
          )
        )
      }
    )
  }

  def saleDetails(result: CalculationResultModel, summary: SummaryModel) = {
    summaryPageSection("saleDetails", Messages("calc.summary.sale.details.title"),
      Array(
        SummaryDataItemModel(
          Messages("calc.disposalDate.question"),
          Dates.datePageFormat.format(Dates.constructDate(summary.disposalDateModel.day, summary.disposalDateModel.month, summary.disposalDateModel.year)),
          Some(routes.CalculationController.disposalDate().toString())
        ),
        SummaryDataItemModel(
          Messages("calc.disposalValue.question"),
          "&pound;" + summary.disposalValueModel.disposalValue.setScale(2).toString,
          Some(routes.CalculationController.disposalValue().toString())
        ),
        SummaryDataItemModel(
          Messages("calc.disposalCosts.question"),
          "&pound;" + (summary.disposalCostsModel.disposalCosts match {
            case Some(data) => data.setScale(2).toString
            case None => "0.00"
          }),
          Some(routes.CalculationController.disposalCosts().toString())
        )
      )
    )
  }

  def deductions(result: CalculationResultModel, summary: SummaryModel) = {
    summaryPageSection("deductions", Messages("calc.summary.deductions.title"),
      summary.calculationElectionModel.calculationType match {
        case "flat" => Array(
          SummaryDataItemModel(
            Messages("calc.entrepreneursRelief.question"),
            summary.entrepreneursReliefModel.entReliefClaimed,
            Some(routes.CalculationController.entrepreneursRelief().toString())
          ),
          SummaryDataItemModel(
            Messages("calc.allowableLosses.question.two"),
            "&pound;" + (summary.allowableLossesModel.isClaimingAllowableLosses match {
              case "Yes" => summary.allowableLossesModel.allowableLossesAmt.get.setScale(2).toString
              case "No" => "0.00"
            }),
            Some(routes.CalculationController.allowableLosses().toString())
          ),
          SummaryDataItemModel(
            Messages("calc.otherReliefs.question"),
            "&pound;" + (summary.otherReliefsModelFlat.otherReliefs match {
              case Some(data) => data.setScale(2).toString
              case None => "0.00"
            }),
            Some(routes.CalculationController.otherReliefs().toString())
          )
        )
        case "time" => Array(
          SummaryDataItemModel(
            Messages("calc.entrepreneursRelief.question"),
            summary.entrepreneursReliefModel.entReliefClaimed,
            Some(routes.CalculationController.entrepreneursRelief().toString())
          ),
          SummaryDataItemModel(
            Messages("calc.allowableLosses.question.two"),
            "&pound;" + (summary.allowableLossesModel.isClaimingAllowableLosses match {
              case "Yes" => summary.allowableLossesModel.allowableLossesAmt.get.setScale(2).toString
              case "No" => "0.00"
            }),
            Some(routes.CalculationController.allowableLosses().toString())
          ),
          SummaryDataItemModel(
            Messages("calc.otherReliefs.question"),
            "&pound;" + (summary.otherReliefsModelTA.otherReliefs match {
              case Some(data) => data.setScale(2).toString
              case None => "0.00"
            }),
            Some(routes.CalculationController.otherReliefsTA().toString())
          )
        )
        case "rebased" => Array(
          SummaryDataItemModel(
            Messages("calc.entrepreneursRelief.question"),
            summary.entrepreneursReliefModel.entReliefClaimed,
            Some(routes.CalculationController.entrepreneursRelief().toString())
          ),
          SummaryDataItemModel(
            Messages("calc.allowableLosses.question.two"),
            "&pound;" + (summary.allowableLossesModel.isClaimingAllowableLosses match {
              case "Yes" => summary.allowableLossesModel.allowableLossesAmt.get.setScale(2).toString
              case "No" => "0.00"
            }),
            Some(routes.CalculationController.allowableLosses().toString())
          ),
          SummaryDataItemModel(
            Messages("calc.otherReliefs.question"),
            "&pound;" + (summary.otherReliefsModelRebased.otherReliefs match {
              case Some(data) => data.setScale(2).toString
              case None => "0.00"
            }),
            Some(routes.CalculationController.otherReliefsRebased().toString())
          )
        )
      }
    )
  }

  def gainMessage (result: CalculationResultModel) = {
    if (result.totalGain >= 0) Messages("calc.otherReliefs.totalGain")
    else Messages("calc.otherReliefs.totalLoss")
  }

  def setPositive (result: CalculationResultModel) = {
    BigDecimal(Math.abs(result.totalGain.toDouble)).setScale(2).toString()
  }
}
