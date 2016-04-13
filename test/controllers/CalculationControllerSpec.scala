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
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Action}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import org.jsoup._

class CalculationControllerSpec extends UnitSpec with WithFakeApplication {

  val s = "Action(parser=BodyParser(anyContent))"

  class fakeRequestTo(url : String, controllerAction : Action[AnyContent]) {
    val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/" + url)
    val result = controllerAction(fakeRequest)
    val jsoupDoc = Jsoup.parse(bodyOf(result))
  }

  //################### Customer Type tests #######################
  "In CalculationController calling the .customerType action " should {

    object CustomerTypeTestDataItem extends fakeRequestTo("customer-type", CalculationController.customerType)

    "return a 200" in {
      status(CustomerTypeTestDataItem.result) shouldBe 200
    }

    "return some HTML that" should {

      "contain some text and use the character set utf-8" in {
        contentType(CustomerTypeTestDataItem.result) shouldBe Some("text/html")
        charset(CustomerTypeTestDataItem.result) shouldBe Some("utf-8")
      }

      "have the title 'Who owned the property?'" in {
        CustomerTypeTestDataItem.jsoupDoc.title shouldEqual Messages("calc.customerType.question")
      }

      "have the heading Calculate your tax (non-residents) " in {
        CustomerTypeTestDataItem.jsoupDoc.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
      }

      "display a radio button with the option `individual`" in {
        CustomerTypeTestDataItem.jsoupDoc.body.getElementById("customerType-individual").parent.text shouldEqual Messages("calc.customerType.individual")
      }

      "display a radio button with the option `trustee`" in {
        CustomerTypeTestDataItem.jsoupDoc.body.getElementById("customerType-trustee").parent.text shouldEqual Messages("calc.customerType.trustee")
      }

      "display a radio button with the option `personal representative`" in {
        CustomerTypeTestDataItem.jsoupDoc.body.getElementById("customerType-personalrep").parent.text shouldEqual Messages("calc.customerType.personalRep")
      }

      "display a 'Continue' button " in {
        CustomerTypeTestDataItem.jsoupDoc.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
      }
    }
  }

