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

import models.OtherPropertiesModel
import play.api.data.Forms._
import play.api.data._
import common.Validation._
import play.api.i18n.Messages

object OtherPropertiesForm {

  def validate(data: OtherPropertiesModel) = {
    data.otherProperties match {
      case "Yes" => data.otherPropertiesAmt.isDefined
      case "No" => true
    }
  }

  def validateMinimum(data: OtherPropertiesModel) = {
    data.otherProperties match {
      case "Yes" => isPositive(data.otherPropertiesAmt.getOrElse(0))
      case "No" => true
    }
  }

  def validateTwoDec(data: OtherPropertiesModel) = {
    data.otherProperties match {
      case "Yes" => isMaxTwoDecimalPlaces(data.otherPropertiesAmt.getOrElse(0))
      case "No" => true
    }
  }

  val otherPropertiesForm = Form (
    mapping(
      "otherProperties" -> nonEmptyText,
      "otherPropertiesAmt" -> optional(bigDecimal)
    )(OtherPropertiesModel.apply)(OtherPropertiesModel.unapply).verifying(Messages("calc.otherProperties.errorQuestion"),
      otherPropertiesForm => validate(otherPropertiesForm))
      .verifying(Messages("calc.otherProperties.errorNegative"),
        otherPropertiesForm => validateMinimum(otherPropertiesForm))
      .verifying(Messages("calc.otherProperties.errorDecimalPlaces"),
        otherPropertiesForm => validateTwoDec(otherPropertiesForm))
  )
}
