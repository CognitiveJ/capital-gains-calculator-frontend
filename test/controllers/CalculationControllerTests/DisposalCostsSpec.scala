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

class DisposalCostsSpec extends UnitSpec with WithFakeApplication with MockitoSugar {

  implicit val hc = new HeaderCarrier()

  def setupTarget(getData: Option[DisposalCostsModel],
                  postData: Option[DisposalCostsModel],
                  acquisitionDate: Option[AcquisitionDateModel],
                  rebasedData: Option[RebasedValueModel] = None): CalculationController = {

    val mockCalcConnector = mock[CalculatorConnector]
    val mockCalcElectionConstructor = mock[CalculationElectionConstructor]

    when(mockCalcConnector.fetchAndGetFormData[DisposalCostsModel](Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(getData))

    when(mockCalcConnector.fetchAndGetFormData[AcquisitionDateModel](Matchers.eq("acquisitionDate"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(acquisitionDate))

    when(mockCalcConnector.fetchAndGetFormData[RebasedValueModel](Matchers.eq("rebasedValue"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(rebasedData))

    lazy val data = CacheMap("form-id", Map("data" -> Json.toJson(postData.getOrElse(DisposalCostsModel(None)))))
    when(mockCalcConnector.saveFormData[DisposalCostsModel](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(data))

    new CalculationController {
      override val calcConnector: CalculatorConnector = mockCalcConnector
      override val calcElectionConstructor: CalculationElectionConstructor = mockCalcElectionConstructor
    }
  }

  //GET Tests
  "In CalculationController calling the .disposalCosts action " should {

    lazy val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/rebased-costs").withSession(SessionKeys.sessionId -> "12345")

    "not supplied with a pre-existing stored model" should {

      val target = setupTarget(None, None, None, None)
      lazy val result = target.disposalCosts(fakeRequest)
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 200" in {
        status(result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          charset(result) shouldBe Some("utf-8")
        }

        "have the title 'How much did you pay in costs when you stopped being the property owner?'" in {
          document.getElementsByTag("title").text shouldBe Messages("calc.disposalCosts.question")
        }

        "have a back link" in {
          document.getElementById("back-link").text shouldEqual Messages("calc.base.back")
        }

        "have the heading 'Calculate your tax (non-residents)'" in {
          document.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a monetary field that" should {

          "have the title 'How much did you pay in costs when you became the property owner?'" in {
            document.select("label[for=disposalCosts]").text should include(Messages("calc.disposalCosts.question"))
          }

          "have an input box for the disposal costs" in {
            document.getElementById("disposalCosts").tagName shouldBe "input"
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

      val target = setupTarget(Some(DisposalCostsModel(Some(1000))), None, None, None)
      lazy val result = target.disposalCosts(fakeRequest)
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
          document.getElementById("disposalCosts").attr("value") shouldEqual ("1000")
        }
      }
    }
  }

  //POST Tests
  "In CalculationController calling the .submitDisposalCosts action" when {

    def buildRequest(body: (String, String)*): FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest("POST", "/calculate-your-capital-gains/disposal-costs")
      .withSession(SessionKeys.sessionId -> "12345")
      .withFormUrlEncodedBody(body: _*)

    def executeTargetWithMockData(amount: String,
                                  acquisitionDate: AcquisitionDateModel,
                                  rebasedData: Option[RebasedValueModel] = None): Future[Result] = {

      lazy val fakeRequest = buildRequest(("disposalCosts", amount))

      val numeric = "(0-9*)".r
      val mockData = amount match {
        case numeric(money) => new DisposalCostsModel(Some(BigDecimal(money)))
        case _ => new DisposalCostsModel(None)
      }

      val target = setupTarget(None, Some(mockData), Some(acquisitionDate), rebasedData)
      target.submitDisposalCosts(fakeRequest)
    }

    "submitting a valid form when any acquisition date has been supplied but no property was revalued" should {

      lazy val result = executeTargetWithMockData("1000", AcquisitionDateModel("Yes", Some(12), Some(3), Some(2016)))

      "return a 303" in {
        status(result) shouldBe 303
      }

      s"redirect to ${routes.CalculationController.privateResidenceRelief()}" in {
        redirectLocation(result) shouldBe Some(s"${routes.CalculationController.privateResidenceRelief()}")
      }
    }

    "submitting a valid form when no acquisition date has been supplied but a property was revalued" should {
      val rebased = RebasedValueModel("Yes", Some(BigDecimal(1000)))
      lazy val result = executeTargetWithMockData("1000", AcquisitionDateModel("No", None, None, None), Some(rebased))

      "return a 303" in {
        status(result) shouldBe 303
      }

      s"redirect to ${routes.CalculationController.privateResidenceRelief()}" in {
        redirectLocation(result) shouldBe Some(s"${routes.CalculationController.privateResidenceRelief()}")
      }
    }

    "submitting a valid form when no acquisition date has been supplied and no property was revalued" should {
      lazy val result = executeTargetWithMockData("1000", AcquisitionDateModel("No", None, None, None))

      "return a 303" in {
        status(result) shouldBe 303
      }

      s"redirect to ${routes.CalculationController.entrepreneursRelief()}" in {
        redirectLocation(result) shouldBe Some(s"${routes.CalculationController.entrepreneursRelief()}")
      }
    }

    "submitting an valid form with no value" should {

      lazy val result = executeTargetWithMockData("", AcquisitionDateModel("No", None, None, None))

      "return a 303" in {
        status(result) shouldBe 303
      }
    }

    "submitting an invalid form with a negative value of -432" should {

      lazy val result = executeTargetWithMockData("-432", AcquisitionDateModel("No", None, None, None))
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 400" in {
        status(result) shouldBe 400
      }

      "display the error message 'Disposal costs can't be negative'" in {
        document.select("div label span.error-notification").text shouldEqual Messages("calc.disposalCosts.errorNegativeNumber")
      }
    }

    "submitting an invalid form with a value that has more than two decimal places" should {

      lazy val result = executeTargetWithMockData("432.222", AcquisitionDateModel("No", None, None, None))
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 400" in {
        status(result) shouldBe 400
      }

      "display the error message 'The costs have too many decimal places'" in {
        document.select("div label span.error-notification").text shouldEqual Messages("calc.disposalCosts.errorDecimalPlaces")
      }
    }

    "submitting an invalid form with a value that is negative and has more than two decimal places" should {

      lazy val result = executeTargetWithMockData("-432.9876", AcquisitionDateModel("No", None, None, None))
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 400" in {
        status(result) shouldBe 400
      }

      "display the error message 'Disposal costs cannot be negative' and 'The costs have too many decimal places'" in {
        document.select("div label span.error-notification").text shouldEqual (Messages("calc.disposalCosts.errorNegativeNumber") + " " + Messages("calc.disposalCosts.errorDecimalPlaces"))
      }
    }
  }
}
