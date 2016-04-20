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

package connectors

import common.Validation._
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class ValidationSpec extends UnitSpec {

  //############# Tests for isValidDate function ##########################################
  "calling common.Validation.isValidDate(day, month, year) " should {

    "with no day value supplied 'isValidDate(0,1,2016)' return false" in {
      isValidDate(0,1,2016) shouldBe false
    }

    "with no month value supplied 'isValidDate(1,0,2016)' return false" in {
      isValidDate(1,0,2016) shouldBe false
    }

    "with no year value supplied 'isValidDate(0,1,2016)' return false" in {
      isValidDate(1,1,0) shouldBe false
    }

    "with invalid date 'isValidDate(32,1,2016)' return false" in {
      isValidDate(32,1,2016) shouldBe false
    }

    "with invalid leap year date 'isValidDate(29,2,2017)' return false" in {
      isValidDate(29,2,2017) shouldBe false
    }

    "with valid leap year date 'isValidDate(29,2,2016)' return true" in {
      isValidDate(29,2,2016) shouldBe true
    }

    "with valid  date 'isValidDate(12,09,1990)' return true" in {
      isValidDate(12,9,1990) shouldBe true
    }
  }


  //############# Tests for isPositive function ##########################################
  "calling common.Validation.isPositive(amount) " should {

    "with a positive numeric supplied isPositive(1) return true" in {
      isPositive(1) shouldBe true
    }

    "with Zero supplied return true" in {
      isPositive(0) shouldBe true
    }

    "with Negative supplied return false" in {
      isPositive(-1) shouldBe false
    }
  }


  //############# Tests for isMaxTwoDecimalPlaces ##########################################
  "calling common.Validation.isMaxTwoDecimalPlaces(amount) " should {

    "with no decimals supplied isMaxTwoDecimalPlaces(1) return true" in {
      isMaxTwoDecimalPlaces(1) shouldBe true
    }

    "with one decimal place supplied isMaxTwoDecimalPlaces(1.1) return true" in {
      isMaxTwoDecimalPlaces(1.1) shouldBe true
    }

    "with two decimal places supplied isMaxTwoDecimalPlaces(1.11) return true" in {
      isMaxTwoDecimalPlaces(1.11) shouldBe true
    }

    "with three decimal places supplied isMaxTwoDecimalPlaces(1.111) return false" in {
      isMaxTwoDecimalPlaces(1.111) shouldBe false
    }
  }
}
