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

package controllers.CalculationControllerTests

import constructors.CalculationElectionConstructor
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.CacheMap
import connectors.CalculatorConnector
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import org.jsoup._
import org.scalatest.mock.MockitoSugar

import scala.concurrent.Future
import controllers.{CalculationController, routes}
import play.api.mvc.Result

class PersonalAllowanceSpec extends UnitSpec with WithFakeApplication with MockitoSugar {

  implicit val hc = new HeaderCarrier()

  def setupTarget(getData: Option[PersonalAllowanceModel], postData: Option[PersonalAllowanceModel]): CalculationController = {

    val mockCalcConnector = mock[CalculatorConnector]
    val mockCalcElectionConstructor = mock[CalculationElectionConstructor]

    when(mockCalcConnector.fetchAndGetFormData[PersonalAllowanceModel](Matchers.anyString())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(getData))

    lazy val data = CacheMap("form-id", Map("data" -> Json.toJson(postData.getOrElse(PersonalAllowanceModel(0)))))
    when(mockCalcConnector.saveFormData[PersonalAllowanceModel](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(data))

    new CalculationController {
      override val calcConnector: CalculatorConnector = mockCalcConnector
      override val calcElectionConstructor: CalculationElectionConstructor = mockCalcElectionConstructor
    }
  }

  // GET Tests
  "Calling the CalculationController.customerType" when {

    lazy val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/personal-allowance").withSession(SessionKeys.sessionId -> "12345")

    "not supplied with a pre-existing stored model" should {

      val target = setupTarget(None, None)
      lazy val result = target.personalAllowance(fakeRequest)
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 200" in {
        status(result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "have the title In the tax year when you stopped owning the property, what was your UK Personal Allowance?" in {
          document.title shouldEqual Messages("calc.personalAllowance.question")
        }

        "have the heading Calculate your tax (non-residents) " in {
          document.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a 'Back' link " in {
          document.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
        }

        "have the question 'In the tax year when you stopped owning the property, what was your UK Personal Allowance?' as the label of the input" in {
          document.body.getElementsByTag("label").text should include(Messages("calc.personalAllowance.question"))
        }

        "display an input box for the Personal Allowance" in {
          document.body.getElementById("personalAllowance").tagName() shouldEqual "input"
        }

        "have no value auto-filled into the input box" in {
          document.getElementById("personalAllowance").attr("value") shouldBe empty
        }

        "display a 'Continue' button " in {
          document.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
        }

        "should contain a Read more sidebar with a link to personal allowances and taxation abroad" in {
          document.select("aside h2").text shouldBe Messages("calc.common.readMore")
          document.select("aside a").first().attr("href") shouldBe "https://www.gov.uk/income-tax-rates/current-rates-and-allowances"
          document.select("aside a").last().attr("href") shouldBe "https://www.gov.uk/tax-uk-income-live-abroad/personal-allowance"
        }
      }
    }

    "supplied with a pre-existing stored model" should {

      val target = setupTarget(Some(PersonalAllowanceModel(1000)), None)
      lazy val result = target.personalAllowance(fakeRequest)
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 200" in {
        status(result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "have the value 1000 auto-filled into the input box" in {
          document.getElementById("personalAllowance").attr("value") shouldEqual ("1000")
        }
      }
    }
  }

  // POST Tests
  "In CalculationController calling the .submitPersonalAllowance action" when {

    def buildRequest(body: (String, String)*): FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest("POST", "/calculate-your-capital-gains/personal-allowance")
      .withSession(SessionKeys.sessionId -> "12345")
      .withFormUrlEncodedBody(body: _*)

    def executeTargetWithMockData(data: String): Future[Result] = {
      lazy val fakeRequest = buildRequest(("personalAllowance", data))
      val mockData = data match {
        case "" => None
        case _ => Some(PersonalAllowanceModel(BigDecimal(data)))
      }
      val target = setupTarget(None, mockData)
      target.submitPersonalAllowance(fakeRequest)
    }

    "submitting a valid form with '1000'" should {

      lazy val result = executeTargetWithMockData("1000")

      "return a 303" in {
        status(result) shouldBe 303
      }

      s"redirect to ${routes.CalculationController.otherProperties()}" in {
        redirectLocation(result) shouldBe Some(s"${routes.CalculationController.otherProperties()}")
      }


      "submitting an invalid form with no value" should {

        lazy val result = executeTargetWithMockData("")

        "return a 400" in {
          status(result) shouldBe 400
        }
      }

      "submitting an invalid form with a negative value of -342" should {

        lazy val result = executeTargetWithMockData("-342")

        "return a 400" in {
          status(result) shouldBe 400
        }
      }

      "submitting an invalid form with value 1.111" should {

        lazy val result = executeTargetWithMockData("1.111")
        lazy val document = Jsoup.parse(bodyOf(result))

        "return a 400" in {
          status(result) shouldBe 400
        }

        s"fail with message ${Messages("calc.personalAllowance.errorDecimalPlaces")}" in {
          document.getElementsByClass("error-notification").text should include(Messages("calc.personalAllowance.errorDecimalPlaces"))
        }
      }

      "submitting a form which exceeds the maximum PA amount" should {

        lazy val result = executeTargetWithMockData("12100.01")
        lazy val document = Jsoup.parse(bodyOf(result))

        "return a 400" in {
          status(result) shouldBe 400
        }

        s"fail with message ${Messages("calc.personalAllowance.errorMaxLimit")}" in {
          document.getElementsByClass("error-notification").text should include(Messages("calc.personalAllowance.errorMaxLimit"))
        }
      }
    }
  }
}
