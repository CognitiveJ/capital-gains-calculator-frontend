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

import connectors.CalculatorConnector
import constructors.CalculationElectionConstructor
import controllers.{routes, CalculationController}
import models.CurrentIncomeModel
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc.{Result, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.{SessionKeys, HeaderCarrier}
import uk.gov.hmrc.play.test.{WithFakeApplication, UnitSpec}

import scala.concurrent.Future

class CurrentIncomeSpec extends UnitSpec with WithFakeApplication with MockitoSugar {

  implicit val hc = new HeaderCarrier()
  def setupTarget(getData: Option[CurrentIncomeModel], postData: Option[CurrentIncomeModel]): CalculationController = {

    val mockCalcConnector = mock[CalculatorConnector]
    val mockCalcElectionConstructor = mock[CalculationElectionConstructor]

    when(mockCalcConnector.fetchAndGetFormData[CurrentIncomeModel](Matchers.anyString())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(getData))

    lazy val data = CacheMap("form-id", Map("data" -> Json.toJson(postData.getOrElse(CurrentIncomeModel(0)))))
    when(mockCalcConnector.saveFormData[CurrentIncomeModel](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(data))

    new CalculationController {
      override val calcConnector: CalculatorConnector = mockCalcConnector
      override val calcElectionConstructor: CalculationElectionConstructor = mockCalcElectionConstructor
    }
  }


  //GET Tests
  "In CalculationController calling the .currentIncome action " when {

    lazy val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/current-income").withSession(SessionKeys.sessionId -> "12345")

    "not supplied with a pre-existing stored model" should {

      val target = setupTarget(None, None)
      lazy val result = target.currentIncome(fakeRequest)
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 200" in {
        status(result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "have the title 'In the tax year when you stopped owning the property, what was your total UK income?'" in {
          document.title shouldEqual Messages("calc.currentIncome.question")
        }

        "have the heading Calculate your tax (non-residents) " in {
          document.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a 'Back' link " in {
          document.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
        }

        "have the question 'In the tax year when you stopped owning the property, what was your total UK income?' as the label of the input" in {
          document.body.getElementsByTag("label").text.contains(Messages("calc.currentIncome.question")) shouldBe true
        }

        "have the help text 'Tax years start on 6 April' as the form-hint of the input" in {
          document.body.getElementsByClass("form-hint").text shouldEqual Messages("calc.currentIncome.helpText")
        }

        "display an input box for the Current Income Amount" in {
          document.body.getElementById("currentIncome").tagName() shouldEqual "input"
        }

        "have no value auto-filled into the input box" in {
          document.getElementById("currentIncome").attr("value") shouldBe empty
        }

        "display a 'Continue' button " in {
          document.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
        }

        "should contain a Read more sidebar with a link to CGT allowances" in {
          document.select("aside h2").text shouldBe Messages("calc.common.readMore")
          document.select("aside a").first.text shouldBe Messages("calc.currentIncome.link.one")
          document.select("aside a").last.text shouldBe Messages("calc.currentIncome.link.two")
        }
      }
    }

    "supplied with a pre-existing stored model" should {

      val target = setupTarget(Some(CurrentIncomeModel(1000)), None)
      lazy val result = target.currentIncome(fakeRequest)
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 200" in {
        status(result) shouldBe 200
      }

      "return some HTML that" should {

        "have some value auto-filled into the input box" in {

          document.getElementById("currentIncome").attr("value") shouldBe "1000"
        }
      }
    }
  }

  //POST Tests
  "In CalculationController calling the .submitCurrentIncome action " when {

    def buildRequest(body: (String, String)*): FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest("POST", "/calculate-your-capital-gains/current-income")
      .withSession(SessionKeys.sessionId -> "12345")
      .withFormUrlEncodedBody(body: _*)

    def executeTargetWithMockData(amount: String): Future[Result] = {

      lazy val fakeRequest = buildRequest(("currentIncome", amount))

      val numeric = "(0-9*)".r
      val mockData = amount match {
        case numeric(money) => new CurrentIncomeModel(BigDecimal(money))
        case _ => new CurrentIncomeModel(0)
      }

      val target = setupTarget(None, Some(mockData))
      target.submitCurrentIncome(fakeRequest)
    }

    "submitting a valid form" should {

      lazy val result = executeTargetWithMockData("1000")

      "return a 303" in {
        status(result) shouldBe 303
      }

      s"redirect to ${routes.CalculationController.personalAllowance()}" in {
        redirectLocation(result) shouldBe Some(s"${routes.CalculationController.personalAllowance()}")
      }
    }

    "submitting an invalid form with no data" should {

      lazy val result = executeTargetWithMockData("")

      "return a 400" in {
        status(result) shouldBe 400
      }
    }

    "submitting an invalid form with a negative value" should {

      lazy val result = executeTargetWithMockData("-1000")

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

      s"fail with message ${Messages("calc.currentIncome.errorDecimalPlaces")}" in {
        document.getElementsByClass("error-notification").text should include (Messages("calc.currentIncome.errorDecimalPlaces"))
      }
    }
  }
}