  //################### Disabled Trustee tests #######################
  "In CalculationController calling the .disabledTrustee action " should {

    object DisabledTrusteeTestDataItem extends fakeRequestTo("disabled-trustee", CalculationController.disabledTrustee)

    "return a 200" in {
      status(DisabledTrusteeTestDataItem.result) shouldBe 200
    }

    "return some HTML that" should {

      "contain some text and use the character set utf-8" in {
        contentType(DisabledTrusteeTestDataItem.result) shouldBe Some("text/html")
        charset(DisabledTrusteeTestDataItem.result) shouldBe Some("utf-8")
      }

      "have the title Are you a trustee for someone who’s vulnerable?" in {
        DisabledTrusteeTestDataItem.jsoupDoc.title shouldEqual Messages("calc.disabledTrustee.question")
      }

      "have the heading Calculate your tax (non-residents) " in {
        DisabledTrusteeTestDataItem.jsoupDoc.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
      }

      "display a radio button with the option 'Yes'" in {
        DisabledTrusteeTestDataItem.jsoupDoc.body.getElementById("isVulnerableYes").parent.text shouldEqual Messages("calc.base.yes")
      }

      "display a radio button with the option 'No'" in {
        DisabledTrusteeTestDataItem.jsoupDoc.body.getElementById("isVulnerableNo").parent.text shouldEqual Messages("calc.base.no")
      }

      "display a 'Continue' button " in {
        DisabledTrusteeTestDataItem.jsoupDoc.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
      }
    }

    //################### Current Income tests #######################
    "In CalculationController calling the .currentIncome action " should {

      "be Action(parser=BodyParser(anyContent)) for currentIncome" in {
        val result = CalculationController.currentIncome.toString()
        result shouldBe s
      }
    }

    //############## Personal Allowance tests ######################
    "In CalculationController calling the .personalAllowance action " should {

      object PersonalAllowanceTestDataItem extends fakeRequestTo("personal-allowance", CalculationController.personalAllowance)

      "return a 200" in {
        status(PersonalAllowanceTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          contentType(PersonalAllowanceTestDataItem.result) shouldBe Some("text/html")
          charset(PersonalAllowanceTestDataItem.result) shouldBe Some("utf-8")
        }
      }
    }

    //############## Other Properties tests ######################
    "In CalculationController calling the .otherProperties action " should {

      object OtherPropertiesTestDataItem extends fakeRequestTo("other-properties", CalculationController.otherProperties)

      "return a 200" in {
        status(OtherPropertiesTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          contentType(OtherPropertiesTestDataItem.result) shouldBe Some("text/html")
          charset(OtherPropertiesTestDataItem.result) shouldBe Some("utf-8")
        }

        "have the title 'Did you sell or give away any other properties in that tax year?'" in {
          OtherPropertiesTestDataItem.jsoupDoc.title shouldEqual Messages("calc.otherProperties.question")
        }

        "have the heading Calculate your tax (non-residents) " in {
          OtherPropertiesTestDataItem.jsoupDoc.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "display a radio button with the option `Yes`" in {
          OtherPropertiesTestDataItem.jsoupDoc.body.getElementById("otherPropertiesYes").parent.text shouldEqual Messages("calc.base.yes")
        }

        "display a radio button with the option `No`" in {
          OtherPropertiesTestDataItem.jsoupDoc.body.getElementById("otherPropertiesNo").parent.text shouldEqual Messages("calc.base.no")
        }

        "display a 'Continue' button " in {
          OtherPropertiesTestDataItem.jsoupDoc.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
        }
      }
    }

//
//    //############## Annual Exempt Amount tests ######################
//    "when calling the annualExemptAmount action" should {
//
//      "return 200" in new fakeRequestTo("allowance") {
//        val result = CalculationController.annualExemptAmount(fakeRequest)
//        status(result) shouldBe 200
//      }
//
//      "return HTML" in new fakeRequestTo("allowance") {
//        val result = CalculationController.annualExemptAmount(fakeRequest)
//        contentType(result) shouldBe Some("text/html")
//        charset(result) shouldBe Some("utf-8")
//      }
//
//      "display the correct page title" in new fakeRequestTo("allowance") {
//        val result = CalculationController.annualExemptAmount(fakeRequest)
//        val jsoupDoc = Jsoup.parse(bodyOf(result))
//        jsoupDoc.title shouldEqual Messages("calc.annualExemptAmount.question")
//      }
//
//      "diplay the correct page heading" in new fakeRequestTo("allowance") {
//        val result = CalculationController.annualExemptAmount(fakeRequest)
//        val jsoupDoc = Jsoup.parse(bodyOf(result))
//        jsoupDoc.body.getElementsByTag("H1").text shouldEqual Messages("calc.base.pageHeading")
//      }
//
//      "contain a back button to the previous page" in new fakeRequestTo("allowance") {
//        val result = CalculationController.annualExemptAmount(fakeRequest)
//        val jsoupDoc = Jsoup.parse(bodyOf(result))
//        jsoupDoc.body.getElementById("link-back").text shouldEqual Messages("calc.base.back")
//      }
//
//      "display the correct question heading" in new fakeRequestTo("allowance") {
//        val result = CalculationController.annualExemptAmount(fakeRequest)
//        val jsoupDoc = Jsoup.parse(bodyOf(result))
//        jsoupDoc.body.getElementsByTag("legend").text shouldEqual Messages("calc.annualExemptAmount.question")
//      }
//
//      "Have an input box for the Annual Exempt Amount" in new fakeRequestTo("allowance") {
//        val result = CalculationController.annualExemptAmount(fakeRequest)
//        val jsoupDoc = Jsoup.parse(bodyOf(result))
//        jsoupDoc.body.getElementById("annualExemptAmount").tagName() shouldEqual "input"
//      }
//
//      "has a Continue button" in new fakeRequestTo("allowance") {
//        val result = CalculationController.annualExemptAmount(fakeRequest)
//        val jsoupDoc = Jsoup.parse(bodyOf(result))
//        jsoupDoc.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
//      }
//
//      "should contain a Read more sidebar with a link to CGT allowances" in new fakeRequestTo("allowance") {
//        val result = CalculationController.annualExemptAmount(fakeRequest)
//        val jsoupDoc = Jsoup.parse(bodyOf(result))
//        jsoupDoc.select("aside h2").text shouldBe Messages("calc.common.readMore")
//        jsoupDoc.select("aside a").text shouldBe Messages("calc.annualExemptAmount.link.one")
//      }
//    }
//
//    //############## Acquisition Value tests ######################
//    "return 200 when sending a GET request `/calculate-your-capital-gains/acquisition-value`" in new fakeRequestTo("acquisition-value") {
//      val result = CalculationController.acquisitionValue(fakeRequest)
//      status(result) shouldBe 200
//    }
//
//    "return HTML when sending a GET request `/calculate-your-capital-gains/acquisition-value`" in new fakeRequestTo("acquisition-value") {
//      val result = CalculationController.acquisitionValue(fakeRequest)
//      contentType(result) shouldBe Some("text/html")
//      charset(result) shouldBe Some("utf-8")
//    }
//
//    //################### Improvements tests #######################
//    "return 200 when sending a GET to `/calculate-your-capital-gains/improvements`" in new fakeRequestTo("improvements") {
//      val result = CalculationController.improvements(fakeRequest)
//      status(result) shouldBe 200
//    }
//
//    "return HTML when sending a GET to `/calculate-your-capital-gains/improvements`" in new fakeRequestTo("improvements"){
//      val result = CalculationController.improvements(fakeRequest)
//      contentType(result) shouldBe Some("text/html")
//      charset(result) shouldBe Some("utf-8")
//    }
//
//    //################### Disposal Date tests #######################
//    "return 200 when sending a GET to `/calculate-your-capital-gains/disposal-date`" in new fakeRequestTo("disposal-date") {
//      val result = CalculationController.disposalDate(fakeRequest)
//      status(result) shouldBe 200
//    }
//
//    "return HTML when sending a GET to `/calculate-your-capital-gains/disposal-date`" in new fakeRequestTo("disposal-date"){
//      val result = CalculationController.disposalDate(fakeRequest)
//      contentType(result) shouldBe Some("text/html")
//      charset(result) shouldBe Some("utf-8")
//    }
//
//    "display the correct title for the disposal-date page" in new fakeRequestTo("disposal-date"){
//      val result = CalculationController.disposalDate(fakeRequest)
//      val jsoupDoc = Jsoup.parse(bodyOf(result))
//      jsoupDoc.title shouldEqual Messages("calc.disposalDate.question")
//    }
//
//    "display the correct heading for the disposal-date page" in new fakeRequestTo("disposal-date") {
//      val result = CalculationController.disposalDate(fakeRequest)
//      val jsoupDoc = Jsoup.parse(bodyOf(result))
//      jsoupDoc.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
//    }
//
//    "contain a simpleInlineDate with question When did you sign the contract that made someone else the owner?" in new fakeRequestTo("disposal-date") {
//      val result = CalculationController.disposalDate(fakeRequest)
//      val jsoupDoc = Jsoup.parse(bodyOf(result))
//      jsoupDoc.select("legend").text shouldEqual Messages("calc.disposalDate.question")
//    }
//
//    "contain a button with id equal to continue in disposal-date" in new fakeRequestTo("disposal-date") {
//      val result = CalculationController.disposalDate(fakeRequest)
//      val jsoupDoc = Jsoup.parse(bodyOf(result))
//      jsoupDoc.select("button#continue").text shouldEqual Messages("calc.base.continue")
//    }
//
//    //################### Disposal Value tests #######################
//    "return 200 from disposal-value" in new fakeRequestTo("disposal-value") {
//      val result = CalculationController.disposalValue(fakeRequest)
//      status(result) shouldBe 200
//    }
//
//    "return HTML from disposal-value" in new fakeRequestTo("disposal-value") {
//      val result = CalculationController.disposalValue(fakeRequest)
//      contentType(result) shouldBe Some("text/html")
//      charset(result) shouldBe Some("utf-8")
//    }
//
//    "contain the question How much did you sell or give away the property for?" in new fakeRequestTo("disposal-value") {
//      val result = CalculationController.disposalValue(fakeRequest)
//      val jsoupDoc = Jsoup.parse(bodyOf(result))
//      jsoupDoc.select("legend").text shouldEqual Messages("calc.disposalValue.question")
//    }
//
//    "contain a button with id equal to continue in disposal-value" in new fakeRequestTo("disposal-date") {
//      val result = CalculationController.disposalValue(fakeRequest)
//      val jsoupDoc = Jsoup.parse(bodyOf(result))
//      jsoupDoc.select("button#continue").text shouldEqual Messages("calc.base.continue")
//    }
//
//    //################### Acquisition Costs tests #######################
//    "return 200 from acquisition-costs" in new fakeRequestTo("acquisition-costs") {
//      val result = CalculationController.acquisitionCosts(fakeRequest)
//      status(result) shouldBe 200
//    }
//
//    "return HTML from acquisition-costs" in new fakeRequestTo("acquisition-costs") {
//      val result = CalculationController.acquisitionCosts(fakeRequest)
//      contentType(result) shouldBe Some("text/html")
//      charset(result) shouldBe Some("utf-8")
//    }
//
//    //################### Disposal Costs tests #######################
//    "return 200 from disposal-costs" in new fakeRequestTo("disposal-costs") {
//      val result = CalculationController.disposalCosts(fakeRequest)
//      status(result) shouldBe 200
//    }
//
//    "return HTML from disposal-costs" in new fakeRequestTo("disposal-costs"){
//      val result = CalculationController.disposalCosts(fakeRequest)
//      contentType(result) shouldBe Some("text/html")
//      charset(result) shouldBe Some("utf-8")
//    }
//
//    //################### Entrepreneurs Relief tests #######################
//    "return 200 from entrepreneurs-relief" in new fakeRequestTo("entrepreneurs-relief") {
//      val result = CalculationController.entrepreneursRelief(fakeRequest)
//      status(result) shouldBe 200
//    }
//
//    "return HTML from entrepreneurs-relief" in new fakeRequestTo("entrepreneurs-relief"){
//      val result = CalculationController.entrepreneursRelief(fakeRequest)
//      contentType(result) shouldBe Some("text/html")
//      charset(result) shouldBe Some("utf-8")
//    }
//
//    //################### Allowable Losses tests #######################
//    "return 200 when sending a GET request `/calculate-your-capital-gains/allowable-losses`" in new fakeRequestTo("allowable-losses") {
//      val result = CalculationController.allowableLosses(fakeRequest)
//      status(result) shouldBe 200
//    }
//
//    "return HTML when sending a GET to `/calculate-your-capital-gains/allowable-losses`" in new fakeRequestTo("allowable-losses"){
//      val result = CalculationController.allowableLosses(fakeRequest)
//      contentType(result) shouldBe Some("text/html")
//      charset(result) shouldBe Some("utf-8")
//    }
//
//    //################### Other Reliefs tests #######################
//    "return 200 when sending a GET request `/calculate-your-capital-gains/other-reliefs`" in new fakeRequestTo("other-reliefs") {
//      val result = CalculationController.otherReliefs(fakeRequest)
//      status(result) shouldBe 200
//    }
//
//    "return HTML when sending a GET request `/calculate-your-capital-gains/other-reliefs`" in new fakeRequestTo("other-reliefs") {
//      val result = CalculationController.otherReliefs(fakeRequest)
//      contentType(result) shouldBe Some("text/html")
//    }
//
//    //################### Summary tests #######################
//    "CalculationController.summary" should {
//      "return 200 from summary" in new fakeRequestTo("summary") {
//        val result = CalculationController.summary(fakeRequest)
//        status(result) shouldBe 200
//      }
//
//      "return HTML from summary" in new fakeRequestTo("summary") {
//        val result = CalculationController.summary(fakeRequest)
//        contentType(result) shouldBe Some("text/html")
//        charset(result) shouldBe Some("utf-8")
//      }
//    }
  }
}