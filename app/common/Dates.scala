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
import java.util.{Calendar, Date}

object Dates {

  val sf = new SimpleDateFormat("dd/MM/yyyy")
  val datePageFormat = new SimpleDateFormat("dd MMMM yyyy")
  val taxStartDate = sf.parse("05/04/2015")
  val taxStartDatePlus18Months = sf.parse("05/10/2016")

  def constructDate (day: Int, month: Int, year: Int): Date = {
    sf.parse(s"$day/$month/$year")
  }

  def dateAfterStart (day: Int, month: Int, year: Int): Boolean = {
    constructDate(day, month, year).after(taxStartDate)
  }

  def dateAfterStart (date: Date): Boolean = {
    date.after(taxStartDate)
  }

  def dateAfterOctober (date: Date): Boolean = {
    date.after(taxStartDatePlus18Months)
  }

  def dateMinusMonths(date: Option[Date], months: Int): String = {
    date match {
      case Some(date) =>
        val cal = Calendar.getInstance()
        cal.setTime(date)
        cal.add(Calendar.MONTH, months * -1)
        new SimpleDateFormat("d MMMMM yyyy").format(cal.getTime)
      case _ => ""
    }
  }
}

