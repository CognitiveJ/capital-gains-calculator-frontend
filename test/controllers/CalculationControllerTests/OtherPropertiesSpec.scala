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

class OtherPropertiesSpec extends UnitSpec with WithFakeApplication with MockitoSugar {

  implicit val hc = new HeaderCarrier()

  def setupTarget(getData: Option[OtherPropertiesModel], postData: Option[OtherPropertiesModel]): CalculationController = {

    val mockCalcConnector = mock[CalculatorConnector]
    val mockCalcElectionConstructor = mock[CalculationElectionConstructor]

    when(mockCalcConnector.fetchAndGetFormData[OtherPropertiesModel](Matchers.anyString())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(getData))

    lazy val data = CacheMap("form-id", Map("data" -> Json.toJson(postData.getOrElse(OtherPropertiesModel("", Some(0))))))
    when(mockCalcConnector.saveFormData[OtherPropertiesModel](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(data))

    new CalculationController {
      override val calcConnector: CalculatorConnector = mockCalcConnector
      override val calcElectionConstructor: CalculationElectionConstructor = mockCalcElectionConstructor
    }
  }

  // GET Tests
  "Calling the CalculationController.otherProperties" when {

    lazy val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/other-properties").withSession(SessionKeys.sessionId -> "12345")

    "not supplied with a pre-existing stored model" should {

      val target = setupTarget(None, None)
      lazy val result = target.otherProperties(fakeRequest)
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 200" in {
        status(result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "have the title 'Did you sell or give away any other properties in that tax year?'" in {
          document.title shouldEqual Messages("calc.otherProperties.question")
        }

        "have the heading Calculate your tax (non-residents) " in {
          document.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a 'Back' link " in {
          document.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
        }

        "have the question 'Did you sell or give away any other properties in that tax year?' as the legend of the input" in {
          document.body.getElementsByTag("legend").text shouldEqual Messages("calc.otherProperties.question")
        }

        "display a radio button with the option `Yes`" in {
          document.body.getElementById("otherProperties-yes").parent.text shouldEqual Messages("calc.base.yes")
        }

        "display a radio button with the option `No`" in {
          document.body.getElementById("otherProperties-no").parent.text shouldEqual Messages("calc.base.no")
        }

        "display a 'Continue' button " in {
          document.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
        }
      }
    }

    "supplied with a model that already contains data" should {

      val target = setupTarget(Some(OtherPropertiesModel("Yes", Some(2100))), None)
      lazy val result = target.otherProperties(fakeRequest)
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 200" in {
        status(result) shouldBe 200
      }

      "return some HTML that" should {
        "contain some text and use the character set utf-8" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "have the radio option `Yes` selected by default" in {
          document.body.getElementById("otherProperties-yes").parent.classNames().contains("selected") shouldBe true
        }

        "have the value 2100 auto filled" in {
          document.body().getElementById("otherPropertiesAmt").attr("value") shouldBe "2100"
        }
      }
    }
  }

  // POST Tests
  "In CalculationController calling the .submitOtherProperties action" when {

    def buildRequest(body: (String, String)*): FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest("POST", "/calculate-your-capital-gains/other-properties")
      .withSession(SessionKeys.sessionId -> "12345")
      .withFormUrlEncodedBody(body: _*)

    def executeTargetWithMockData(selection: String, amount: String): Future[Result] = {
      lazy val fakeRequest = buildRequest(("otherProperties", selection), ("otherPropertiesAmt", amount))
      val mockData = amount match {
        case "" => OtherPropertiesModel(selection, None)
        case _ => OtherPropertiesModel(selection, Some(BigDecimal(amount)))
      }
      val target = setupTarget(None, Some(mockData))
      target.submitOtherProperties(fakeRequest)
    }

    "submitting a valid form with 'Yes' and an amount" should {

      lazy val result = executeTargetWithMockData("Yes", "2100")

      "return a 303" in {
        status(result) shouldBe 303
      }
    }

    "submitting a valid form with 'No'" should {

      lazy val result = executeTargetWithMockData("No", "")

      "return a 303" in {
        status(result) shouldBe 303
      }
    }

    "submitting an form with no data" should {

      lazy val result = executeTargetWithMockData("", "")

      "return a 400" in {
        status(result) shouldBe 400
      }
    }

    "submitting an invalid form with 'Yes' selection and a no amount" should {

      lazy val result = executeTargetWithMockData("Yes", "")

      "return a 400" in {
        status(result) shouldBe 400
      }
    }

    "submitting an invalid form with 'Yes' selection and an amount with three decimal places" should {

      lazy val result = executeTargetWithMockData("Yes", "1000.111")
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 400" in {
        status(result) shouldBe 400
      }

      s"fail with message ${Messages("calc.otherProperties.errorDecimalPlaces")}" in {
        document.getElementsByClass("error-notification").text should include (Messages("calc.otherProperties.errorDecimalPlaces"))
      }
    }

    "submitting an invalid form with 'Yes' selection and a negative amount" should {

      lazy val result = executeTargetWithMockData("Yes", "-1000")
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 400" in {
        status(result) shouldBe 400
      }

      s"fail with message ${Messages("calc.otherProperties.errorNegative")}" in {
        document.getElementsByClass("error-notification").text should include (Messages("calc.otherProperties.errorNegative"))
      }
    }
  }
}
