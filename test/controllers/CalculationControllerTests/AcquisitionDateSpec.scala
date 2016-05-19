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
import common.DefaultRoutes._
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

class AcquisitionDateSpec extends UnitSpec with WithFakeApplication with MockitoSugar {

  implicit val hc = new HeaderCarrier()

  def setupTarget(getData: Option[AcquisitionDateModel],
                  postData: Option[AcquisitionDateModel],
                  otherPropertiesData: Option[OtherPropertiesModel]
                 ): CalculationController = {

    val mockCalcConnector = mock[CalculatorConnector]
    val mockCalcElectionConstructor = mock[CalculationElectionConstructor]

    when(mockCalcConnector.fetchAndGetFormData[AcquisitionDateModel](Matchers.anyString())(Matchers.any(), Matchers.any()))
    .thenReturn(Future.successful(getData))

    when(mockCalcConnector.fetchAndGetFormData[OtherPropertiesModel](Matchers.eq(KeystoreKeys.otherProperties))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(otherPropertiesData))

    lazy val data = CacheMap("form-id", Map("data" -> Json.toJson(postData.getOrElse(AcquisitionDateModel("",None,None,None)))))
    when(mockCalcConnector.saveFormData[AcquisitionDateModel](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
    .thenReturn(Future.successful(data))

    new CalculationController {
      override val calcConnector: CalculatorConnector = mockCalcConnector
      override val calcElectionConstructor: CalculationElectionConstructor = mockCalcElectionConstructor
    }
  }

  "In CalculationController calling the .acquisitionDate action " should {

    lazy val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/customer-type").withSession(SessionKeys.sessionId -> "12345")
    
    "not supplied with a pre-existing model" should {

      "when Previous Taxable Gains is 'No'" should {

        val target = setupTarget(None, None, Some(OtherPropertiesModel("No", None)))
        lazy val result = target.acquisitionDate(fakeRequest)
        lazy val document = Jsoup.parse(bodyOf(result))

        "return a 200" in {
          status(result) shouldBe 200
        }

        "return some HTML that" should {

          "contain some text and use the character set utf-8" in {
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }

          s"have the title '${Messages("calc.acquisitionDate.question")}'" in {
            document.title shouldEqual Messages("calc.acquisitionDate.question")
          }

          "have the heading Calculate your tax (non-residents) " in {
            document.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
          }

          s"have a 'Back' link to ${routes.CalculationController.otherProperties().url} " in {
            document.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
            document.body.getElementById("back-link").attr("href") shouldEqual routes.CalculationController.otherProperties().url
          }

          s"have the question '${Messages("calc.acquisitionDate.question")}" in {
            document.body.getElementsByTag("legend").text should include(Messages("calc.acquisitionDate.question"))
          }

          "display the correct wording for radio option `yes`" in {
            document.body.getElementById("hasAcquisitionDate-yes").parent.text shouldEqual Messages("calc.base.yes")
          }

          "display the correct wording for radio option `no`" in {
            document.body.getElementById("hasAcquisitionDate-no").parent.text shouldEqual Messages("calc.base.no")
          }

          "contain a hidden component with an input box" in {
            document.body.getElementById("hidden").html should include("input")
          }

          "display three input boxes with labels Day, Month and Year respectively" in {
            document.select("label[for=acquisitionDate.day]").text shouldEqual Messages("calc.common.date.fields.day")
            document.select("label[for=acquisitionDate.month]").text shouldEqual Messages("calc.common.date.fields.month")
            document.select("label[for=acquisitionDate.year]").text shouldEqual Messages("calc.common.date.fields.year")
          }

          "display a 'Continue' button " in {
            document.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
          }
        }
      }

      "when Previous Taxable Gains is 'Yes' and amount is 0.00" should {

        val target = setupTarget(None, None, Some(OtherPropertiesModel("Yes", Some(0))))
        lazy val result = target.acquisitionDate(fakeRequest)
        lazy val document = Jsoup.parse(bodyOf(result))

        s"have a 'Back' link to ${routes.CalculationController.otherProperties().url} " in {
          document.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
          document.body.getElementById("back-link").attr("href") shouldEqual routes.CalculationController.otherProperties().url
        }
      }

      "when Previous Taxable Gains is 'Yes' and amount is > 0.00" should {

        val target = setupTarget(None, None, Some(OtherPropertiesModel("Yes", Some(0.01))))
        lazy val result = target.acquisitionDate(fakeRequest)
        lazy val document = Jsoup.parse(bodyOf(result))

        s"have a 'Back' link to ${routes.CalculationController.annualExemptAmount().url} " in {
          document.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
          document.body.getElementById("back-link").attr("href") shouldEqual routes.CalculationController.annualExemptAmount().url
        }
      }

      "when there is no Previous Taxable Gains model" should {

        val target = setupTarget(None, None, None)
        lazy val result = target.acquisitionDate(fakeRequest)
        lazy val document = Jsoup.parse(bodyOf(result))

        s"have a 'Back' link to ${missingDataRoute} " in {
          document.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
          document.body.getElementById("back-link").attr("href") shouldEqual missingDataRoute
        }
      }
    }

    "supplied with a model already filled with data" should {

      val testAcquisitionDateModel = new AcquisitionDateModel("Yes", Some(10), Some(12), Some(2016))
      val target = setupTarget(Some(testAcquisitionDateModel), None, Some(OtherPropertiesModel("No",None)))
      lazy val result = target.acquisitionDate(fakeRequest)
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 200" in {
        status(result) shouldBe 200
      }

      "return some HTML that" should {

        "have the radio option `Yes` selected if `Yes` is supplied in the model" in {
          document.body.getElementById("hasAcquisitionDate-yes").parent.classNames().contains("selected") shouldBe true
        }

        "have the date 10, 12, 2016 pre-populated" in {
          document.body.getElementById("acquisitionDate.day").attr("value") shouldEqual testAcquisitionDateModel.day.get.toString
          document.body.getElementById("acquisitionDate.month").attr("value") shouldEqual testAcquisitionDateModel.month.get.toString
          document.body.getElementById("acquisitionDate.year").attr("value") shouldEqual testAcquisitionDateModel.year.get.toString
        }
      }
    }
  }

  "In CalculationController calling the submitAcquisitionDate action" when {

    def buildRequest(body: (String, String)*): FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest("POST", "/calculate-your-capital-gains/acquisition-date")
      .withSession(SessionKeys.sessionId -> "12345")
      .withFormUrlEncodedBody(body: _*)

    def executeTargetWithMockData
    (
      hasAcquisitionDate: String,
      day: Option[Int] = None,
      month: Option[Int] = None,
      year: Option[Int] = None
    ): Future[Result] = {
      lazy val fakeRequest = buildRequest(
        ("hasAcquisitionDate", hasAcquisitionDate),
        ("acquisitionDate.day", day.getOrElse("").toString),
        ("acquisitionDate.month", month.getOrElse("").toString),
        ("acquisitionDate.year", year.getOrElse("").toString))
      val mockData = new AcquisitionDateModel("Yes",day,month,year)
      val target = setupTarget(None, Some(mockData), Some(OtherPropertiesModel("No",None)))
      target.submitAcquisitionDate(fakeRequest)
    }

    "submitting a valid form" should {

      lazy val result = executeTargetWithMockData("Yes",Some(10),Some(2),Some(2015))

      "return a 303" in {
        status(result) shouldBe 303
      }
    }

    "submitting a valid form with 'No' and no date value" should {

      lazy val result = executeTargetWithMockData("No")

      "return a 303" in {
        status(result) shouldBe 303
      }
    }

    "submitting a valid leap year date 29/02/2016" should {

      lazy val result = executeTargetWithMockData("Yes",Some(29),Some(2),Some(2016))

      "return a 303" in {
        status(result) shouldBe 303
      }

      s"redirect to ${routes.CalculationController.acquisitionValue()}" in {
        redirectLocation(result) shouldBe Some(s"${routes.CalculationController.acquisitionValue()}")
      }
    }

    "submitting an invalid leap year date 29/02/2017" should {

      lazy val result = executeTargetWithMockData("Yes",Some(29),Some(2),Some(2017))
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 400" in {
        status(result) shouldBe 400
      }

      s"should error with message ${Messages("calc.common.date.error.invalidDate")}" in {
        document.select(".error-notification").text should include (Messages("calc.common.date.error.invalidDate"))
      }
    }

    "submitting a day less than 1" should {

      lazy val result = executeTargetWithMockData("Yes",Some(0),Some(2),Some(2017))
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 400" in {
        status(result) shouldBe 400
      }

      s"should error with message ${Messages("calc.common.date.error.lessThan1")}" in {
        document.select(".error-notification").text should include (Messages("calc.common.date.error.invalidDate"))
      }
    }

