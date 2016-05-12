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

class AnnualExemptAmountSpec extends UnitSpec with WithFakeApplication with MockitoSugar {

  implicit val hc = new HeaderCarrier()

  def setupTarget(
                   getData: Option[AnnualExemptAmountModel],
                   postData: Option[AnnualExemptAmountModel],
                   customerType: String = "individual",
                   disabledTrustee: String = ""
                 ): CalculationController = {

    val mockCalcConnector = mock[CalculatorConnector]
    val mockCalcElectionConstructor = mock[CalculationElectionConstructor]

    when(mockCalcConnector.fetchAndGetValue[DisabledTrusteeModel](Matchers.eq("disabledTrustee"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(Some(DisabledTrusteeModel(disabledTrustee))))

    when(mockCalcConnector.fetchAndGetFormData[CustomerTypeModel](Matchers.eq("customerType"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(Some(CustomerTypeModel(customerType))))

    when(mockCalcConnector.fetchAndGetFormData[AnnualExemptAmountModel](Matchers.eq("annualExemptAmount"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(getData))

    lazy val data = CacheMap("form-id", Map("data" -> Json.toJson(postData.getOrElse(AnnualExemptAmountModel(0)))))
    when(mockCalcConnector.saveFormData[AnnualExemptAmountModel](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(data))

    new CalculationController {
      override val calcConnector: CalculatorConnector = mockCalcConnector
      override val calcElectionConstructor: CalculationElectionConstructor = mockCalcElectionConstructor
    }
  }

  // GET Tests
  "Calling the CalculationController.annualExemptAmount action" when {

    lazy val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/allowance").withSession(SessionKeys.sessionId -> "12345")

    "not supplied with a pre-existing stored model" should {

      val target = setupTarget(None, None)
      lazy val result = target.annualExemptAmount(fakeRequest)
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 200" in {
        status(result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "have the title 'How much of your Capital Gains Tax allowance have you got left?'" in {
          document.title shouldEqual Messages("calc.annualExemptAmount.question")
        }

        "have the heading Calculate your tax (non-residents) " in {
          document.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a 'Back' link " in {
          document.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
        }

        "have the question 'How much of your Capital Gains Tax allowance have you got left?' as the legend of the input" in {
          document.body.getElementsByTag("label").text should include(Messages("calc.annualExemptAmount.question"))
        }

        "display an input box for the Annual Exempt Amount" in {
          document.body.getElementById("annualExemptAmount").tagName() shouldEqual "input"
        }

        "have no value auto-filled into the input box" in {
          document.getElementById("annualExemptAmount").attr("value") shouldBe empty
        }

        "display a 'Continue' button " in {
          document.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
        }

        "should contain a Read more sidebar with a link to CGT allowances" in {
          document.select("aside h2").text shouldBe Messages("calc.common.readMore")
          document.select("aside a").text shouldBe Messages("calc.annualExemptAmount.link.one")
        }
      }
    }

    "supplied with a pre-existing stored model" should {

      val target = setupTarget(Some(AnnualExemptAmountModel(1000)), None)
      lazy val result = target.annualExemptAmount(fakeRequest)
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
          document.getElementById("annualExemptAmount").attr("value") shouldEqual ("1000")
        }
      }
    }
  }

  // POST Tests
  "In CalculationController calling the .submitAnnualExemptAmount action" when {

    def buildRequest(body: (String, String)*): FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest("POST", "/calculate-your-capital-gains/allowance")
      .withSession(SessionKeys.sessionId -> "12345")
      .withFormUrlEncodedBody(body: _*)

    def executeTargetWithMockData(amount: String, customerType: String, disabledTrustee: String): Future[Result] = {
      lazy val fakeRequest = buildRequest(("annualExemptAmount", amount))
      val mockData = amount match {
        case "" => AnnualExemptAmountModel(0)
        case _ => AnnualExemptAmountModel(BigDecimal(amount))
      }
      val target = setupTarget(None, Some(mockData), customerType, disabledTrustee)
      target.submitAnnualExemptAmount(fakeRequest)
    }

    "submitting a valid form" should {

      lazy val result = executeTargetWithMockData("1000", "individual", "")

      "return a 303" in {
        status(result) shouldBe 303
      }
    }

    "submitting an invalid form with no value" should {

      lazy val result = executeTargetWithMockData("", "individual", "")

      "return a 400" in {
        status(result) shouldBe 400
      }
    }

    "submitting an valid form below the maximum value for a non-vulnerable trustee" should {

      lazy val result = executeTargetWithMockData("5550", "trustee", "No")

      "return a 303" in {
        status(result) shouldBe 303
      }
    }

    "submitting an valid form when no customer type selected (impossible - for coverage only)" should {

      lazy val result = executeTargetWithMockData("1", "", "No")

      "return a 303" in {
        status(result) shouldBe 303
      }
    }

    "submitting a value above the maximum amount for a non-vulnerable trustee" should {

      lazy val result = executeTargetWithMockData("5550.01", "trustee", "No") //failing

      "return a 400" in {
        status(result) shouldBe 400
      }
    }

    "submitting an invalid form above the maximum value for a vulnerable trustee" should {

      lazy val result = executeTargetWithMockData("11100.01", "trustee", "Yes")

      "return a 400" in {
        status(result) shouldBe 400
      }
    }

    "submitting an invalid form above the maximum value for a non-Trustee customer type" should {

      lazy val result = executeTargetWithMockData("11100.01", "individual", "")

      "return a 400" in {
        status(result) shouldBe 400
      }
    }

    "submitting an invalid form below the minimum" should {

      lazy val result = executeTargetWithMockData("-1000", "individual", "")

      "return a 400" in {
        status(result) shouldBe 400
      }
    }

    "submitting an invalid form with value 1.111" should {

      lazy val result = executeTargetWithMockData("1.111", "individual", "")
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 400" in {
        status(result) shouldBe 400
      }

      s"fail with message ${Messages("calc.annualExemptAmount.errorDecimalPlaces")}" in {
        document.getElementsByClass("error-notification").text should include(Messages("calc.annualExemptAmount.errorDecimalPlaces"))
      }
    }
  }
}
