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
import controllers.CalculationController
import models.PrivateResidenceReliefModel
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future

class PrivateResidenceReliefSpec extends UnitSpec with WithFakeApplication with MockitoSugar {

  implicit val hc = new HeaderCarrier()
  def setupTarget(getData: Option[PrivateResidenceReliefModel], postData: Option[PrivateResidenceReliefModel]): CalculationController = {

    val mockCalcConnector = mock[CalculatorConnector]
    val mockCalcElectionConstructor = mock[CalculationElectionConstructor]

    when(mockCalcConnector.fetchAndGetFormData[PrivateResidenceReliefModel](Matchers.anyString())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(getData))

    lazy val data = CacheMap("form-id", Map("data" -> Json.toJson(postData.getOrElse(PrivateResidenceReliefModel("", None)))))
    when(mockCalcConnector.saveFormData[PrivateResidenceReliefModel](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(data))

    new CalculationController {
      override val calcConnector: CalculatorConnector = mockCalcConnector
      override val calcElectionConstructor: CalculationElectionConstructor = mockCalcElectionConstructor
    }
  }

  "In CalculationController calling the .privateResidenceRelief action " should {
    lazy val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/private-residence-relief").withSession(SessionKeys.sessionId -> "12345")
    val target = setupTarget(None, None)
    lazy val result = target.privateResidenceRelief(fakeRequest)
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

      "have the title 'calc.privateResidenceRelief.question'" in {
        document.title shouldEqual Messages("calc.privateResidenceRelief.question")
      }

      "have the heading 'Calculate your tax (non-residents)'" in {
        document.body.getElementsByTag("H1").text shouldEqual Messages("calc.base.pageHeading")
      }

      "have a yes no helper with hidden content and question 'calc.privateResidenceRelief.question'" in {
        document.body.getElementById("isClaimingPRR-yes").parent.text shouldBe Messages("calc.base.yes")
        document.body.getElementById("isClaimingPRR-no").parent.text shouldBe Messages("calc.base.no")
        document.body.getElementsByTag("legend").text shouldBe Messages("calc.privateResidenceRelief.question")
      }

      "have a hidden input with question 'calc.privateResidenceRelief.questionTwo'" in {
        document.body.getElementById("daysClaimed").tagName shouldEqual "input"
        document.select("label[for=daysClaimed]").text should include (Messages("calc.privateResidenceRelief.questionTwoStart"))
        document.select("label[for=daysClaimed]").text should include (Messages("calc.privateResidenceRelief.questionTwoEnd"))
      }
    }
  }
}
