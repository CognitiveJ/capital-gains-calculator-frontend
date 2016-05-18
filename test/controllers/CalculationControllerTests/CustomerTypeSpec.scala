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
import controllers.{routes, CalculationController}
import play.api.mvc.Result

class CustomerTypeSpec extends UnitSpec with WithFakeApplication with MockitoSugar {

  implicit val hc = new HeaderCarrier()

  def setupTarget(getData: Option[CustomerTypeModel], postData: Option[CustomerTypeModel]): CalculationController = {

    val mockCalcConnector = mock[CalculatorConnector]
    val mockCalcElectionConstructor = mock[CalculationElectionConstructor]

    when(mockCalcConnector.fetchAndGetFormData[CustomerTypeModel](Matchers.anyString())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(getData))

    lazy val data = CacheMap("form-id", Map("data" -> Json.toJson(postData.getOrElse(CustomerTypeModel("")))))
    when(mockCalcConnector.saveFormData[CustomerTypeModel](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(data))

    new CalculationController {
      override val calcConnector: CalculatorConnector = mockCalcConnector
      override val calcElectionConstructor: CalculationElectionConstructor = mockCalcElectionConstructor
    }
  }

  // GET Tests
  "Calling the CalculationController.customerType" when {

    lazy val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/customer-type").withSession(SessionKeys.sessionId -> "12345")

    "not supplied with a pre-existing stored model" should {

      val target = setupTarget(None, None)
      lazy val result = target.customerType(fakeRequest)
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 200" in {
        status(result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "have the title 'Who owned the property?'" in {
          document.title shouldEqual Messages("calc.customerType.question")
        }

        "have the heading Calculate your tax (non-residents) " in {
          document.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
        }

        s"have a 'Back' link to ${routes.IntroductionController.introduction}" in {
          document.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
          document.body.getElementById("back-link").attr("href") shouldEqual routes.IntroductionController.introduction.toString()
        }

        "have the question 'Who owned the property?' as the legend of the input" in {
          document.body.getElementsByTag("legend").text shouldEqual Messages("calc.customerType.question")
        }

        "display a radio button with the option `individual`" in {
          document.body.getElementById("customerType-individual").parent.text shouldEqual Messages("calc.customerType.individual")
        }

        "have the radio option `individual` not selected by default" in {
          document.body.getElementById("customerType-individual").parent.classNames().contains("selected") shouldBe false
        }

        "display a radio button with the option `trustee`" in {
          document.body.getElementById("customerType-trustee").parent.text shouldEqual Messages("calc.customerType.trustee")
        }

        "display a radio button with the option `personal representative`" in {
          document.body.getElementById("customerType-personalrep").parent.text shouldEqual Messages("calc.customerType.personalRep")
        }

        "display a 'Continue' button " in {
          document.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
        }
      }
    }

    "supplied with a pre-existing stored model" should {

      val target = setupTarget(Some(CustomerTypeModel("individual")), None)
      lazy val result = target.customerType(fakeRequest)
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 200" in {
        status(result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "have the radio option `individual` selected by default" in {
          document.body.getElementById("customerType-individual").parent.classNames().contains("selected") shouldBe true
        }
      }
    }
  }

  // POST Tests
  "In CalculationController calling the .submitCustomerType action" when {

    def buildRequest(body: (String, String)*): FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest("POST", "/calculate-your-capital-gains/customer-type")
      .withSession(SessionKeys.sessionId -> "12345")
      .withFormUrlEncodedBody(body: _*)

    def executeTargetWithMockData(data: String): Future[Result] = {
      lazy val fakeRequest = buildRequest(("customerType", data))
      val mockData = new CustomerTypeModel(data)
      val target = setupTarget(None, Some(mockData))
      target.submitCustomerType(fakeRequest)
    }

    "submitting a valid form with 'individual'" should {

      lazy val result = executeTargetWithMockData("individual")

      "return a 303" in {
        status(result) shouldBe 303
      }
    }

    "submitting a valid form with 'trustee'" should {

      lazy val result = executeTargetWithMockData("trustee")

      "return a 303" in {
        status(result) shouldBe 303
      }
    }

    "submitting a valid form with 'personalRep'" should {

      lazy val result = executeTargetWithMockData("personalRep")

      "return a 303" in {
        status(result) shouldBe 303
      }
    }

    "submitting an invalid form with no content" should {

      lazy val result = executeTargetWithMockData("")
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 400" in {
        status(result) shouldBe 400
      }

      "display a visible Error Summary field" in {
        document.getElementById("error-summary-display").hasClass("error-summary--show")
      }

      "link to the radio field set in Error Summary" in {
        document.getElementById("customerType-error-summary").attr("href") should include("#customerType")
      }
    }

    "submitting an invalid form with incorrect content" should {

      lazy val result = executeTargetWithMockData("invalid-user")

      "return a 400" in {
        status(result) shouldBe 400
      }
    }
  }
}
