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

package common

import java.text.SimpleDateFormat

object Validation {

  def isValidDate(day:Int,month:Int,year:Int): Boolean = {
    try {
      val fmt = new SimpleDateFormat("dd/MM/yyyy")
      fmt.setLenient(false)
      fmt.parse(s"${day}/${month}/${year}")
      true
    } catch {
      case e: Exception => false
    }
  }

  def isMaxTwoDecimalPlaces(amount: BigDecimal): Boolean = {
    amount match {
      case amount if amount.scale <= 2 => true
      case _ => false
    }
  }

  def hasNoDecimalPlaces (amount: BigDecimal): Boolean = {
    amount match {
      case value if amount.scale <= 0 => true
      case _ => false
    }
  }

  def isPositive(amount: BigDecimal): Boolean = {
    amount match {
      case amount if amount < 0 => false
      case _ => true
    }
  }
}
