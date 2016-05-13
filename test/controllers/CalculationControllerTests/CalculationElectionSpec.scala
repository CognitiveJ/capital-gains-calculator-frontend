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

import common.TestModels
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

class CalculationElectionSpec extends UnitSpec with WithFakeApplication with MockitoSugar {

  implicit val hc = new HeaderCarrier()

  def setupTarget(getData: Option[CalculationElectionModel],
                  postData: Option[CalculationElectionModel],
                  summaryData: SummaryModel,
                  calc: Option[CalculationResultModel] = None): CalculationController = {

    val mockCalcConnector = mock[CalculatorConnector]
    val mockCalcElectionConstructor = mock[CalculationElectionConstructor]

    when(mockCalcConnector.createSummary(Matchers.any()))
      .thenReturn(summaryData)

    when(mockCalcElectionConstructor.generateElection(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
      .thenReturn(Seq(
        ("flat", "8000.00", "flat calculation",
          None, routes.CalculationController.otherReliefs().toString()),
        ("time", "8000.00", "time apportioned calculation",
          Some(Messages("calc.calculationElection.message.timeDate")), routes.CalculationController.otherReliefsTA().toString())))

    when(mockCalcConnector.calculateFlat(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(calc))
    when(mockCalcConnector.calculateTA(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(calc))
    when(mockCalcConnector.calculateRebased(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(calc))

    when(mockCalcConnector.fetchAndGetFormData[CalculationElectionModel](Matchers.anyString())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(getData))

    lazy val data = CacheMap("form-id", Map("data" -> Json.toJson(postData.getOrElse(CalculationElectionModel("")))))
    when(mockCalcConnector.saveFormData[CalculationElectionModel](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(data))

    new CalculationController {
      override val calcConnector: CalculatorConnector = mockCalcConnector
      override val calcElectionConstructor: CalculationElectionConstructor = mockCalcElectionConstructor
    }
  }

  // GET Tests
  "In CalculationController calling the .calculationElection action" when {

    lazy val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/calculation-election").withSession(SessionKeys.sessionId -> "12345")

    "supplied with no pre-existing data" should {

      val target = setupTarget(None, None, TestModels.summaryTrusteeTAWithoutAEA)
      lazy val result = target.calculationElection(fakeRequest)
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 200" in {
        status(result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set UTF-8" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "have the title Which method of calculation would you like?" in {
          document.title shouldEqual Messages("calc.calculationElection.question")
        }

        "have the heading Calculate your tax (non-residents) " in {
          document.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a 'Back' link " in {
          document.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
        }

        "have the paragraph You can decide what to base your Capital Gains Tax on. It affects how much you'll pay." in {
          document.body.getElementById("calculationElection").text shouldEqual Messages("calc.calculationElection.message")
        }

        "have a calculationElectionHelper for the option of a flat calculation rendered on the page" in {
          document.body.getElementById("calculationElection-flat").attr("value") shouldEqual "flat"
          document.body.getElementById("flat-para").text shouldEqual ("Based on " + "flat calculation")
        }

        "display a 'Continue' button " in {
          document.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
        }

        "display a concertina information box with 'They sometimes qualify for larger tax reliefs. This can lower the amount you owe or even reduce it to zero' as the content" in {
          document.select("summary span.summary").text shouldEqual Messages("calc.calculationElection.message.whyMore")
          document.select("div#details-content-0 p").text shouldEqual Messages("calc.calculationElection.message.whyMoreDetails")
        }

        "have no pre-selected option" in {
          document.body.getElementById("calculationElection-flat").parent.classNames().contains("selected") shouldBe false
        }
      }
    }

    "supplied with no pre-existing data and no acquisition date" should {

      val target = setupTarget(None, None, TestModels.summaryIndividualFlatWithAEA)
      lazy val result = target.calculationElection(fakeRequest)
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 200" in {
        status(result) shouldBe 200
      }

    }

    "supplied with no pre-existing data and an acquisition date after tax start date" should {

      val target = setupTarget(None, None, TestModels.summaryIndividualRebasedAcqDateAfter)
      lazy val result = target.calculationElection(fakeRequest)
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 200" in {
        status(result) shouldBe 200
      }

    }

    "supplied with no pre-existing data and an acquisition date before tax start date" should {

      val target = setupTarget(None, None, TestModels.summaryIndividualRebased)
      lazy val result = target.calculationElection(fakeRequest)
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 200" in {
        status(result) shouldBe 200
      }

    }

    "supplied with no pre-existing data and a None rebased value" should {

      val target = setupTarget(None, None, TestModels.summaryIndividualImprovementsNoRebasedModel)
      lazy val result = target.calculationElection(fakeRequest)
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 200" in {
        status(result) shouldBe 200
      }

    }

    "supplied with no pre-existing data and a rebased value with no acquisition date" should {

      val target = setupTarget(None, None, TestModels.summaryIndividualRebasedNoAcqDate)
      lazy val result = target.calculationElection(fakeRequest)
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 200" in {
        status(result) shouldBe 200
      }

    }

    "supplied with pre-existing data" should {

      val target = setupTarget(Some(CalculationElectionModel("flat")), None, TestModels.summaryTrusteeTAWithoutAEA)
      lazy val result = target.calculationElection(fakeRequest)
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 200" in {
        status(result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "have the stored value of flat calculation selected" in {
          document.body.getElementById("calculationElection-flat").parent.classNames().contains("selected") shouldBe true
        }
      }
    }
  }

