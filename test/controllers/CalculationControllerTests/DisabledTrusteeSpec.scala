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

class DisabledTrusteeSpec extends UnitSpec with WithFakeApplication with MockitoSugar {

  implicit val hc = new HeaderCarrier()

  def setupTarget(getData: Option[DisabledTrusteeModel], postData: Option[DisabledTrusteeModel]): CalculationController = {

    val mockCalcConnector = mock[CalculatorConnector]
    val mockCalcElectionConstructor = mock[CalculationElectionConstructor]

    when(mockCalcConnector.fetchAndGetFormData[DisabledTrusteeModel](Matchers.anyString())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(getData))

    lazy val data = CacheMap("form-id", Map("data" -> Json.toJson(postData.getOrElse(DisabledTrusteeModel("")))))
    when(mockCalcConnector.saveFormData[DisabledTrusteeModel](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(data))

    new CalculationController {
      override val calcConnector: CalculatorConnector = mockCalcConnector
      override val calcElectionConstructor: CalculationElectionConstructor = mockCalcElectionConstructor
    }
  }

  // GET Tests
  "Calling the CalculationController.disabledTrustee" when {

    lazy val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/disabled-trustee").withSession(SessionKeys.sessionId -> "12345")

    "not supplied with a pre-existing stored model" should {

      val target = setupTarget(None, None)
      lazy val result = target.disabledTrustee(fakeRequest)
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 200" in {
        status(result) shouldBe 200
      }

      "return some HTML that" should {
        "contain some text and use the character set utf-8" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "have the title Are you a trustee for someone who’s vulnerable?" in {
          document.title shouldEqual Messages("calc.disabledTrustee.question")
        }

        "have the heading Calculate your tax (non-residents) " in {
          document.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a 'Back' link " in {
          document.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
        }

        "have the question 'When did you sign the contract that made someone else the owner?' as the legend of the input" in {
          document.body.getElementsByTag("legend").text shouldEqual Messages("calc.disabledTrustee.question")
        }

        "display a radio button with the option 'Yes'" in {
          document.body.getElementById("isVulnerable-yes").parent.text shouldEqual Messages("calc.base.yes")
        }
        "display a radio button with the option 'No'" in {
          document.body.getElementById("isVulnerable-no").parent.text shouldEqual Messages("calc.base.no")
        }

        "display a 'Continue' button " in {
          document.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
        }
      }
    }

    "supplied with a pre-existing stored model" should {

      "return a 200" in {
        val target = setupTarget(Some(DisabledTrusteeModel("Yes")), None)
        lazy val result = target.disabledTrustee(fakeRequest)
        status(result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          val target = setupTarget(Some(DisabledTrusteeModel("Yes")), None)
          lazy val result = target.disabledTrustee(fakeRequest)
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "have the radio option `Yes` selected if `Yes` is supplied in the model" in {
          val target = setupTarget(Some(DisabledTrusteeModel("Yes")), None)
          lazy val result = target.disabledTrustee(fakeRequest)
          lazy val document = Jsoup.parse(bodyOf(result))
          document.body.getElementById("isVulnerable-yes").parent.classNames().contains("selected") shouldBe true
        }

        "have the radio option `No` selected if `No` is supplied in the model" in {
          val target = setupTarget(Some(DisabledTrusteeModel("No")), None)
          lazy val result = target.disabledTrustee(fakeRequest)
          lazy val document = Jsoup.parse(bodyOf(result))
          document.body.getElementById("isVulnerable-no").parent.classNames().contains("selected") shouldBe true
        }
      }
    }
  }

  // POST Tests
  "In CalculationController calling the .submitDisabledTrustee action" when {

    def buildRequest(body: (String, String)*): FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest("POST", "/calculate-your-capital-gains/disabled-trustee")
      .withSession(SessionKeys.sessionId -> "12345")
      .withFormUrlEncodedBody(body: _*)

    def executeTargetWithMockData(data: String): Future[Result] = {
      lazy val fakeRequest = buildRequest(("isVulnerable", data))
      val mockData = new DisabledTrusteeModel(data)
      val target = setupTarget(None, Some(mockData))
      target.submitDisabledTrustee(fakeRequest)
    }

    "submitting a valid form with 'Yes'" should {

      lazy val result = executeTargetWithMockData("Yes")

      "return a 303" in {
        status(result) shouldBe 303
      }
    }

    "submitting a valid form with 'No'" should {

      lazy val result = executeTargetWithMockData("No")

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
    }
  }
}
