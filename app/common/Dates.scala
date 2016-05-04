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
import java.util.Date

object Dates {

  val sf = new SimpleDateFormat("dd/MM/yyyy")
  val datePageFormat = new SimpleDateFormat("dd MMMM yyyy")
  val taxStartDate = sf.parse("05/04/2015")

  def constructDate (day: Int, month: Int, year: Int): Date = {

//    sf.parse(s"${day match {
//      case day if day < 10 => "0" + day
//      case _ => day
//    }
//    }/${month match {
//      case month if month < 10 => "0" + month
//      case _ => month
//    }
//    }/$year")
    sf.parse(s"$day/$month/$year")
  }

  def dateAfterStart (day: Int, month: Int, year: Int): Boolean = {
    constructDate(day, month, year).after(taxStartDate)
  }
}

