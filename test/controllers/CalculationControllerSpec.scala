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

import java.util.UUID
import scala.collection.JavaConversions._

import connectors.KeystoreConnector
import models.CustomerTypeModel
import org.scalatest.BeforeAndAfterEach
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.{SessionKeys, HeaderCarrier}
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import org.jsoup._
import org.scalatest.mock.MockitoSugar
import scala.concurrent.Future

class CalculationControllerSpec extends UnitSpec with WithFakeApplication with MockitoSugar with BeforeAndAfterEach{

  val s = "Action(parser=BodyParser(anyContent))"
  val sessionId = UUID.randomUUID.toString

  class fakeRequestTo(url : String) {
    val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/" + url).withSession(SessionKeys.sessionId -> s"session-$sessionId")
  }

  val mockKeystoreConnector = mock[KeystoreConnector]
  val TestCalculationController = new CalculationController {
    override val keystoreConnector: KeystoreConnector = mockKeystoreConnector
  }

  implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(sessionId.toString)))

  "CalculationController methods " should {

    //################### Customer Type tests #######################
    "when calling the customerType action" should {
      "when not supplied with a test model" should {

        "return 200" in new fakeRequestTo("customer-type") {
          when(mockKeystoreConnector.fetchAndGetFormData[CustomerTypeModel](Matchers.anyString())(Matchers.any(), Matchers.any()))
            .thenReturn(Future.successful(None))
          val result = TestCalculationController.customerType(fakeRequest)
          status(result) shouldBe 200
        }

        "return HTML" in new fakeRequestTo("customer-type") {
          when(mockKeystoreConnector.fetchAndGetFormData[CustomerTypeModel](Matchers.anyString())(Matchers.any(), Matchers.any()))
            .thenReturn(Future.successful(None))
          val result = TestCalculationController.customerType(fakeRequest)
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "display the correct title" in new fakeRequestTo("customer-type") {
          when(mockKeystoreConnector.fetchAndGetFormData[CustomerTypeModel](Matchers.anyString())(Matchers.any(), Matchers.any()))
            .thenReturn(Future.successful(None))
          val result = TestCalculationController.customerType(fakeRequest)
          val jsoupDoc = Jsoup.parse(bodyOf(result))
          jsoupDoc.title shouldEqual Messages("calc.customerType.title")
        }

        "display the correct heading" in new fakeRequestTo("customer-type") {
          when(mockKeystoreConnector.fetchAndGetFormData[CustomerTypeModel](Matchers.anyString())(Matchers.any(), Matchers.any()))
            .thenReturn(Future.successful(None))
          val result = TestCalculationController.customerType(fakeRequest)
          val jsoupDoc = Jsoup.parse(bodyOf(result))
          jsoupDoc.body.getElementsByTag("H1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "display the correct wording for radio option `individual`" in new fakeRequestTo("customer-type") {
          when(mockKeystoreConnector.fetchAndGetFormData[CustomerTypeModel](Matchers.anyString())(Matchers.any(), Matchers.any()))
            .thenReturn(Future.successful(None))
          val result = TestCalculationController.customerType(fakeRequest)
          val jsoupDoc = Jsoup.parse(bodyOf(result))
          jsoupDoc.body.getElementById("customerType-individual").parent.text shouldEqual Messages("calc.customerType.individual")
        }

        "display the correct wording for radio option `trustee`" in new fakeRequestTo("customer-type") {
          when(mockKeystoreConnector.fetchAndGetFormData[CustomerTypeModel](Matchers.anyString())(Matchers.any(), Matchers.any()))
            .thenReturn(Future.successful(None))
          val result = TestCalculationController.customerType(fakeRequest)
          val jsoupDoc = Jsoup.parse(bodyOf(result))
          jsoupDoc.body.getElementById("customerType-trustee").parent.text shouldEqual Messages("calc.customerType.trustee")
        }

        "display the correct wording for radio option `Personal Representative`" in new fakeRequestTo("customer-type") {
          when(mockKeystoreConnector.fetchAndGetFormData[CustomerTypeModel](Matchers.anyString())(Matchers.any(), Matchers.any()))
            .thenReturn(Future.successful(None))
          val result = TestCalculationController.customerType(fakeRequest)
          val jsoupDoc = Jsoup.parse(bodyOf(result))
          jsoupDoc.body.getElementById("customerType-personalrep").parent.text shouldEqual Messages("calc.customerType.personalRep")
        }

        "have the radio option `individual` not selected by default" in new fakeRequestTo("customer-type") {
          when(mockKeystoreConnector.fetchAndGetFormData[CustomerTypeModel](Matchers.anyString())(Matchers.any(), Matchers.any()))
            .thenReturn(Future.successful(None))
          val result = TestCalculationController.customerType(fakeRequest)
          val jsoupDoc = Jsoup.parse(bodyOf(result))
          jsoupDoc.body.getElementById("customerType-individual").parent.classNames().contains("selected") shouldBe false
        }

      }

      "when supplied with a test model containing the variable 'individual'" should {

        val testCustomerTypeModel = new CustomerTypeModel("individual")

        "return 200" in new fakeRequestTo("customer-type") {
          when(mockKeystoreConnector.fetchAndGetFormData[CustomerTypeModel](Matchers.anyString())(Matchers.any(), Matchers.any()))
            .thenReturn(Future.successful(Some(testCustomerTypeModel)))
          val result = TestCalculationController.customerType(fakeRequest)
          status(result) shouldBe 200
        }

        "return HTML" in new fakeRequestTo("customer-type") {
          when(mockKeystoreConnector.fetchAndGetFormData[CustomerTypeModel](Matchers.anyString())(Matchers.any(), Matchers.any()))
            .thenReturn(Future.successful(Some(testCustomerTypeModel)))
          val result = TestCalculationController.customerType(fakeRequest)
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "have the radio option `individual` selected by default" in new fakeRequestTo("customer-type") {
          when(mockKeystoreConnector.fetchAndGetFormData[CustomerTypeModel](Matchers.anyString())(Matchers.any(), Matchers.any()))
            .thenReturn(Future.successful(Some(testCustomerTypeModel)))
          val result = TestCalculationController.customerType(fakeRequest)
          val jsoupDoc = Jsoup.parse(bodyOf(result))
          jsoupDoc.body.getElementById("customerType-individual").parent.classNames().contains("selected") shouldBe true
        }

      }
    }

    //################### Disabled Trustee tests #######################
    "return 200 from disabled-trustee" in new fakeRequestTo("disabled-trustee") {
      val result = CalculationController.disabledTrustee(fakeRequest)
      status(result) shouldBe 200
    }

    "return HTML from disabled-trustee" in new fakeRequestTo("disabled-trustee"){
      val result = CalculationController.disabledTrustee(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }

    "display the correct title for the disabled-trustee page" in new fakeRequestTo("disabled-trustee"){
      val result = CalculationController.disabledTrustee(fakeRequest)
      val jsoupDoc = Jsoup.parse(bodyOf(result))
      jsoupDoc.title shouldEqual Messages("calc.disabledTrustee.question")
    }

    "display the correct heading for the disabled-trustee page" in new fakeRequestTo("disabled-trustee") {
      val result = CalculationController.disabledTrustee(fakeRequest)
      val jsoupDoc = Jsoup.parse(bodyOf(result))
      jsoupDoc.body.getElementsByTag("H1").text shouldEqual Messages("calc.base.pageHeading")
    }

    "display Yes/No radio options on disabled-trustee page" in new fakeRequestTo("disabled-trustee"){
      val result = CalculationController.disabledTrustee(fakeRequest)
      val jsoupDoc = Jsoup.parse(bodyOf(result))
      jsoupDoc.body.getElementById("isVulnerableYes").parent.text shouldEqual Messages("calc.base.yes")
      jsoupDoc.body.getElementById("isVulnerableNo").parent.text shouldEqual Messages("calc.base.no")
    }

    "display a Continue button on disabled-trustee page" in new fakeRequestTo("disabled-trustee"){
      val result = CalculationController.disabledTrustee(fakeRequest)
      val jsoupDoc = Jsoup.parse(bodyOf(result))
      jsoupDoc.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
    }

    //################### Current Income tests #######################
    "be Action(parser=BodyParser(anyContent)) for currentIncome" in {
      val result = CalculationController.currentIncome.toString()
      result shouldBe s
    }

    //############## Personal Allowance tests ######################
    "return 200 from personal-allowance" in new fakeRequestTo("personal-allowance") {
      val result = CalculationController.personalAllowance(fakeRequest)
      status(result) shouldBe 200
    }

    "return HTML from personal-allowance" in new fakeRequestTo("personal-allowance"){
      val result = CalculationController.personalAllowance(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }

    //############## Other Properties tests ######################
    "return 200 from other-properties" in new fakeRequestTo("other-properties") {
      val result = CalculationController.otherProperties(fakeRequest)
      status(result) shouldBe 200
    }

    "return HTML from other-properties" in new fakeRequestTo("other-properties"){
      val result = CalculationController.otherProperties(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }

    "display the correct title for the other-properties page" in new fakeRequestTo("other-properties"){
      val result = CalculationController.otherProperties(fakeRequest)
      val jsoupDoc = Jsoup.parse(bodyOf(result))
      jsoupDoc.title shouldEqual Messages("calc.otherProperties.title")
    }

    "display the correct heading for the other-properties page" in new fakeRequestTo("other-properties") {
      val result = CalculationController.otherProperties(fakeRequest)
      val jsoupDoc = Jsoup.parse(bodyOf(result))
      jsoupDoc.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
    }

    "display the correct wording for radio option `Yes`" in new fakeRequestTo("other-properties"){
      val result = CalculationController.otherProperties(fakeRequest)
      val jsoupDoc = Jsoup.parse(bodyOf(result))
      jsoupDoc.body.getElementById("otherPropertiesYes").parent.text shouldEqual Messages("calc.base.yes")
    }

    "display the correct wording for radio option `No`" in new fakeRequestTo("other-properties"){
      val result = CalculationController.otherProperties(fakeRequest)
      val jsoupDoc = Jsoup.parse(bodyOf(result))
      jsoupDoc.body.getElementById("otherPropertiesNo").parent.text shouldEqual Messages("calc.base.no")
    }

    "contain a button with id equal to continue in other-properties" in new fakeRequestTo("other-properties") {
      val result = CalculationController.otherProperties(fakeRequest)
      val jsoupDoc = Jsoup.parse(bodyOf(result))
      jsoupDoc.select("button#continue").text shouldEqual Messages("calc.base.continue")
    }

    //############## Annual Exempt Amount tests ######################
    "when calling the annualExemptAmount action" should {

      "return 200" in new fakeRequestTo("allowance") {
        val result = CalculationController.annualExemptAmount(fakeRequest)
        status(result) shouldBe 200
      }

      "return HTML" in new fakeRequestTo("allowance") {
        val result = CalculationController.annualExemptAmount(fakeRequest)
        contentType(result) shouldBe Some("text/html")
        charset(result) shouldBe Some("utf-8")
      }

      "display the correct page title" in new fakeRequestTo("allowance") {
        val result = CalculationController.annualExemptAmount(fakeRequest)
        val jsoupDoc = Jsoup.parse(bodyOf(result))
        jsoupDoc.title shouldEqual Messages("calc.annualExemptAmount.question")
      }

      "diplay the correct page heading" in new fakeRequestTo("allowance") {
        val result = CalculationController.annualExemptAmount(fakeRequest)
        val jsoupDoc = Jsoup.parse(bodyOf(result))
        jsoupDoc.body.getElementsByTag("H1").text shouldEqual Messages("calc.base.pageHeading")
      }

      "contain a back button to the previous page" in new fakeRequestTo("allowance") {
        val result = CalculationController.annualExemptAmount(fakeRequest)
        val jsoupDoc = Jsoup.parse(bodyOf(result))
        jsoupDoc.body.getElementById("link-back").text shouldEqual Messages("calc.base.back")
      }

      "display the correct question heading" in new fakeRequestTo("allowance") {
        val result = CalculationController.annualExemptAmount(fakeRequest)
        val jsoupDoc = Jsoup.parse(bodyOf(result))
        jsoupDoc.body.getElementsByTag("legend").text shouldEqual Messages("calc.annualExemptAmount.question")
      }

      "Have an input box for the Annual Exempt Amount" in new fakeRequestTo("allowance") {
        val result = CalculationController.annualExemptAmount(fakeRequest)
        val jsoupDoc = Jsoup.parse(bodyOf(result))
        jsoupDoc.body.getElementById("annualExemptAmount").tagName() shouldEqual "input"
      }

      "has a Continue button" in new fakeRequestTo("allowance") {
        val result = CalculationController.annualExemptAmount(fakeRequest)
        val jsoupDoc = Jsoup.parse(bodyOf(result))
        jsoupDoc.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
      }

      "should contain a Read more sidebar with a link to CGT allowances" in new fakeRequestTo("allowance") {
        val result = CalculationController.annualExemptAmount(fakeRequest)
        val jsoupDoc = Jsoup.parse(bodyOf(result))
        jsoupDoc.select("aside h2").text shouldBe Messages("calc.common.readMore")
        jsoupDoc.select("aside a").text shouldBe Messages("calc.annualExemptAmount.link.one")
      }
    }

    //############## Acquisition Value tests ######################
    "return 200 when sending a GET request `/calculate-your-capital-gains/acquisition-value`" in new fakeRequestTo("acquisition-value") {
      val result = CalculationController.acquisitionValue(fakeRequest)
      status(result) shouldBe 200
    }

    "return HTML when sending a GET request `/calculate-your-capital-gains/acquisition-value`" in new fakeRequestTo("acquisition-value") {
      val result = CalculationController.acquisitionValue(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }

    //################### Improvements tests #######################
    "return 200 when sending a GET to `/calculate-your-capital-gains/improvements`" in new fakeRequestTo("improvements") {
      val result = CalculationController.improvements(fakeRequest)
      status(result) shouldBe 200
    }

    "return HTML when sending a GET to `/calculate-your-capital-gains/improvements`" in new fakeRequestTo("improvements"){
      val result = CalculationController.improvements(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }

    //################### Disposal Date tests #######################
    "return 200 when sending a GET to `/calculate-your-capital-gains/disposal-date`" in new fakeRequestTo("disposal-date") {
      val result = CalculationController.disposalDate(fakeRequest)
      status(result) shouldBe 200
    }

    "return HTML when sending a GET to `/calculate-your-capital-gains/disposal-date`" in new fakeRequestTo("disposal-date"){
      val result = CalculationController.disposalDate(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }

    "display the correct title for the disposal-date page" in new fakeRequestTo("disposal-date"){
      val result = CalculationController.disposalDate(fakeRequest)
      val jsoupDoc = Jsoup.parse(bodyOf(result))
      jsoupDoc.title shouldEqual Messages("calc.disposalDate.title")
    }

    "display the correct heading for the disposal-date page" in new fakeRequestTo("disposal-date") {
      val result = CalculationController.disposalDate(fakeRequest)
      val jsoupDoc = Jsoup.parse(bodyOf(result))
      jsoupDoc.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
    }

    "contain a simpleInlineDate with question When did you sign the contract that made someone else the owner?" in new fakeRequestTo("disposal-date") {
      val result = CalculationController.disposalDate(fakeRequest)
      val jsoupDoc = Jsoup.parse(bodyOf(result))
      jsoupDoc.select("legend").text shouldEqual Messages("calc.disposalDate.title")
    }

    "contain a button with id equal to continue in disposal-date" in new fakeRequestTo("disposal-date") {
      val result = CalculationController.disposalDate(fakeRequest)
      val jsoupDoc = Jsoup.parse(bodyOf(result))
      jsoupDoc.select("button#continue").text shouldEqual Messages("calc.base.continue")
    }

    //################### Disposal Value tests #######################
    "return 200 from disposal-value" in new fakeRequestTo("disposal-value") {
      val result = CalculationController.disposalValue(fakeRequest)
      status(result) shouldBe 200
    }

    "return HTML from disposal-value" in new fakeRequestTo("disposal-value") {
      val result = CalculationController.disposalValue(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }

    "contain the question How much did you sell or give away the property for?" in new fakeRequestTo("disposal-value") {
      val result = CalculationController.disposalValue(fakeRequest)
      val jsoupDoc = Jsoup.parse(bodyOf(result))
      jsoupDoc.select("legend").text shouldEqual Messages("calc.disposalValue.title")
    }

    "contain a button with id equal to continue in disposal-value" in new fakeRequestTo("disposal-date") {
      val result = CalculationController.disposalValue(fakeRequest)
      val jsoupDoc = Jsoup.parse(bodyOf(result))
      jsoupDoc.select("button#continue").text shouldEqual Messages("calc.base.continue")
    }

    //################### Acquisition Costs tests #######################
    "return 200 from acquisition-costs" in new fakeRequestTo("acquisition-costs") {
      val result = CalculationController.acquisitionCosts(fakeRequest)
      status(result) shouldBe 200
    }

    "return HTML from acquisition-costs" in new fakeRequestTo("acquisition-costs") {
      val result = CalculationController.acquisitionCosts(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }

    //################### Disposal Costs tests #######################
    "return 200 from disposal-costs" in new fakeRequestTo("disposal-costs") {
      val result = CalculationController.disposalCosts(fakeRequest)
      status(result) shouldBe 200
    }

    "return HTML from disposal-costs" in new fakeRequestTo("disposal-costs"){
      val result = CalculationController.disposalCosts(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }

    //################### Entrepreneurs Relief tests #######################
    "return 200 from entrepreneurs-relief" in new fakeRequestTo("entrepreneurs-relief") {
      val result = CalculationController.entrepreneursRelief(fakeRequest)
      status(result) shouldBe 200
    }

    "return HTML from entrepreneurs-relief" in new fakeRequestTo("entrepreneurs-relief"){
      val result = CalculationController.entrepreneursRelief(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }

    //################### Allowable Losses tests #######################
    "return 200 when sending a GET request `/calculate-your-capital-gains/allowable-losses`" in new fakeRequestTo("allowable-losses") {
      val result = CalculationController.allowableLosses(fakeRequest)
      status(result) shouldBe 200
    }

    "return HTML when sending a GET to `/calculate-your-capital-gains/allowable-losses`" in new fakeRequestTo("allowable-losses"){
      val result = CalculationController.allowableLosses(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }

    //################### Other Reliefs tests #######################
    "return 200 when sending a GET request `/calculate-your-capital-gains/other-reliefs`" in new fakeRequestTo("other-reliefs") {
      val result = CalculationController.otherReliefs(fakeRequest)
      status(result) shouldBe 200
    }

    "return HTML when sending a GET request `/calculate-your-capital-gains/other-reliefs`" in new fakeRequestTo("other-reliefs") {
      val result = CalculationController.otherReliefs(fakeRequest)
      contentType(result) shouldBe Some("text/html")
    }

    //################### Summary tests #######################
    "CalculationController.summary" should {
      "return 200 from summary" in new fakeRequestTo("summary") {
        val result = CalculationController.summary(fakeRequest)
        status(result) shouldBe 200
      }

      "return HTML from summary" in new fakeRequestTo("summary") {
        val result = CalculationController.summary(fakeRequest)
        contentType(result) shouldBe Some("text/html")
        charset(result) shouldBe Some("utf-8")
      }
    }
  }
}