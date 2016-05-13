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

class AcquisitionValueSpec extends UnitSpec with WithFakeApplication with MockitoSugar {

  implicit val hc = new HeaderCarrier()

  def setupTarget(
                   getData: Option[AcquisitionValueModel],
                   postData: Option[AcquisitionValueModel],
                   acquisitionDateModel: Option[AcquisitionDateModel] = None): CalculationController = {

    val mockCalcConnector = mock[CalculatorConnector]
    val mockCalcElectionConstructor = mock[CalculationElectionConstructor]

    when(mockCalcConnector.fetchAndGetFormData[AcquisitionValueModel](Matchers.anyString())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(getData))

    when(mockCalcConnector.fetchAndGetValue[AcquisitionDateModel](Matchers.eq("acquisitionDate"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(acquisitionDateModel))

    lazy val data = CacheMap("form-id", Map("data" -> Json.toJson(postData.getOrElse(AcquisitionValueModel(0)))))
    when(mockCalcConnector.saveFormData[AcquisitionValueModel](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(data))

    new CalculationController {
      override val calcConnector: CalculatorConnector = mockCalcConnector
      override val calcElectionConstructor: CalculationElectionConstructor = mockCalcElectionConstructor
    }
  }

  // GET Tests
  "Calling the CalculationController.acquisitionValue" when {

    lazy val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/acquisition-value").withSession(SessionKeys.sessionId -> "12345")

    "not supplied with a pre-existing stored model" should {

      val target = setupTarget(None, None)
      lazy val result = target.acquisitionValue(fakeRequest)
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 200" in {
        status(result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "have the title 'How much did you pay for the property?'" in {
          document.title shouldEqual Messages("calc.acquisitionValue.question")
        }

        "have the heading Calculate your tax (non-residents) " in {
          document.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a 'Back' link " in {
          document.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
        }

        "have the question 'How much did you pay for the property?'" in {
          document.body.getElementsByTag("label").text should include (Messages("calc.acquisitionValue.question"))
        }

        "display an input box for the Acquisition Value" in {
          document.body.getElementById("acquisitionValue").tagName shouldEqual "input"
        }
        "have no value auto-filled into the input box" in {
          document.getElementById("acquisitionValue").attr("value") shouldEqual ""
        }
        "display a 'Continue' button " in {
          document.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
        }
      }
    }

    "supplied with a pre-existing stored model" should {

      val target = setupTarget(Some(AcquisitionValueModel(1000)), None)
      lazy val result = target.acquisitionValue(fakeRequest)
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
          document.getElementById("acquisitionValue").attr("value") shouldEqual "1000"
        }
      }
    }
  }

  // POST Tests
  "In CalculationController calling the .submitAcquisitionValue action" when {

    def buildRequest(body: (String, String)*): FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest("POST", "/calculate-your-capital-gains/acquisition-value")
      .withSession(SessionKeys.sessionId -> "12345")
      .withFormUrlEncodedBody(body: _*)

    def executeTargetWithMockData(data: String, mockDate: Option[AcquisitionDateModel]): Future[Result] = {
      lazy val fakeRequest = buildRequest(("acquisitionValue", data))
      val mockData = data match {
        case "" => None
        case _ => Some(AcquisitionValueModel(BigDecimal(data)))
      }
      val target = setupTarget(None, mockData, mockDate)
      target.submitAcquisitionValue(fakeRequest)
    }

    s"return a 303 to ${routes.CalculationController.improvements()}" in {

      val acquisitionDateModelYesAfterStartDate = new AcquisitionDateModel("Yes", Some(10), Some(10), Some(2017))
      lazy val result = executeTargetWithMockData("1000", Some(acquisitionDateModelYesAfterStartDate))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(s"${routes.CalculationController.improvements()}")
    }

    "submitting a valid form with a date before 5 5 2015" should {

      val date = new AcquisitionDateModel("Yes", Some(10), Some(10), Some(2010))
      lazy val result = executeTargetWithMockData("1000", Some(date))

      s"return a 303 to ${routes.CalculationController.rebasedValue()}" in {
        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some(s"${routes.CalculationController.rebasedValue()}")
      }
    }

    "submitting a valid form with No date supplied" should {

      val noDate = new AcquisitionDateModel("No", None, None, None)
      lazy val result = executeTargetWithMockData("1000", Some(noDate))

      s"return a 303 to ${routes.CalculationController.rebasedValue()}" in {
        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some(s"${routes.CalculationController.rebasedValue()}")
      }
    }

    "submitting an invalid form with no value" should {

      lazy val result = executeTargetWithMockData("", None)

      "return a 400" in {
        status(result) shouldBe 400
      }
    }

    "submitting an invalid form with a negative value" should {

      lazy val result = executeTargetWithMockData("-1000", None)

      "return a 400" in {
        status(result) shouldBe 400
      }
    }

    "submitting an invalid form with value 1.111" should {

      lazy val result = executeTargetWithMockData("1.111", None)
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 400" in {
        status(result) shouldBe 400
      }

      s"fail with message ${Messages("calc.acquisitionValue.errorDecimalPlaces")}" in {
        document.getElementsByClass("error-notification").text should include (Messages("calc.acquisitionValue.errorDecimalPlaces"))
      }
    }
  }
}