  "In CalculationController calling the .submitCalculationElection action" when {

    def buildRequest(body: (String, String)*): FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest("POST", "/calculate-your-capital-gains/calculation-election")
      .withSession(SessionKeys.sessionId -> "12345")
      .withFormUrlEncodedBody(body: _*)

    def executeTargetWithMockData(data: String, calc: Option[CalculationResultModel], summary: SummaryModel): Future[Result] = {
      lazy val fakeRequest = buildRequest(("calculationElection", data))
      val mockData = new CalculationElectionModel(data)
      val target = setupTarget(None, Some(mockData), summary, calc)
      target.submitCalculationElection(fakeRequest)
    }

    "submitting a valid form with 'flat' selected" should {

      lazy val result = executeTargetWithMockData("flat", Some(TestModels.calcModelOneRate), TestModels.summaryTrusteeTAWithoutAEA)

      "return a 303" in {
        status(result) shouldBe 303
      }

      "redirect to the summary page" in {
        redirectLocation(result) shouldBe Some(s"${routes.CalculationController.summary}")
      }
    }

    "submitting a valid form with 'time' selected" should {

      lazy val result = executeTargetWithMockData("time", Some(TestModels.calcModelOneRate), TestModels.summaryIndividualAcqDateAfter)

      "return a 303" in {
        status(result) shouldBe 303
      }
    }

    "submitting a valid form with 'rebased' selected" should {

      lazy val result = executeTargetWithMockData("rebased", Some(TestModels.calcModelOneRate), TestModels.summaryIndividualFlatWithAEA)

      "return a 303" in {
        status(result) shouldBe 303
      }
    }

    "submitting a form with no data" should  {

      lazy val result = executeTargetWithMockData("", Some(TestModels.calcModelOneRate), TestModels.summaryIndividualImprovementsWithRebasedModel)
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 400" in {
        status(result) shouldBe 400
      }

      "display a visible Error Summary field" in {
        document.getElementById("error-summary-display").hasClass("error-summary--show")
      }

      "link to the invalid input box in Error Summary" in {
        document.getElementById("calculationElection-error-summary").attr("href") should include ("#calculationElection")
      }
    }

    "submitting a form with completely unrelated 'ew1234qwer'" should  {

      lazy val result = executeTargetWithMockData("ew1234qwer", Some(TestModels.calcModelOneRate), TestModels.summaryIndividualImprovementsNoRebasedModel)

      "return a 400" in {
        status(result) shouldBe 400
      }
    }

    "submitting an invalid form with an acquisition date after the tax start date" should {
      lazy val result = executeTargetWithMockData("", Some(TestModels.calcModelOneRate), TestModels.summaryIndividualAcqDateAfter)

      "return a 400" in {
        status(result) shouldBe 400
      }
    }

    "submitting an invalid form with no acquisition date" should {
      lazy val result = executeTargetWithMockData("", Some(TestModels.calcModelOneRate), TestModels.summaryIndividualFlatWithoutAEA)

      "return a 400" in {
        status(result) shouldBe 400
      }
    }

    "submitting an invalid form with none rebased value" should {
      lazy val result = executeTargetWithMockData("", Some(TestModels.calcModelOneRate), TestModels.summaryIndividualImprovementsNoRebasedModel)

      "return a 400" in {
        status(result) shouldBe 400
      }
    }

    "submitting an invalid form with a rebased value and no acquisition date" should {
      lazy val result = executeTargetWithMockData("", Some(TestModels.calcModelOneRate), TestModels.summaryIndividualRebasedNoAcqDateOrRebasedCosts)

      "return a 400" in {
        status(result) shouldBe 400
      }
    }

    "submitting an invalid form with a rebased value and an acquisition date after tax start" should {
      lazy val result = executeTargetWithMockData("", Some(TestModels.calcModelOneRate), TestModels.summaryIndividualRebasedAcqDateAfter)

      "return a 400" in {
        status(result) shouldBe 400
      }
    }
  }
}
