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

object AcquisitionValueForm {

  val acquisitionValueForm = Form(
    mapping(
      "acquisitionValue" -> bigDecimal
        .verifying(Messages("calc.acquisitionValue.errorNegative"), acquisitionValue => isPositive(acquisitionValue))
        .verifying(Messages("calc.acquisitionValue.errorDecimalPlaces"), acquisitionValue => isMaxTwoDecimalPlaces(acquisitionValue))
    )(AcquisitionValueModel.apply)(AcquisitionValueModel.unapply)
  )
}
