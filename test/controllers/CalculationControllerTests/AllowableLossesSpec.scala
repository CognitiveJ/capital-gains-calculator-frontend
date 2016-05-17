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
import controllers.{CalculationController, routes}
import models.{AcquisitionDateModel, AcquisitionValueModel, AllowableLossesModel, RebasedValueModel}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future

class AllowableLossesSpec extends UnitSpec with WithFakeApplication with MockitoSugar {

  implicit val hc = new HeaderCarrier()

  def setupTarget(
                   getData: Option[AllowableLossesModel],
                   postData: Option[AllowableLossesModel],
                   acquisitionDate: Option[AcquisitionDateModel],
                   rebasedData: Option[RebasedValueModel] = None): CalculationController = {

    val mockCalcConnector = mock[CalculatorConnector]
    val mockCalcElectionConstructor = mock[CalculationElectionConstructor]

    when(mockCalcConnector.fetchAndGetFormData[AllowableLossesModel](Matchers.eq("allowableLosses"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(getData))

    when(mockCalcConnector.fetchAndGetFormData[AcquisitionDateModel](Matchers.eq("acquisitionDate"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(acquisitionDate))

    when(mockCalcConnector.fetchAndGetFormData[RebasedValueModel](Matchers.eq("rebasedValue"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(rebasedData))

    lazy val data = CacheMap("form-id", Map("data" -> Json.toJson(postData.getOrElse(AllowableLossesModel("No", None)))))
    when(mockCalcConnector.saveFormData[AllowableLossesModel](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(data))

    new CalculationController {
      override val calcConnector: CalculatorConnector = mockCalcConnector
      override val calcElectionConstructor: CalculationElectionConstructor = mockCalcElectionConstructor
    }
  }

  "In CalculationController calling the .allowableLosses action " when {
    lazy val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/allowable-losses").withSession(SessionKeys.sessionId -> "12345")

    "not supplied with a pre-existing stored value" should {
      val target = setupTarget(None, None, None)
      lazy val result = target.allowableLosses(fakeRequest)
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 200" in {
        status(result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "have a back button" in {
          document.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
        }

        "have the title 'Are you claiming any allowable losses?'" in {
          document.title shouldEqual Messages("calc.allowableLosses.question.one")
        }

        "have the heading 'Calculate your tax (non-residents)'" in {
          document.body.getElementsByTag("H1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a yes no helper with hidden content and question 'Are you claiming any allowable losses?'" in {
          document.body.getElementById("isClaimingAllowableLosses-yes").parent.text shouldBe Messages("calc.base.yes")
          document.body.getElementById("isClaimingAllowableLosses-no").parent.text shouldBe Messages("calc.base.no")
          document.body.getElementsByTag("legend").text shouldBe Messages("calc.allowableLosses.question.one")
        }

        "have a hidden monetary input with question 'Whats the total value of your allowable losses?'" in {
          document.body.getElementById("allowableLossesAmt").tagName shouldEqual "input"
          document.select("label[for=allowableLossesAmt]").text should include(Messages("calc.allowableLosses.question.two"))
        }

        "have no value auto-filled into the input box" in {
          document.getElementById("allowableLossesAmt").attr("value") shouldBe empty
        }

        "have a hidden help text section with summary 'What are allowable losses?' and correct content" in {
          document.select("div#allowableLossesHiddenHelp").text should
            include(Messages("calc.allowableLosses.helpText.title"))
          include(Messages("calc.allowableLosses.helpText.paragraph.one"))
          include(Messages("calc.allowableLosses.helpText.bullet.one"))
          include(Messages("calc.allowableLosses.helpText.bullet.two"))
          include(Messages("calc.allowableLosses.helpText.bullet.three"))
        }

        "has a Continue button" in {
          document.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
        }
      }
    }
    "supplied with a pre-existing stored model" should {
      val testAllowableLossesModel = AllowableLossesModel("Yes", Some(9999.54))
      val target = setupTarget(Some(testAllowableLossesModel), None, None)
      lazy val result = target.allowableLosses(fakeRequest)
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 200" in {
        status(result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "have the 'Yes' Radio option selected" in {
          document.getElementById("isClaimingAllowableLosses-yes").parent.classNames().contains("selected") shouldBe true
        }

        "have the value 9999.54 auto-filled into the input box" in {
          document.getElementById("allowableLossesAmt").attr("value") shouldEqual ("9999.54")
        }
      }
    }
  }

  "In CalculationController calling the .submitAllowableLosses action" when {
    def buildRequest(body: (String, String)*): FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest("POST", "/calculate-your-capital-gains/allowance")
      .withSession(SessionKeys.sessionId -> "12345")
      .withFormUrlEncodedBody(body: _*)

    def executeTargetWithMockData(
                                   answer: String,
                                   amount: String,
                                   acquisitionDate: AcquisitionDateModel,
                                   rebasedData: Option[RebasedValueModel] = None): Future[Result] = {

      lazy val fakeRequest = buildRequest(("isClaimingAllowableLosses", answer), ("allowableLossesAmt", amount))

      val mockData = amount match {
        case "" => AllowableLossesModel(answer, None)
        case _ => AllowableLossesModel(answer, Some(BigDecimal(amount)))
      }

      val target = setupTarget(None, Some(mockData), Some(acquisitionDate), rebasedData)
      target.submitAllowableLosses(fakeRequest)
    }

    "submitting a valid form with 'Yes' and an amount with no acquisition date" should {
      lazy val result = executeTargetWithMockData("Yes", "1000", AcquisitionDateModel("No", None, None, None))

      "return a 303" in {
        status(result) shouldBe 303
      }
    }

    "submitting a valid form with 'Yes' and an amount with two decimal places with an acquisition date after the tax start date" should {
      lazy val result = executeTargetWithMockData("Yes", "1000.11", AcquisitionDateModel("Yes", Some(10), Some(10), Some(2016)))

      "return a 303" in {
        status(result) shouldBe 303
      }
    }

    "submitting a valid form with 'No' and a null amount with an acquisition date before the tax start date" should {
      lazy val result = executeTargetWithMockData("No", "", AcquisitionDateModel("Yes", Some(9), Some(9), Some(1999)))

      "return a 303" in {
        status(result) shouldBe 303
      }
    }

    "submitting a valid form with 'No' and a negative amount with no returned acquisition date model" should {
      lazy val result = executeTargetWithMockData("No", "-1000", AcquisitionDateModel("No", None, None, None))

      "return a 303" in {
        status(result) shouldBe 303
      }
    }

    "submitting an invalid form with no selection and a null amount" should {
      lazy val result = executeTargetWithMockData("", "", AcquisitionDateModel("No", None, None, None))

      "return a 400" in {
        status(result) shouldBe 400
      }
    }

    "submitting an invalid form with 'Yes' selection and a null amount" should {
      lazy val result = executeTargetWithMockData("Yes", "", AcquisitionDateModel("No", None, None, None))

      "return a 400" in {
        status(result) shouldBe 400
      }
    }

    "submitting an invalid form with 'Yes' selection and an amount with three decimal places" should {
      lazy val result = executeTargetWithMockData("Yes", "1000.111", AcquisitionDateModel("No", None, None, None))

      "return a 400" in {
        status(result) shouldBe 400
      }
    }

    "submitting an invalid form with 'Yes' selection and a negative amount" should {
      lazy val result = executeTargetWithMockData("Yes", "-1000", AcquisitionDateModel("No", None, None, None))

      "return a 400" in {
        status(result) shouldBe 400
      }
    }

    "submitting a valid form when an acquisition date (before 2015-04-06) has been supplied but no property was not revalued" should {
      val dateBefore = AcquisitionDateModel("Yes", Some(1), Some(4), Some(2015))
      lazy val result = executeTargetWithMockData("No", "", dateBefore)

      "return a 303" in {
        status(result) shouldBe 303
      }

      "redirect to the calculation election view" in {
        redirectLocation(result) shouldBe Some(s"${routes.CalculationController.calculationElection()}")
      }
    }

    "submitting a valid form when an acquisition date (after 2015-04-06) has been supplied but no property was not revalued" should {
      val dateAfter = AcquisitionDateModel("Yes", Some(1), Some(6), Some(2015))
      lazy val result = executeTargetWithMockData("No", "", dateAfter)

      "return a 303" in {
        status(result) shouldBe 303
      }

      "redirect to the other reliefs view" in {
        redirectLocation(result) shouldBe Some(s"${routes.CalculationController.otherReliefs()}")
      }
    }

    "submitting a valid form when no acquisition date is supplied and the property was not revalued" should {
      val noDate = AcquisitionDateModel("No", None, None, None)
      lazy val result = executeTargetWithMockData("No", "", noDate)

      "return a 303" in {
        status(result) shouldBe 303
      }

      "redirect to the other reliefs view" in {
        redirectLocation(result) shouldBe Some(s"${routes.CalculationController.otherReliefs()}")
      }
    }

    "submitting a valid form when no acquisition date is supplied and the property was revalued" should {
      val rebased = RebasedValueModel("Yes", Some(BigDecimal(1000)))
      val noDate = AcquisitionDateModel("No", None, None, None)
      lazy val result = executeTargetWithMockData("No", "", noDate, Some(rebased))

      "return a 303" in {
        status(result) shouldBe 303
      }

      "redirect to the calculation election view" in {
        redirectLocation(result) shouldBe Some(s"${routes.CalculationController.calculationElection()}")
      }
    }

    "submitting a valid form when an invalid Acquisition Date Model has been supplied and no property was revalued" should {
      val invalidDate = AcquisitionDateModel("invalid", None, None, None)
      lazy val result = executeTargetWithMockData("No", "", invalidDate)

      "return a 303" in {
        status(result) shouldBe 303
      }

      s"redirect to ${routes.CalculationController.otherReliefs()}" in {
        redirectLocation(result) shouldBe Some(s"${routes.CalculationController.otherReliefs()}")
      }
    }
  }
}
