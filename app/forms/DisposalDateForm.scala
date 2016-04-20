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
import common.Validation._
import play.api.i18n.Messages

object DisposalDateForm {

  val disposalDateForm = Form(
    mapping(
      "disposalDate.day" -> number
        .verifying(Messages("calc.common.date.error.day.lessThan1"), day => day > 0)
        .verifying(Messages("calc.common.date.error.day.greaterThan31"), day => day < 32),
      "disposalDate.month" -> number
        .verifying(Messages("calc.common.date.error.month.lessThan1"), month => month > 0)
        .verifying(Messages("calc.common.date.error.month.greaterThan12"), month => month < 13),
      "disposalDate.year" -> number
    )(DisposalDateModel.apply)(DisposalDateModel.unapply) verifying(Messages("calc.common.date.error.invalidDate"), fields =>
      isValidDate(fields.day, fields.month, fields.year)))
}
