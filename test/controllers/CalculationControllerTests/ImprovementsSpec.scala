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

class ImprovementsSpec extends UnitSpec with WithFakeApplication with MockitoSugar {

  implicit val hc = new HeaderCarrier()

  def setupTarget(getData: Option[ImprovementsModel],
                  postData: Option[ImprovementsModel],
                  rebasedValueData: Option[RebasedValueModel] = None
                 ): CalculationController = {

    val mockCalcConnector = mock[CalculatorConnector]
    val mockCalcElectionConstructor = mock[CalculationElectionConstructor]

    when(mockCalcConnector.fetchAndGetFormData[ImprovementsModel](Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(getData))

    when(mockCalcConnector.fetchAndGetValue[RebasedValueModel](Matchers.eq("rebasedValue"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(rebasedValueData))

    lazy val data = CacheMap("form-id", Map("data" -> Json.toJson(postData.getOrElse(ImprovementsModel("",None,None)))))
    when(mockCalcConnector.saveFormData[ImprovementsModel](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(data))

    new CalculationController {
      override val calcConnector: CalculatorConnector = mockCalcConnector
      override val calcElectionConstructor: CalculationElectionConstructor = mockCalcElectionConstructor
    }
  }

  "In CalculationController calling the .improvements action " when {

    lazy val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/improvements").withSession(SessionKeys.sessionId -> "12345")

    "not supplied with a pre-existing stored model" should {

      val target = setupTarget(None,None)
      lazy val result = target.improvements(fakeRequest)
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
          document.title shouldEqual Messages("calc.improvements.question")
        }

        "have the heading Calculate your tax (non-residents)" in {
          document.body.getElementsByTag("H1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "display the correct wording for radio option `yes`" in {
          document.body.getElementById("isClaimingImprovements-yes").parent.text shouldEqual Messages("calc.base.yes")
        }

        "display the correct wording for radio option `no`" in {
          document.body.getElementById("isClaimingImprovements-no").parent.text shouldEqual Messages("calc.base.no")
        }

        "contain a hidden component with an input box" in {
          document.body.getElementById("hidden").html should include ("input")
        }
      }
    }

    "supplied with a pre-existing model with 'Yes' checked and value already entered" should {

      val target = setupTarget(Some(ImprovementsModel("Yes", Some(10000))), None)
      lazy val result = target.improvements(fakeRequest)
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 200" in {
        status(result) shouldBe 200
      }

      "return some HTML that" should {

        "be pre populated with Yes box selected and a value of 10000 entered" in {
          document.getElementById("isClaimingImprovements-yes").attr("checked") shouldEqual "checked"
          document.getElementById("improvementsAmt").attr("value") shouldEqual "10000"
        }
      }
    }

    "supplied with a pre-existing model with 'No' checked and value already entered" should {

      val target = setupTarget(Some(ImprovementsModel("No", Some(0))), None)
      lazy val result = target.improvements(fakeRequest)
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 200" in {
        status(result) shouldBe 200
      }

      "return some HTML that" should {

        "be pre populated with No box selected and a value of 0" in {
          document.getElementById("isClaimingImprovements-no").attr("checked") shouldEqual "checked"
          document.getElementById("improvementsAmt").attr("value") shouldEqual "0"
        }
      }
    }

    "not supplied with a pre-existing stored model but with a rebased value" should {

      val target = setupTarget(None,None,Some(RebasedValueModel("Yes", Some(1000))))
      lazy val result = target.improvements(fakeRequest)
      lazy val document = Jsoup.parse(bodyOf(result))

      "contain a two hidden input boxes for improvements" in {
        document.body.getElementById("hidden").getElementsByTag("input").first().id() shouldBe "improvementsAmt"
        document.body.getElementById("hidden").getElementsByTag("input").last().id() shouldBe "improvementsAmtAfter"
      }
    }

    "not supplied with a pre-existing stored model and with no rebased value model" should {

      val target = setupTarget(None, None)
      lazy val result = target.improvements(fakeRequest)
      lazy val document = Jsoup.parse(bodyOf(result))

      "contain a two hidden input boxes for improvements" in {
        document.body.getElementById("hidden").html should include("input")
        document.body.getElementById("hidden").getElementsByTag("input").first().id() shouldBe "improvementsAmt"
        document.body.getElementById("hidden").getElementsByTag("input").last().id() shouldBe "improvementsAmt"
      }
    }
  }

  "In CalculationController calling the .submitImprovements action " when {

    def buildRequest(body: (String, String)*): FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest("POST", "/calculate-your-capital-gains/rebased-value")
      .withSession(SessionKeys.sessionId -> "12345")
      .withFormUrlEncodedBody(body: _*)

    def executeTargetWithMockData(selection: String, amount: String): Future[Result] = {
      lazy val fakeRequest = buildRequest(("isClaimingImprovements", selection), ("improvementsAmt", amount))
      val mockData = amount match {
        case "" => ImprovementsModel(selection, None)
        case "fhu39awd8" => ImprovementsModel(selection, None) // required for real number test ONLY
        case _ => ImprovementsModel(selection, Some(BigDecimal(amount)))
      }
      val target = setupTarget(None, Some(mockData))
      target.submitImprovements(fakeRequest)
    }

    "submitting a valid form with 'Yes' and a value of 12045" should {

      lazy val result = executeTargetWithMockData("Yes","12045")

      "return a 303" in {
        status(result) shouldBe 303
      }
    }

    "submitting a valid form with 'No' and no value" should {

      lazy val result = executeTargetWithMockData("No","")

      "return a 303" in {
        status(result) shouldBe 303
      }
    }

    "submitting an invalid form with 'Yes' and a value of 'fhu39awd8'" should {

      lazy val result = executeTargetWithMockData("Yes","fhu39awd8")
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 400" in {
        status(result) shouldBe 400
      }

      "return HTML that displays the error message " in {
        document.select("div#hidden span.error-notification").text shouldEqual "Real number value expected"
      }
    }

    "submitting an invalid form with 'Yes' and a negative value of -100'" should {

      lazy val result = executeTargetWithMockData("Yes","-100")
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 400" in {
        status(result) shouldBe 400
      }

      "return HTML that displays the error message " in {
        document.select("div#hidden span.error-notification").text shouldEqual Messages("calc.improvements.errorNegative")
      }
    }


    "submitting an invalid form with 'Yes' and an empty value'" should {

      lazy val result = executeTargetWithMockData("Yes","")
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 400" in {
        status(result) shouldBe 400
      }

      "return HTML that displays the error message " in {
        document.select("div#hidden span.error-notification").text shouldEqual Messages("calc.improvements.error.no.value.supplied")
      }
    }

    "submitting an invalid form with 'Yes' and value 1.111'" should {

      lazy val result = executeTargetWithMockData("Yes","1.111")
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 400" in {
        status(result) shouldBe 400
      }

      "return HTML that displays the error message " in {
        document.select("div#hidden span.error-notification").text shouldEqual Messages("calc.improvements.errorDecimalPlaces")
      }
    }
  }
}
