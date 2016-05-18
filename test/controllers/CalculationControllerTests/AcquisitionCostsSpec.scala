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
import models.AcquisitionCostsModel
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

class AcquisitionCostsSpec extends UnitSpec with WithFakeApplication with MockitoSugar {

  implicit val hc = new HeaderCarrier()
  def setupTarget(getData: Option[AcquisitionCostsModel], postData: Option[AcquisitionCostsModel]): CalculationController = {

    val mockCalcConnector = mock[CalculatorConnector]
    val mockCalcElectionConstructor = mock[CalculationElectionConstructor]

    when(mockCalcConnector.fetchAndGetFormData[AcquisitionCostsModel](Matchers.anyString())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(getData))

    lazy val data = CacheMap("form-id", Map("data" -> Json.toJson(postData.getOrElse(AcquisitionCostsModel(None)))))
    when(mockCalcConnector.saveFormData[AcquisitionCostsModel](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(data))

    new CalculationController {
      override val calcConnector: CalculatorConnector = mockCalcConnector
      override val calcElectionConstructor: CalculationElectionConstructor = mockCalcElectionConstructor
    }
  }

  "In CalculationController calling the .acquisitionCosts action " should {

    lazy val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/acquisition-costs").withSession(SessionKeys.sessionId -> "12345")

    "not supplied with a pre-existing stored model" should {
      val target = setupTarget(None, None)
      lazy val result = target.acquisitionCosts(fakeRequest)
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 200" in {
        status(result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "have the title 'How much did you pay in costs when you became the property owner'" in {
          document.getElementsByTag("title").text shouldEqual Messages("calc.acquisitionCosts.question")
        }

        s"have a 'Back' link to ${routes.CalculationController.disposalValue}" in {
          document.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
          document.body.getElementById("back-link").attr("href") shouldEqual routes.CalculationController.disposalValue.toString()
        }

        "have the page heading 'Calculate your tax (non-residents)'" in {
          document.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a monetary field that" should {

          "have the title 'How much did you pay in costs when you became the property owner?'" in {
            document.select("label[for=acquisitionCosts]").text should include (Messages("calc.acquisitionCosts.question"))
          }

          "have the help text 'Costs include agent fees, legal fees and surveys'" in {
            document.select("span.form-hint").text shouldEqual Messages("calc.acquisitionCosts.helpText")
          }

          "have an input box for the acquisition costs" in {
            document.getElementById("acquisitionCosts").tagName shouldBe "input"
          }
        }

        "have a continue button that" should {

          "be a button element" in {
            document.getElementById("continue-button").tagName shouldBe "button"
          }

          "have the text 'Continue'" in {
            document.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
          }
        }
      }
    }

    "supplied with a pre-existing stored model" should {
      val testAcquisitionCostsModel = new AcquisitionCostsModel(Some(1000))
      val target = setupTarget(Some(testAcquisitionCostsModel), None)
      lazy val result = target.acquisitionCosts(fakeRequest)
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 200" in {
        status(result) shouldBe 200
      }

      "return some HTML that" should {
        "have the value 1000 auto-filled into the input box" in {
            document.getElementById("acquisitionCosts").attr("value") shouldEqual "1000"
        }
      }
    }
  }

  "In CalculationController calling the .submitAcquisitionCosts action" when {
    def buildRequest(body: (String, String)*): FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest("POST", "/calculate-your-capital-gains/acquisition-date")
      .withSession(SessionKeys.sessionId -> "12345")
      .withFormUrlEncodedBody(body: _*)

    def executeTargetWithMockData
    (
      acquisitionCosts: Option[BigDecimal] = None
    ): Future[Result] = {
      lazy val fakeRequest = buildRequest(
        ("acquisitionCosts", acquisitionCosts.getOrElse("").toString))
      val mockData = new AcquisitionCostsModel(acquisitionCosts)
      val target = setupTarget(None, Some(mockData))
      target.submitAcquisitionCosts(fakeRequest)
    }

    "submitting a valid form" should {

      "with value 1000" should {
        lazy val result = executeTargetWithMockData(Some(1000))

        "return a 303" in {
          status(result) shouldBe 303
        }

        s"redirect to ${routes.CalculationController.disposalCosts()}" in {
          redirectLocation(result) shouldBe Some(s"${routes.CalculationController.disposalCosts()}")
        }
      }

      "with no value" should {
        lazy val result = executeTargetWithMockData(None)

        "return a 303" in {
          status(result) shouldBe 303
        }

        s"redirect to ${routes.CalculationController.disposalCosts()}" in {
          redirectLocation(result) shouldBe Some(s"${routes.CalculationController.disposalCosts()}")
        }
      }
    }

    "submitting an invalid form" should {
      val testModel = new AcquisitionCostsModel(Some(0))

      "with value -1" should {
        lazy val result = executeTargetWithMockData(Some(-1))
        lazy val document = Jsoup.parse(bodyOf(result))

        "return a 400" in {
          status(result) shouldBe 400
        }

        s"fail with message ${Messages("calc.acquisitionCosts.errorNegative")}" in {
          document.getElementsByClass("error-notification").text should include (Messages("calc.acquisitionCosts.errorNegative"))
        }

        "display a visible Error Summary field" in {
          document.getElementById("error-summary-display").hasClass("error-summary--show")
        }

        "link to the invalid input box in Error Summary" in {
          document.getElementById("acquisitionCosts-error-summary").attr("href") should include ("#acquisitionCosts")
        }
      }

      "with value 1.111" should {
        lazy val result = executeTargetWithMockData(Some(1.111))
        lazy val document = Jsoup.parse(bodyOf(result))

        "return a 400" in {
          status(result) shouldBe 400
        }

        s"fail with message ${Messages("calc.acquisitionCosts.errorDecimalPlaces")}" in {
          document.getElementsByClass("error-notification").text should include(Messages("calc.acquisitionCosts.errorDecimalPlaces"))
        }

        "display a visible Error Summary field" in {
          document.getElementById("error-summary-display").hasClass("error-summary--show")
        }

        "link to the invalid input box in Error Summary" in {
          document.getElementById("acquisitionCosts-error-summary").attr("href") should include ("#acquisitionCosts")
        }
      }
    }
  }

}
