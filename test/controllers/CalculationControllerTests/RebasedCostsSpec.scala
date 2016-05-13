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
import controllers.CalculationController
import play.api.mvc.Result

class RebasedCostsSpec extends UnitSpec with WithFakeApplication with MockitoSugar {

  implicit val hc = new HeaderCarrier()

  def setupTarget(getData: Option[RebasedCostsModel], postData: Option[RebasedCostsModel]): CalculationController = {

    val mockCalcConnector = mock[CalculatorConnector]
    val mockCalcElectionConstructor = mock[CalculationElectionConstructor]

    when(mockCalcConnector.fetchAndGetFormData[RebasedCostsModel](Matchers.eq("rebasedCosts"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(getData))

    lazy val data = CacheMap("form-id", Map("data" -> Json.toJson(postData.getOrElse(RebasedCostsModel("No", None)))))
    when(mockCalcConnector.saveFormData[RebasedCostsModel](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(data))

    new CalculationController {
      override val calcConnector: CalculatorConnector = mockCalcConnector
      override val calcElectionConstructor: CalculationElectionConstructor = mockCalcElectionConstructor
    }
  }

  // GET Tests
  "Calling the CalculationController.rebasedCosts" when {

    lazy val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/rebased-costs").withSession(SessionKeys.sessionId -> "12345")

    "not supplied with a pre-existing stored model" should {

      val target = setupTarget(None, None)
      lazy val result = target.rebasedCosts(fakeRequest)
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 200" in {
        status(result) shouldBe 200
      }

      "when no previous value is supplied return some HTML that" should {

        "contain some text and use the character set utf-8" in{
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "have the title 'Calculate your Capital Gains Tax" in {
          document.getElementsByTag("h1").text shouldBe "Calculate your Capital Gains Tax"
        }

        "have the question 'Did you pay for the valuation?" in {
          document.getElementsByTag("legend").text shouldBe "Did you pay for the valuation?"
        }

        "display the correct wording for radio option `yes`" in {
          document.body.getElementById("hasRebasedCosts-yes").parent.text shouldEqual Messages("calc.base.yes")
        }

        "display the correct wording for radio option `no`" in {
          document.body.getElementById("hasRebasedCosts-no").parent.text shouldEqual Messages("calc.base.no")
        }

        "contain a hidden component with an input box" in {
          document.body.getElementById("hidden").html should include ("input")
        }

        "have a back link" in {
          document.getElementById("back-link").tagName() shouldBe "a"
        }

        "have a continue button" in {
          document.getElementById("continue-button").tagName() shouldBe "button"
        }

        "have no auto selected option and an empty input field" in {
          document.getElementById("hasRebasedCosts-yes").parent.classNames().contains("selected") shouldBe false
          document.getElementById("hasRebasedCosts-no").parent.classNames().contains("selected") shouldBe false
          document.getElementById("rebasedCosts").attr("value") shouldBe ""
        }
      }

      "when a previous value is supplied return some HTML that" should {

        val target = setupTarget(Some(RebasedCostsModel("Yes", Some(1500))), None)
        lazy val result = target.rebasedCosts(fakeRequest)
        lazy val document = Jsoup.parse(bodyOf(result))

        "have an auto selected option and a filled input field" in {
          document.getElementById("hasRebasedCosts-yes").parent.classNames().contains("selected") shouldBe true
          document.getElementById("rebasedCosts").attr("value") shouldBe "1500"
        }
      }
    }
  }

  // POST Tests
  "In CalculationController calling the .submitRebasedCosts action" when {

    def buildRequest(body: (String, String)*): FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest("POST", "/calculate-your-capital-gains/rebased-costs")
      .withSession(SessionKeys.sessionId -> "12345")
      .withFormUrlEncodedBody(body: _*)

    def executeTargetWithMockData(selection: String, amount: String): Future[Result] = {

      lazy val fakeRequest = buildRequest(("hasRebasedCosts", selection), ("rebasedCosts", amount))

      val numeric = "(0-9*)".r
      val mockData = amount match {
        case numeric(money) => new RebasedCostsModel(selection, Some(BigDecimal(money)))
        case _ => new RebasedCostsModel(selection, None)
      }

      val target = setupTarget(None, Some(mockData))
      target.submitRebasedCosts(fakeRequest)
    }

    "submitting a valid form with no costs" should {
      lazy val result = executeTargetWithMockData("No", "")

      "return a 303" in {
        status(result) shouldBe 303
      }
    }

    "submitting a valid form with costs" should {
      lazy val result = executeTargetWithMockData("Yes", "1000")

      "return a 303" in {
        status(result) shouldBe 303
      }
    }

    "submitting an invalid form with 'Yes' and a value of 'fhu39awd8'" should {

      lazy val result = executeTargetWithMockData("Yes", "fhu39awd8")
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 400" in {
        status(result) shouldBe 400
      }

      "return HTML that displays the error message " in {
        document.select("div#hidden span.error-notification").text shouldEqual "Real number value expected"
      }
    }

    "submitting an invalid form with 'Yes' and a value of '-200'" should {

      lazy val result = executeTargetWithMockData("Yes", "-200")
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 400" in {
        status(result) shouldBe 400
      }

      "return HTML that displays the error message " in {
        document.select("div#hidden span.error-notification").text shouldEqual Messages("calc.rebasedCosts.errorNegative")
      }
    }

    "submitting an invalid form with 'Yes' and an empty value" should {

      lazy val result = executeTargetWithMockData("Yes", "")
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 400" in {
        status(result) shouldBe 400
      }

      "return HTML that displays the error message " in {
        document.select("div#hidden span.error-notification").text shouldEqual Messages("calc.rebasedCosts.error.no.value.supplied")
      }
    }

    "submitting an invalid form with 'Yes' and a value of 1.111" should {

      lazy val result = executeTargetWithMockData("Yes", "1.111")
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 400" in {
        status(result) shouldBe 400
      }

      "return HTML that displays the error message " in {
        document.select("div#hidden span.error-notification").text shouldEqual Messages("calc.rebasedCosts.errorDecimalPlaces")
      }
    }
  }
}
