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
import connectors.CalculatorConnector
import controllers.{routes, CalculationController}
import models.SummaryModel
import play.api.i18n.Messages
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object CalculationElectionConstructor extends CalculationElectionConstructor {
  val calcConnector = CalculatorConnector
}

trait CalculationElectionConstructor {

  val calcConnector: CalculatorConnector

  def generateElection(summary: SummaryModel, hc: HeaderCarrier) = {
    summary.acquisitionDateModel.hasAcquisitionDate match {
      case "Yes" if Dates.dateAfterStart(summary.acquisitionDateModel.day.get, summary.acquisitionDateModel.month.get, summary.acquisitionDateModel.year.get) => {
        Seq("flat"->(resultFlat(summary, hc), Messages("calc.calculationElection.message.flat"), None, routes.CalculationController.otherReliefs().toString()))
      }
      case "Yes" if !Dates.dateAfterStart(summary.acquisitionDateModel.day.get, summary.acquisitionDateModel.month.get, summary.acquisitionDateModel.year.get) => {
        Seq(
          "flat" ->(resultFlat(summary, hc), Messages("calc.calculationElection.message.flat"),
            None, routes.CalculationController.otherReliefs().toString()),
          "time" ->(resultTime(summary, hc), Messages("calc.calculationElection.message.time"),
            Some(Messages("calc.calculationElection.message.timeDate")), routes.CalculationController.otherReliefsTA().toString()))
      }
      case "No" => {
        Seq("flat"->(resultFlat(summary, hc), Messages("calc.calculationElection.message.flat"), None, routes.CalculationController.otherReliefs().toString()))
      }
    }
  }

  def resultFlat (summary: SummaryModel, hc: HeaderCarrier) = {
    Await.result(calcConnector.calculateFlat(summary)(hc), Duration("5s")).get.taxOwed.setScale(2).toString()
  }

  def resultTime(summary: SummaryModel, hc: HeaderCarrier) = {
    Await.result(calcConnector.calculateTA(summary)(hc), Duration("5s")).get.taxOwed.setScale(2).toString()
  }
}
