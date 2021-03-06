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

import common.KeystoreKeys
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

class EntrepreneursReliefSpec extends UnitSpec with WithFakeApplication with MockitoSugar {

  implicit val hc = new HeaderCarrier()

  def setupTarget(getData: Option[EntrepreneursReliefModel],
                  postData: Option[EntrepreneursReliefModel],
                  acquisitionDateData: Option[AcquisitionDateModel] = None,
                  rebasedValueData: Option[RebasedValueModel] = None
                 ): CalculationController = {

    val mockCalcConnector = mock[CalculatorConnector]
    val mockCalcElectionConstructor = mock[CalculationElectionConstructor]

    when(mockCalcConnector.fetchAndGetFormData[EntrepreneursReliefModel](Matchers.anyString())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(getData))

    when(mockCalcConnector.fetchAndGetFormData[RebasedValueModel](Matchers.eq(KeystoreKeys.rebasedValue))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(rebasedValueData))

    when(mockCalcConnector.fetchAndGetFormData[AcquisitionDateModel](Matchers.eq(KeystoreKeys.acquisitionDate))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(acquisitionDateData))

    lazy val data = CacheMap("form-id", Map("data" -> Json.toJson(postData.getOrElse(EntrepreneursReliefModel("")))))
    when(mockCalcConnector.saveFormData[EntrepreneursReliefModel](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(data))

    new CalculationController {
      override val calcConnector: CalculatorConnector = mockCalcConnector
      override val calcElectionConstructor: CalculationElectionConstructor = mockCalcElectionConstructor
    }
  }

  //GET Tests
  "In CalculationController calling the .entrepreneursRelief action " should {

    lazy val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/entrepreneurs-relief").withSession(SessionKeys.sessionId -> "12345")

    "not supplied with a pre-existing stored model" should {

      "when no acquisition date supplied and no rebased value supplied" should {

        val target = setupTarget(None, None)
        lazy val result = target.entrepreneursRelief(fakeRequest)
        lazy val document = Jsoup.parse(bodyOf(result))

        "return a 200" in {
          status(result) shouldBe 200
        }

        "return some HTML that" should {

          "contain some text and use the character set utf-8" in {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }

          "have the title 'Are you claiming Entrepreneurs Relief?'" in {
            document.title shouldEqual Messages("calc.entrepreneursRelief.question")
          }

          "have the heading Calculate your tax (non-residents) " in {
            document.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
          }

          s"have a 'Back' link to ${routes.CalculationController.disposalCosts().url} " in {
            document.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
            document.body.getElementById("back-link").attr("href") shouldEqual routes.CalculationController.disposalCosts().url
          }

          "have the question 'Are you claiming Entrepreneurs Relief?' as the legend of the input" in {
            document.body.getElementsByTag("legend").text shouldEqual Messages("calc.entrepreneursRelief.question")
          }

          "display a 'Continue' button " in {
            document.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
          }

          "have a sidebar with additional links" in {
            document.body.getElementsByClass("sidebar")
          }
        }
      }

      "when acquisition date supplied and no rebased value supplied" should {

        val target = setupTarget(None, None, Some(AcquisitionDateModel("Yes", Some(1), Some(1), Some(2016))))
        lazy val result = target.entrepreneursRelief(fakeRequest)
        lazy val document = Jsoup.parse(bodyOf(result))

        s"have a 'Back' link to ${routes.CalculationController.privateResidenceRelief().url} " in {
          document.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
          document.body.getElementById("back-link").attr("href") shouldEqual routes.CalculationController.privateResidenceRelief().url
        }
      }

      "when no acquisition date supplied but a rebased value is supplied" should {

        val target = setupTarget(None, None, None, Some(RebasedValueModel("Yes",Some(500))))
        lazy val result = target.entrepreneursRelief(fakeRequest)
        lazy val document = Jsoup.parse(bodyOf(result))

        s"have a 'Back' link to ${routes.CalculationController.privateResidenceRelief().url} " in {
          document.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
          document.body.getElementById("back-link").attr("href") shouldEqual routes.CalculationController.privateResidenceRelief().url
        }
      }
    }

    "supplied with a pre-existing stored model" should {

      val target = setupTarget(Some(EntrepreneursReliefModel("Yes")), None)
      lazy val result = target.entrepreneursRelief(fakeRequest)
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 200" in {
        status(result) shouldBe 200
      }

      "return some HTML that" should {

        "have the radio option `Yes` selected if `Yes` is supplied in the model" in {
          document.body.getElementById("entrepreneursRelief-yes").parent.classNames().contains("selected") shouldBe true
        }
      }
    }
  }

  //POST Tests
  "In CalculationController calling the .submitEntrepreneursRelief action" when {

    def buildRequest(body: (String, String)*): FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest("POST", "/calculate-your-capital-gains/entrepreneurs-relief")
      .withSession(SessionKeys.sessionId -> "12345")
      .withFormUrlEncodedBody(body: _*)

    def executeTargetWithMockData(data: String): Future[Result] = {
      lazy val fakeRequest = buildRequest(("entrepreneursRelief", data))
      val mockData = new EntrepreneursReliefModel(data)
      val target = setupTarget(None, Some(mockData))
      target.submitEntrepreneursRelief(fakeRequest)
    }

    "submitting a valid form with 'Yes'" should {

      lazy val result = executeTargetWithMockData("Yes")

      "return a 303" in {
        status(result) shouldBe 303
      }

      s"redirect to ${routes.CalculationController.allowableLosses()}" in {
        redirectLocation(result) shouldBe Some(s"${routes.CalculationController.allowableLosses()}")
      }
    }

    "submitting a valid form with 'No'" should {

      lazy val result = executeTargetWithMockData("No")

      "return a 303" in {
        status(result) shouldBe 303
      }

      s"redirect to ${routes.CalculationController.allowableLosses()}" in {
        redirectLocation(result) shouldBe Some(s"${routes.CalculationController.allowableLosses()}")
      }
    }

    "submitting an invalid form with no data" should {

      lazy val result = executeTargetWithMockData("")

      "return a 400" in {
        status(result) shouldBe 400
      }
    }
  }
}
