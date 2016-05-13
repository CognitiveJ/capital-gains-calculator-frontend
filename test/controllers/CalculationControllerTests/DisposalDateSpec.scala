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

class DisposalDateSpec extends UnitSpec with WithFakeApplication with MockitoSugar {

  implicit val hc = new HeaderCarrier()

  def setupTarget(getData: Option[DisposalDateModel], postData: Option[DisposalDateModel]): CalculationController = {

    val mockCalcConnector = mock[CalculatorConnector]
    val mockCalcElectionConstructor = mock[CalculationElectionConstructor]

    when(mockCalcConnector.fetchAndGetFormData[DisposalDateModel](Matchers.anyString())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(getData))

    lazy val data = CacheMap("form-id", Map("data" -> Json.toJson(postData.getOrElse(DisposalDateModel(1, 1, 1)))))
    when(mockCalcConnector.saveFormData[DisposalDateModel](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(data))

    new CalculationController {
      override val calcConnector: CalculatorConnector = mockCalcConnector
      override val calcElectionConstructor: CalculationElectionConstructor = mockCalcElectionConstructor
    }
  }

  // GET Tests
  "Calling the CalculationController.disposalDate" when {

    lazy val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/disposal-date").withSession(SessionKeys.sessionId -> "12345")

    "not supplied with a pre-existing stored model" should {

      val target = setupTarget(None, None)
      lazy val result = target.disposalDate(fakeRequest)
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 200" in {
        status(result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          charset(result) shouldBe Some("utf-8")
        }

        "have the title 'When did you sign the contract that made someone else the owner?'" in {
          document.title shouldEqual Messages("calc.disposalDate.question")
        }

        "have the heading Calculate your tax (non-residents) " in {
          document.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a 'Back' link " in {
          document.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
        }

        s"have the question '${Messages("calc.disposalDate.question")}'" in {
          document.body.getElementsByTag("fieldset").text should include (Messages("calc.disposalDate.question"))
        }

        "display three input boxes with labels Day, Month and Year respectively" in {
          document.select("label[for=disposalDate.day]").text shouldEqual Messages("calc.common.date.fields.day")
          document.select("label[for=disposalDate.month]").text shouldEqual Messages("calc.common.date.fields.month")
          document.select("label[for=disposalDate.year]").text shouldEqual Messages("calc.common.date.fields.year")
        }

        "display a 'Continue' button " in {
          document.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
        }
      }
    }

    "supplied with a model already filled with data" should {

      val target = setupTarget(Some(DisposalDateModel(10, 12, 2016)), None)
      lazy val result = target.disposalDate(fakeRequest)
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 200" in {
        status(result) shouldBe 200
      }

      "return some HTML that" should {
        "contain some text and use the character set utf-8" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "be pre-populated with the date 10, 12, 2016" in {
          document.body.getElementById("disposalDate.day").attr("value") shouldEqual "10"
          document.body.getElementById("disposalDate.month").attr("value") shouldEqual "12"
          document.body.getElementById("disposalDate.year").attr("value") shouldEqual "2016"
        }
      }
    }
  }

  // POST Tests
  "In CalculationController calling the .submitDisposalDate action" when {

    val numeric = "([0-9]+)".r

    def getIntOrDefault(input: String): Int = input match {
      case numeric(number) => number.toInt
      case _ => 0
    }

    def buildRequest(body: (String, String)*): FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest("POST", "/calculate-your-capital-gains/disposal-date")
      .withSession(SessionKeys.sessionId -> "12345")
      .withFormUrlEncodedBody(body: _*)

    def executeTargetWithMockData(day: String, month: String, year: String): Future[Result] = {
      lazy val fakeRequest = buildRequest(("disposalDate.day", day), ("disposalDate.month", month), ("disposalDate.year", year))
      val mockData = new DisposalDateModel(getIntOrDefault(day), getIntOrDefault(month), getIntOrDefault(year))
      val target = setupTarget(None, Some(mockData))
      target.submitDisposalDate(fakeRequest)
    }

    "submitting a valid date 31/01/2016" should {

      lazy val result = executeTargetWithMockData("31", "1", "2016")

      "return a 303" in {
        status(result) shouldBe 303
      }

      s"redirect to ${routes.CalculationController.disposalValue()}" in {
        redirectLocation(result) shouldBe Some(s"${routes.CalculationController.disposalValue()}")
      }
    }

    "submitting a valid leap year date 29/02/2016" should {

      lazy val result = executeTargetWithMockData("29", "2", "2016")

      "return a 303" in {
        status(result) shouldBe 303
      }

      s"redirect to ${routes.CalculationController.disposalValue()}" in {
        redirectLocation(result) shouldBe Some(s"${routes.CalculationController.disposalValue()}")
      }
    }

    "submitting an invalid leap year date 29/02/2017" should {

      lazy val result = executeTargetWithMockData("29", "2", "2017")
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 400" in {
        status(result) shouldBe 400
      }

      s"should error with message '${Messages("calc.common.date.error.invalidDate")}'" in {
        document.select(".error-notification").text should include (Messages("calc.common.date.error.invalidDate"))
      }
    }

    "submitting a day less than 1" should {

      lazy val result = executeTargetWithMockData("0", "2", "2017")
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 400" in {
        status(result) shouldBe 400
      }

      s"should error with message '${Messages("calc.common.date.error.day.lessThan1")}'" in {
        document.select(".error-notification").text should include (Messages("calc.common.date.error.invalidDate"))
      }
    }

    "submitting a day greater than 31" should {

      lazy val result = executeTargetWithMockData("32", "2", "2017")
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 400" in {
        status(result) shouldBe 400
      }

      s"should error with message '${Messages("calc.common.date.error.day.greaterThan31")}'" in {
        document.select(".error-notification").text should include (Messages("calc.common.date.error.invalidDate"))
      }
    }

    "submitting a month greater than 12" should {

      lazy val result = executeTargetWithMockData("31", "13", "2017")
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 400" in {
        status(result) shouldBe 400
      }

      s"should error with message '${Messages("calc.common.date.error.month.greaterThan12")}'" in {
        document.select(".error-notification").text should include (Messages("calc.common.date.error.invalidDate"))
      }
    }

    "submitting a month less than 1" should {

      lazy val result = executeTargetWithMockData("31", "0", "2017")
      lazy val document = Jsoup.parse(bodyOf(result))

      val testModel = new DisposalDateModel(31,0,2017)

      "return a 400" in {
        status(result) shouldBe 400
      }

      s"should error with message '${Messages("calc.common.date.error.month.lessThan1")}'" in {
        document.select(".error-notification").text should include (Messages("calc.common.date.error.invalidDate"))
      }
    }

    "submitting a day with no value" should {

      lazy val result = executeTargetWithMockData("", "12", "2017")
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 400" in {
        status(result) shouldBe 400
      }

      "should error with message 'calc.common.date.error.invalidDate'" in {
        document.select(".error-notification").text should include (Messages("calc.common.date.error.invalidDate"))
      }
    }

    "submitting a month with no value" should {

      lazy val result = executeTargetWithMockData("31", "", "2017")
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 400" in {
        status(result) shouldBe 400
      }

      "should error with message 'calc.common.date.error.invalidDate'" in {
        document.select(".error-notification").text should include (Messages("calc.common.date.error.invalidDate"))
      }
    }

    "submitting a year with no value" should {

      lazy val result = executeTargetWithMockData("31", "12", "")
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 400" in {
        status(result) shouldBe 400
      }

      "should error with message 'calc.common.date.error.invalidDate'" in {
        document.select(".error-notification").text should include (Messages("calc.common.date.error.invalidDate"))
      }
    }
  }
}
