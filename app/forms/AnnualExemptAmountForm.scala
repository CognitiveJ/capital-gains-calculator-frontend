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

package forms

import play.api.data._
import play.api.data.Forms._
import models._
import play.api.i18n.Messages
import common.Validation._
import uk.gov.hmrc.play.http.HeaderCarrier
import controllers.CalculationController._

object AnnualExemptAmountForm {

  val maxAEA = 11100
  val maxNonVulnerableTrusteeAEA = 5550

  def validateMaximum(isAllowedMaxAEA: Boolean, aea: BigDecimal): Boolean = {
    isAllowedMaxAEA match {
      case true => if (aea > maxAEA) false else true
      case false => if (aea > maxNonVulnerableTrusteeAEA) false else true
    }
  }

  def errorMaxMessage(isAllowedMaxAEA: Boolean): String = {
    isAllowedMaxAEA match {
      case true => Messages("calc.annualExemptAmount.errorMax") + maxAEA
      case false => Messages("calc.annualExemptAmount.errorMax") + maxNonVulnerableTrusteeAEA
    }
  }

  def annualExemptAmountForm (isAllowedMaxAEA: Boolean): Form[AnnualExemptAmountModel] = Form(
    mapping(
      "annualExemptAmount" -> bigDecimal
        .verifying(errorMaxMessage(isAllowedMaxAEA), annualExemptAmount => validateMaximum(isAllowedMaxAEA, annualExemptAmount))
        .verifying(Messages("calc.annualExemptAmount.errorNegative"), annualExemptAmount => isPositive(annualExemptAmount))
        .verifying(Messages("calc.annualExemptAmount.errorDecimalPlaces"), annualExemptAmount => isMaxTwoDecimalPlaces(annualExemptAmount))
    )(AnnualExemptAmountModel.apply)(AnnualExemptAmountModel.unapply)
  )
}
