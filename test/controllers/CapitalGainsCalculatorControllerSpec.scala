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

package controllers

import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class CapitalGainsCalculatorControllerSpec extends UnitSpec with WithFakeApplication {

  val s = "Action(parser=BodyParser(anyContent))"

  class fakeRequestTo(url : String) {
    val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/" + url)
  }

  "CapitalGainsCalculatorController methods " should {

    "return 200 from customer-type" in new fakeRequestTo("customer-type") {
      val result = CapitalGainsCalculatorController.customerType(fakeRequest)
      status(result) shouldBe 200
    }

    "return HTML from customer-type" in new fakeRequestTo("customer-type"){
      val result = CapitalGainsCalculatorController.customerType(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }

    "be Action(parser=BodyParser(anyContent)) for disabledTrustee" in {
      val result = CapitalGainsCalculatorController.disabledTrustee.toString()
      result shouldBe s
    }
    "return 204 from disabled-trustee" in new fakeRequestTo("disabled-trustee") {
      val result = CapitalGainsCalculatorController.disabledTrustee(fakeRequest)
      status(result) shouldBe 204
    }

    "return HTML from disabled-trustee" in new fakeRequestTo("disabled-trustee"){
      val result = CapitalGainsCalculatorController.disabledTrustee(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }

    "be Action(parser=BodyParser(anyContent)) for currentIncome" in {
      val result = CapitalGainsCalculatorController.currentIncome.toString()
      result shouldBe s
    }
    "be Action(parser=BodyParser(anyContent)) for personalAllowance" in {
      val result = CapitalGainsCalculatorController.personalAllowance.toString()
      result shouldBe s
    }

    //############## Other Properties tests ######################
    "return 200 from other-properties" in new fakeRequestTo("other-properties") {
      val result = CapitalGainsCalculatorController.otherProperties(fakeRequest)
      status(result) shouldBe 200
    }

    "return HTML from other-properties" in new fakeRequestTo("other-properties"){
      val result = CapitalGainsCalculatorController.otherProperties(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }

    //############## Annual Exempt Amount tests ######################
    "return 200 for annual-exempt-amount" in new fakeRequestTo("annual-exempt-amount") {
      val result = CapitalGainsCalculatorController.annualExemptAmount(fakeRequest)
      status(result) shouldBe 200
    }

    "return HTML for annual-exempt-amount" in new fakeRequestTo("annual-exempt-amount"){
      val result = CapitalGainsCalculatorController.annualExemptAmount(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }

    //############## Acquisition Value tests ######################
    "be Action(parser=BodyParser(anyContent)) for acquisitionValue" in {
      val result = CapitalGainsCalculatorController.acquisitionValue.toString()
      result shouldBe s
    }
    "be Action(parser=BodyParser(anyContent)) for improvements" in {
      val result = CapitalGainsCalculatorController.improvements.toString()
      result shouldBe s
    }
    "be Action(parser=BodyParser(anyContent)) for disposalDate" in {
      val result = CapitalGainsCalculatorController.disposalDate.toString()
      result shouldBe s
    }
    "return 200 from disposal-date" in new fakeRequestTo("disposal-date") {
      val result = CapitalGainsCalculatorController.disposalDate(fakeRequest)
      status(result) shouldBe 200
    }

    "return HTML from disposal-date" in new fakeRequestTo("disposal-date"){
      val result = CapitalGainsCalculatorController.disposalDate(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }

    "be Action(parser=BodyParser(anyContent)) for disposalValue" in {
      val result = CapitalGainsCalculatorController.disposalValue.toString()
      result shouldBe s
    }
    "be Action(parser=BodyParser(anyContent)) for acquisitionCosts" in {
      val result = CapitalGainsCalculatorController.acquisitionCosts.toString()
      result shouldBe s
    }
    "be Action(parser=BodyParser(anyContent)) for disposalCosts" in {
      val result = CapitalGainsCalculatorController.disposalCosts.toString()
      result shouldBe s
    }
    "be Action(parser=BodyParser(anyContent)) for entrepreneursRelief" in {
      val result = CapitalGainsCalculatorController.entrepreneursRelief.toString()
      result shouldBe s
    }
    "be Action(parser=BodyParser(anyContent)) for allowableLosses" in {
      val result = CapitalGainsCalculatorController.allowableLosses.toString()
      result shouldBe s
    }
    "be Action(parser=BodyParser(anyContent)) for otherReliefs" in {
      val result = CapitalGainsCalculatorController.otherReliefs.toString()
      result shouldBe s
    }
  }
}