    "submitting a day greater than 31" should {

      lazy val result = executeTargetWithMockData("Yes",Some(32),Some(2),Some(2017))
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 400" in {
        status(result) shouldBe 400
      }

      s"should error with message ${Messages("calc.common.date.error.greaterThan31")}" in {
        document.select(".error-notification").text should include (Messages("calc.common.date.error.invalidDate"))
      }
    }

    "submitting a month greater than 12" should {

      lazy val result = executeTargetWithMockData("Yes",Some(31),Some(13),Some(2017))
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 400" in {
        status(result) shouldBe 400
      }

      s"should error with message ${Messages("calc.common.date.error.greaterThan12")}" in {
        document.select(".error-notification").text should include (Messages("calc.common.date.error.invalidDate"))
      }
    }

    "submitting a month less than 1" should {

      lazy val result = executeTargetWithMockData("Yes",Some(31),Some(0),Some(2017))
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 400" in {
        status(result) shouldBe 400
      }

      s"should error with message ${Messages("calc.common.date.error.lessThan1")}" in {
        document.select(".error-notification").text should include (Messages("calc.common.date.error.invalidDate"))
      }
    }

    "submitting a day with no value" should {

      lazy val result = executeTargetWithMockData("Yes",None,Some(0),Some(2017))
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 400" in {
        status(result) shouldBe 400
      }

      "should error with message 'calc.common.date.error.invalidDate'" in {
        document.select(".error-notification").text should include (Messages("calc.common.date.error.invalidDate"))
      }
    }

    "submitting a month with no value" should {

      lazy val result = executeTargetWithMockData("Yes",Some(31),None,Some(2017))
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 400" in {
        status(result) shouldBe 400
      }

      "should error with message 'calc.common.date.error.invalidDate'" in {
        document.select(".error-notification").text should include (Messages("calc.common.date.error.invalidDate"))
      }
    }

    "submitting a year with no value" should {

      lazy val result = executeTargetWithMockData("Yes",Some(31),Some(1),None)
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
