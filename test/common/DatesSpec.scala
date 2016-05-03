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
import uk.gov.hmrc.play.test.UnitSpec

class DatesSpec extends UnitSpec {

  val sf = new SimpleDateFormat("dd/MM/yyyy")

  "Calling constructDate method" should {

    "return a valid date object with single digit inputs" in {
      Dates.constructDate(1, 2, 1990) shouldBe sf.parse("01/02/1990")
    }

    "return a valid date object with double digit inputs" in {
      Dates.constructDate(10, 11, 2016) shouldBe sf.parse("10/11/2016")
    }
  }

  "Calling dateAfterStart method" should {

    "return a true if date entered is after the 5th April 2015" in {
      Dates.dateAfterStart(6, 4, 2015) shouldBe true
    }

    "return a false if date entered is the 5th April 2015" in {
      Dates.dateAfterStart(5, 4, 2015) shouldBe false
    }

    "return a false if date entered is before the 5th April 2015" in {
      Dates.dateAfterStart(4, 4, 2015) shouldBe false
    }
  }
}
