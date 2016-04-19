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

package controllers

import java.util.UUID
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.CacheMap
import scala.collection.JavaConversions._
import connectors.CalculatorConnector
import models._
import org.scalatest.BeforeAndAfterEach
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Action}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.{SessionKeys, HeaderCarrier}
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import org.jsoup._
import org.scalatest.mock.MockitoSugar
import scala.concurrent.Future


//noinspection ScalaStyle
class CalculationControllerSpec extends UnitSpec with WithFakeApplication with MockitoSugar with BeforeAndAfterEach {

  val s = "Action(parser=BodyParser(anyContent))"
  val sessionId = UUID.randomUUID.toString
  val mockCalcConnector = mock[CalculatorConnector]
  val TestCalculationController = new CalculationController {
    override val calcConnector: CalculatorConnector = mockCalcConnector
  }

  implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(sessionId.toString)))

  class fakeRequestTo(url: String, controllerAction: Action[AnyContent]) {
    val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/" + url).withSession(SessionKeys.sessionId -> s"session-$sessionId")
    val result = controllerAction(fakeRequest)
    val jsoupDoc = Jsoup.parse(bodyOf(result))
  }

  class fakeRequestToPost(url: String, controllerAction: Action[AnyContent], data: (String, String)*) {
    val fakeRequest = FakeRequest("POST", "/calculate-your-capital-gains/" + url)
      .withSession(SessionKeys.sessionId -> s"session-$sessionId")
      .withFormUrlEncodedBody(data:_*)
    val result = controllerAction(fakeRequest)
    val jsoupDoc = Jsoup.parse(bodyOf(result))
  }

  def keystoreFetchCondition[T](data: Option[T]): Unit = {
    when(mockCalcConnector.fetchAndGetFormData[T](Matchers.anyString())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(data))
  }

  //################### Customer Type tests #######################
  "In CalculationController calling the .customerType action " when {
    "not supplied with a pre-existing stored model" should {
      object CustomerTypeTestDataItem extends fakeRequestTo("customer-type", TestCalculationController.customerType)

      "return a 200" in {
        keystoreFetchCondition[CustomerTypeModel](None)
        status(CustomerTypeTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          keystoreFetchCondition[CustomerTypeModel](None)
          contentType(CustomerTypeTestDataItem.result) shouldBe Some("text/html")
          charset(CustomerTypeTestDataItem.result) shouldBe Some("utf-8")
        }

        "have the title 'Who owned the property?'" in {
          keystoreFetchCondition[CustomerTypeModel](None)
          CustomerTypeTestDataItem.jsoupDoc.title shouldEqual Messages("calc.customerType.question")
        }

        "have the heading Calculate your tax (non-residents) " in {
          keystoreFetchCondition[CustomerTypeModel](None)
          CustomerTypeTestDataItem.jsoupDoc.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a 'Back' link " in {
          keystoreFetchCondition[CustomerTypeModel](None)
          CustomerTypeTestDataItem.jsoupDoc.body.getElementById("link-back").text shouldEqual Messages("calc.base.back")
        }

        "have the question 'Who owned the property?' as the legend of the input" in {
          keystoreFetchCondition[CustomerTypeModel](None)
          CustomerTypeTestDataItem.jsoupDoc.body.getElementsByTag("legend").text shouldEqual Messages("calc.customerType.question")
        }

        "display a radio button with the option `individual`" in {
          keystoreFetchCondition[CustomerTypeModel](None)
          CustomerTypeTestDataItem.jsoupDoc.body.getElementById("customerType-individual").parent.text shouldEqual Messages("calc.customerType.individual")
        }

        "have the radio option `individual` not selected by default" in {
          keystoreFetchCondition[CustomerTypeModel](None)
          CustomerTypeTestDataItem.jsoupDoc.body.getElementById("customerType-individual").parent.classNames().contains("selected") shouldBe false
        }

        "display a radio button with the option `trustee`" in {
          keystoreFetchCondition[CustomerTypeModel](None)
          CustomerTypeTestDataItem.jsoupDoc.body.getElementById("customerType-trustee").parent.text shouldEqual Messages("calc.customerType.trustee")
        }

        "display a radio button with the option `personal representative`" in {
          keystoreFetchCondition[CustomerTypeModel](None)
          CustomerTypeTestDataItem.jsoupDoc.body.getElementById("customerType-personalrep").parent.text shouldEqual Messages("calc.customerType.personalRep")
        }

        "display a 'Continue' button " in {
          keystoreFetchCondition[CustomerTypeModel](None)
          CustomerTypeTestDataItem.jsoupDoc.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
        }
      }
    }
    "supplied with a pre-existing stored model" should {
      object CustomerTypeTestDataItem extends fakeRequestTo("customer-type", TestCalculationController.customerType)
      val testModel = new CustomerTypeModel("individual")
      "return a 200" in {
        keystoreFetchCondition[CustomerTypeModel](Some(testModel))
        status(CustomerTypeTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          keystoreFetchCondition[CustomerTypeModel](Some(testModel))
          contentType(CustomerTypeTestDataItem.result) shouldBe Some("text/html")
          charset(CustomerTypeTestDataItem.result) shouldBe Some("utf-8")
        }

        "have the radio option `individual` selected by default" in {
          keystoreFetchCondition[CustomerTypeModel](Some(testModel))
          CustomerTypeTestDataItem.jsoupDoc.body.getElementById("customerType-individual").parent.classNames().contains("selected") shouldBe true
        }
      }
    }
  }

  "In CalculationController calling the .submitCustomerType action" when {
    def keystoreCacheCondition[T](data: CustomerTypeModel): Unit = {
      lazy val returnedCacheMap = CacheMap("form-id", Map("data" -> Json.toJson(data)))
      when(mockCalcConnector.saveFormData[T](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(returnedCacheMap))
    }
    "submitting a valid form with 'individual'" should {
      object CustomerTypeTestDataItem extends fakeRequestToPost(
        "customer-type",
        TestCalculationController.submitCustomerType,
        ("customerType", "individual")
      )
      val testModel = new CustomerTypeModel("individual")

      "return a 303" in {
        keystoreCacheCondition[CustomerTypeModel](testModel)
        status(CustomerTypeTestDataItem.result) shouldBe 303
      }
    }

    "submitting a valid form with 'trustee'" should {
      object CustomerTypeTestDataItem extends fakeRequestToPost(
        "customer-type",
        TestCalculationController.submitCustomerType,
        ("customerType", "trustee")
      )
      val testModel = new CustomerTypeModel("trustee")

      "return a 303" in {
        keystoreCacheCondition[CustomerTypeModel](testModel)
        status(CustomerTypeTestDataItem.result) shouldBe 303
      }
    }

    "submitting a valid form with 'personalRep'" should {
      object CustomerTypeTestDataItem extends fakeRequestToPost(
        "customer-type",
        TestCalculationController.submitCustomerType,
        ("customerType", "personalRep")
      )
      val testModel = new CustomerTypeModel("personalRep")

      "return a 303" in {
        keystoreCacheCondition[CustomerTypeModel](testModel)
        status(CustomerTypeTestDataItem.result) shouldBe 303
      }
    }

    "submitting an invalid form with no content" should {
      object CustomerTypeTestDataItem extends fakeRequestToPost(
        "customer-type",
        TestCalculationController.submitCustomerType,
        ("customerType", "")
      )
      val testModel = new CustomerTypeModel("")

      "return a 400" in {
        keystoreCacheCondition[CustomerTypeModel](testModel)
        status(CustomerTypeTestDataItem.result) shouldBe 400
      }
    }

    "submitting an invalid form with incorrect content" should {
      object CustomerTypeTestDataItem extends fakeRequestToPost(
        "customer-type",
        TestCalculationController.submitCustomerType,
        ("customerType", "invalid-user")
      )
      val testModel = new CustomerTypeModel("invalid-user")

      "return a 400" in {
        keystoreCacheCondition[CustomerTypeModel](testModel)
        status(CustomerTypeTestDataItem.result) shouldBe 400
      }
    }
  }

  //################### Disabled Trustee tests #######################
  "In CalculationController calling the .disabledTrustee action " when {

    "not supplied with a pre-existing stored model" should {

      object DisabledTrusteeTestDataItem extends fakeRequestTo("disabled-trustee", TestCalculationController.disabledTrustee)

      "return a 200" in {
        keystoreFetchCondition[DisabledTrusteeModel](None)
        status(DisabledTrusteeTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {
        "contain some text and use the character set utf-8" in {
          keystoreFetchCondition[DisabledTrusteeModel](None)
          contentType(DisabledTrusteeTestDataItem.result) shouldBe Some("text/html")
          charset(DisabledTrusteeTestDataItem.result) shouldBe Some("utf-8")
        }

        "have the title Are you a trustee for someone who’s vulnerable?" in {
          keystoreFetchCondition[DisabledTrusteeModel](None)
          DisabledTrusteeTestDataItem.jsoupDoc.title shouldEqual Messages("calc.disabledTrustee.question")
        }

        "have the heading Calculate your tax (non-residents) " in {
          keystoreFetchCondition[DisabledTrusteeModel](None)
          DisabledTrusteeTestDataItem.jsoupDoc.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a 'Back' link " in {
          keystoreFetchCondition[DisabledTrusteeModel](None)
          DisabledTrusteeTestDataItem.jsoupDoc.body.getElementById("link-back").text shouldEqual Messages("calc.base.back")
        }

        "have the question 'When did you sign the contract that made someone else the owner?' as the legend of the input" in {
          keystoreFetchCondition[DisabledTrusteeModel](None)
          DisabledTrusteeTestDataItem.jsoupDoc.body.getElementsByTag("legend").text shouldEqual Messages("calc.disabledTrustee.question")
        }

        "display a radio button with the option 'Yes'" in {
          keystoreFetchCondition[DisabledTrusteeModel](None)
          DisabledTrusteeTestDataItem.jsoupDoc.body.getElementById("isVulnerable-yes").parent.text shouldEqual Messages("calc.base.yes")
        }
        "display a radio button with the option 'No'" in {
          keystoreFetchCondition[DisabledTrusteeModel](None)
          DisabledTrusteeTestDataItem.jsoupDoc.body.getElementById("isVulnerable-no").parent.text shouldEqual Messages("calc.base.no")
        }

        "display a 'Continue' button " in {
          keystoreFetchCondition[DisabledTrusteeModel](None)
          DisabledTrusteeTestDataItem.jsoupDoc.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
        }
      }
    }

    "supplied with a pre-existing stored model" should {

      "return some HTML that" should {

        "have the radio option `Yes` selected if `Yes` is supplied in the model" in {
          object DisabledTrusteeTestDataItem extends fakeRequestTo("disabled-trustee", TestCalculationController.disabledTrustee)
          keystoreFetchCondition[DisabledTrusteeModel](Some(DisabledTrusteeModel("Yes")))
          DisabledTrusteeTestDataItem.jsoupDoc.body.getElementById("isVulnerable-yes").parent.classNames().contains("selected") shouldBe true
        }

        "have the radio option `No` selected if `No` is supplied in the model" in {
          object DisabledTrusteeTestDataItem extends fakeRequestTo("disabled-trustee", TestCalculationController.disabledTrustee)
          keystoreFetchCondition[DisabledTrusteeModel](Some(DisabledTrusteeModel("No")))
          DisabledTrusteeTestDataItem.jsoupDoc.body.getElementById("isVulnerable-no").parent.classNames().contains("selected") shouldBe true
        }
      }
    }
  }

  "In CalculationController calling the .submitDisabledTrustee action " should {

    def keystoreCacheCondition[T](data: DisabledTrusteeModel): Unit = {
      lazy val returnedCacheMap = CacheMap("form-id", Map("data" -> Json.toJson(data)))
      when(mockCalcConnector.saveFormData[T](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(returnedCacheMap))
    }

    "render errors when no option is selected" in {
      object DisabledTrusteeTestDataItem extends fakeRequestToPost(
        "disabled-trustee",
        TestCalculationController.submitDisabledTrustee,
        ("", "")
      )
      status(DisabledTrusteeTestDataItem.result) shouldBe 400
    }

    "when 'Yes' is selected" should {
      object DisabledTrusteeTestDataItem extends fakeRequestToPost(
        "disabled-trustee",
        TestCalculationController.submitDisabledTrustee,
        ("isVulnerable", "Yes")
      )

      "return a 303" in {
        status(DisabledTrusteeTestDataItem.result) shouldBe 303
      }

      "redirect to the other-properties page" in {
        redirectLocation(DisabledTrusteeTestDataItem.result) shouldBe Some(s"${routes.CalculationController.otherProperties}")
      }
    }
    "when 'No' is selected" should {
      object DisabledTrusteeTestDataItem extends fakeRequestToPost(
        "disabled-trustee",
        TestCalculationController.submitDisabledTrustee,
        ("isVulnerable", "No")
      )

      "return a 303" in {
        status(DisabledTrusteeTestDataItem.result) shouldBe 303
      }

      "redirect to the other-properties page" in {
        redirectLocation(DisabledTrusteeTestDataItem.result) shouldBe Some(s"${routes.CalculationController.otherProperties}")
      }
    }
  }

  //############## Personal Allowance tests ######################
  "In CalculationController calling the .personalAllowance action " should {
    "not supplied with a model that already contains data" should {
      object PersonalAllowanceTestDataItem extends fakeRequestTo("personal-allowance", TestCalculationController.personalAllowance)

      "return a 200" in {
        keystoreFetchCondition[PersonalAllowanceModel](None)
        status(PersonalAllowanceTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          keystoreFetchCondition[PersonalAllowanceModel](None)
          contentType(PersonalAllowanceTestDataItem.result) shouldBe Some("text/html")
          charset(PersonalAllowanceTestDataItem.result) shouldBe Some("utf-8")
        }

        "have the title In the tax year when you stopped owning the property, what was your UK Personal Allowance?" in {
          keystoreFetchCondition[PersonalAllowanceModel](None)
          PersonalAllowanceTestDataItem.jsoupDoc.title shouldEqual Messages("calc.personalAllowance.question")
        }

        "have the heading Calculate your tax (non-residents) " in {
          keystoreFetchCondition[PersonalAllowanceModel](None)
          PersonalAllowanceTestDataItem.jsoupDoc.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a 'Back' link " in {
          keystoreFetchCondition[PersonalAllowanceModel](None)
          PersonalAllowanceTestDataItem.jsoupDoc.body.getElementById("link-back").text shouldEqual Messages("calc.base.back")
        }

        "have the question 'In the tax year when you stopped owning the property, what was your UK Personal Allowance?' as the label of the input" in {
          keystoreFetchCondition[PersonalAllowanceModel](None)
          PersonalAllowanceTestDataItem.jsoupDoc.body.getElementsByTag("label").text shouldEqual Messages("calc.personalAllowance.question")
        }

        "display an input box for the Personal Allowance" in {
          keystoreFetchCondition[PersonalAllowanceModel](None)
          PersonalAllowanceTestDataItem.jsoupDoc.body.getElementById("personalAllowance").tagName() shouldEqual "input"
        }

        "have no value auto-filled into the input box" in {
          keystoreFetchCondition[PersonalAllowanceModel](None)
          PersonalAllowanceTestDataItem.jsoupDoc.getElementById("personalAllowance").attr("value") shouldBe empty
        }

        "display a 'Continue' button " in {
          keystoreFetchCondition[PersonalAllowanceModel](None)
          PersonalAllowanceTestDataItem.jsoupDoc.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
        }

        "should contain a Read more sidebar with a link to personal allowances and taxation abroad" in {
          keystoreFetchCondition[PersonalAllowanceModel](None)
          PersonalAllowanceTestDataItem.jsoupDoc.select("aside h2").text shouldBe Messages("calc.common.readMore")
          PersonalAllowanceTestDataItem.jsoupDoc.select("aside a").first().attr("href") shouldBe "https://www.gov.uk/income-tax-rates/current-rates-and-allowances"
          PersonalAllowanceTestDataItem.jsoupDoc.select("aside a").last().attr("href") shouldBe "https://www.gov.uk/tax-uk-income-live-abroad/personal-allowance"
        }
      }
    }

    "supplied with a model that already contains data" should {
      object PersonalAllowanceTestDataItem extends fakeRequestTo("personal-allowance", TestCalculationController.personalAllowance)
      val testModel = new PersonalAllowanceModel(1000)

      "return a 200" in {
        keystoreFetchCondition[PersonalAllowanceModel](Some(testModel))
        status(PersonalAllowanceTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "have the value 1000 auto-filled into the input box" in {
          keystoreFetchCondition[PersonalAllowanceModel](Some(testModel))
          PersonalAllowanceTestDataItem.jsoupDoc.getElementById("personalAllowance").attr("value") shouldEqual ("1000")
        }
      }
    }
  }

  //############## Other Properties tests ######################
  "In CalculationController calling the .otherProperties action " when {

    "not supplied with a model that already contains data" should {

      object OtherPropertiesTestDataItem extends fakeRequestTo("other-properties", TestCalculationController.otherProperties)

      "return a 200" in {
        keystoreFetchCondition[OtherPropertiesModel](None)
        status(OtherPropertiesTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          contentType(OtherPropertiesTestDataItem.result) shouldBe Some("text/html")
          charset(OtherPropertiesTestDataItem.result) shouldBe Some("utf-8")
        }

        "have the title 'Did you sell or give away any other properties in that tax year?'" in {
          OtherPropertiesTestDataItem.jsoupDoc.title shouldEqual Messages("calc.otherProperties.question")
        }

        "have the heading Calculate your tax (non-residents) " in {
          OtherPropertiesTestDataItem.jsoupDoc.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a 'Back' link " in {
          OtherPropertiesTestDataItem.jsoupDoc.body.getElementById("link-back").text shouldEqual Messages("calc.base.back")
        }

        "have the question 'Did you sell or give away any other properties in that tax year?' as the legend of the input" in {
          OtherPropertiesTestDataItem.jsoupDoc.body.getElementsByTag("legend").text shouldEqual Messages("calc.otherProperties.question")
        }

        "display a radio button with the option `Yes`" in {
          OtherPropertiesTestDataItem.jsoupDoc.body.getElementById("otherProperties-yes").parent.text shouldEqual Messages("calc.base.yes")
        }

        "display a radio button with the option `No`" in {
          OtherPropertiesTestDataItem.jsoupDoc.body.getElementById("otherProperties-no").parent.text shouldEqual Messages("calc.base.no")
        }

        "display a 'Continue' button " in {
          OtherPropertiesTestDataItem.jsoupDoc.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
        }
      }
    }

    "supplied with a model that already contains data" should {

      object OtherPropertiesTestDataItem extends fakeRequestTo("other-properties", TestCalculationController.otherProperties)
      val otherPropertiesTestModel = new OtherPropertiesModel("Yes")

      "return a 200" in {
        keystoreFetchCondition[OtherPropertiesModel](Some(otherPropertiesTestModel))
        status(OtherPropertiesTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {
        "contain some text and use the character set utf-8" in {
          contentType(OtherPropertiesTestDataItem.result) shouldBe Some("text/html")
          charset(OtherPropertiesTestDataItem.result) shouldBe Some("utf-8")
        }

        "have the radio option `Yes` selected by default" in {
          keystoreFetchCondition[OtherPropertiesModel](Some(otherPropertiesTestModel))
          OtherPropertiesTestDataItem.jsoupDoc.body.getElementById("otherProperties-yes").parent.classNames().contains("selected") shouldBe true

        }
      }
    }
  }

  "In CalculationController calling the .submitOtherProperties action" when {
    def keystoreCacheCondition[T](data: OtherPropertiesModel): Unit = {
      lazy val returnedCacheMap = CacheMap("form-id", Map("data" -> Json.toJson(data)))
      when(mockCalcConnector.saveFormData[T](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(returnedCacheMap))
    }
    "submitting a valid form with 'Yes'" should {
      object OtherPropertiesTestDataItem extends fakeRequestToPost(
        "allowance",
        TestCalculationController.submitOtherProperties,
        ("otherProperties", "Yes")
      )
      val testModel = new OtherPropertiesModel("Yes")

      "return a 303" in {
        keystoreCacheCondition[OtherPropertiesModel](testModel)
        status(OtherPropertiesTestDataItem.result) shouldBe 303
      }
    }

    "submitting a valid form with 'No'" should {
      object OtherPropertiesTestDataItem extends fakeRequestToPost(
        "allowance",
        TestCalculationController.submitOtherProperties,
        ("otherProperties", "No")
      )
      val testModel = new OtherPropertiesModel("No")

      "return a 303" in {
        keystoreCacheCondition[OtherPropertiesModel](testModel)
        status(OtherPropertiesTestDataItem.result) shouldBe 303
      }
    }

    "submitting an invalid form" should {
      object OtherPropertiesTestDataItem extends fakeRequestToPost(
        "allowance",
        TestCalculationController.submitOtherProperties,
        ("annualExemptAmount", "")
      )
      val testModel = new OtherPropertiesModel("")

      "return a 400" in {
        keystoreCacheCondition[AnnualExemptAmountModel](testModel)
        status(OtherPropertiesTestDataItem.result) shouldBe 400
      }
    }
  }

  //############## Annual Exempt Amount tests ######################
  "In CalculationController calling the .annualExemptAmount action " when {
    "not supplied with a pre-existing stored model" should {
      object AnnualExemptAmountTestDataItem extends fakeRequestTo("allowance", TestCalculationController.annualExemptAmount)

      "return a 200" in {
        keystoreFetchCondition[AnnualExemptAmountModel](None)
        status(AnnualExemptAmountTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          keystoreFetchCondition[AnnualExemptAmountModel](None)
          contentType(AnnualExemptAmountTestDataItem.result) shouldBe Some("text/html")
          charset(AnnualExemptAmountTestDataItem.result) shouldBe Some("utf-8")
        }

        "have the title 'How much of your Capital Gains Tax allowance have you got left?'" in {
          keystoreFetchCondition[AnnualExemptAmountModel](None)
          AnnualExemptAmountTestDataItem.jsoupDoc.title shouldEqual Messages("calc.annualExemptAmount.question")
        }

        "have the heading Calculate your tax (non-residents) " in {
          keystoreFetchCondition[AnnualExemptAmountModel](None)
          AnnualExemptAmountTestDataItem.jsoupDoc.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a 'Back' link " in {
          keystoreFetchCondition[AnnualExemptAmountModel](None)
          AnnualExemptAmountTestDataItem.jsoupDoc.body.getElementById("link-back").text shouldEqual Messages("calc.base.back")
        }

        "have the question 'How much of your Capital Gains Tax allowance have you got left?' as the legend of the input" in {
          keystoreFetchCondition[AnnualExemptAmountModel](None)
          AnnualExemptAmountTestDataItem.jsoupDoc.body.getElementsByTag("label").text shouldEqual Messages("calc.annualExemptAmount.question")
        }

        "display an input box for the Annual Exempt Amount" in {
          keystoreFetchCondition[AnnualExemptAmountModel](None)
          AnnualExemptAmountTestDataItem.jsoupDoc.body.getElementById("annualExemptAmount").tagName() shouldEqual "input"
        }

        "have no value auto-filled into the input box" in {
          keystoreFetchCondition[AnnualExemptAmountModel](None)
          AnnualExemptAmountTestDataItem.jsoupDoc.getElementById("annualExemptAmount").attr("value") shouldBe empty
        }

        "display a 'Continue' button " in {
          keystoreFetchCondition[AnnualExemptAmountModel](None)
          AnnualExemptAmountTestDataItem.jsoupDoc.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
        }

        "should contain a Read more sidebar with a link to CGT allowances" in {
          keystoreFetchCondition[AnnualExemptAmountModel](None)
          AnnualExemptAmountTestDataItem.jsoupDoc.select("aside h2").text shouldBe Messages("calc.common.readMore")
          AnnualExemptAmountTestDataItem.jsoupDoc.select("aside a").text shouldBe Messages("calc.annualExemptAmount.link.one")
        }
      }
    }
  }

  "supplied with a pre-existing stored model" should {
    object AnnualExemptAmountTestDataItem extends fakeRequestTo("allowance", TestCalculationController.annualExemptAmount)
    val testModel = new AnnualExemptAmountModel(1000)

    "return a 200" in {
      keystoreFetchCondition[AnnualExemptAmountModel](Some(testModel))
      status(AnnualExemptAmountTestDataItem.result) shouldBe 200
    }

    "return some HTML that" should {

      "contain some text and use the character set utf-8" in {
        keystoreFetchCondition[AnnualExemptAmountModel](Some(testModel))
        contentType(AnnualExemptAmountTestDataItem.result) shouldBe Some("text/html")
        charset(AnnualExemptAmountTestDataItem.result) shouldBe Some("utf-8")
      }

      "have the value 1000 auto-filled into the input box" in {
        keystoreFetchCondition[AnnualExemptAmountModel](Some(testModel))
        AnnualExemptAmountTestDataItem.jsoupDoc.getElementById("annualExemptAmount").attr("value") shouldEqual ("1000")
      }
    }
  }

  "In CalculationController calling the .submitAnnualExemptAmount action" when {
    def keystoreCacheCondition[T](data: AnnualExemptAmountModel): Unit = {
      lazy val returnedCacheMap = CacheMap("form-id", Map("data" -> Json.toJson(data)))
      when(mockCalcConnector.saveFormData[T](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(returnedCacheMap))
    }
    "submitting a valid form" should {
      object AnnualExemptAmountTestDataItem extends fakeRequestToPost(
        "allowance",
        TestCalculationController.submitAnnualExemptAmount,
        ("annualExemptAmount", "1000")
      )
      val testModel = new AnnualExemptAmountModel(1000)

      "return a 303" in {
        keystoreCacheCondition[AnnualExemptAmountModel](testModel)
        status(AnnualExemptAmountTestDataItem.result) shouldBe 303
      }
    }

    "submitting an invalid form with no value" should {
      object AnnualExemptAmountTestDataItem extends fakeRequestToPost(
        "allowance",
        TestCalculationController.submitAnnualExemptAmount,
        ("annualExemptAmount", "")
      )
      val testModel = new AnnualExemptAmountModel(0)

      "return a 400" in {
        keystoreCacheCondition[AnnualExemptAmountModel](testModel)
        status(AnnualExemptAmountTestDataItem.result) shouldBe 400
      }
    }

    "submitting an invalid form above the maximum value" should {
      object AnnualExemptAmountTestDataItem extends fakeRequestToPost(
        "allowance",
        TestCalculationController.submitAnnualExemptAmount,
        ("annualExemptAmount", "15000")
      )
      val testModel = new AnnualExemptAmountModel(15000)

      "return a 400" in {
        keystoreCacheCondition[AnnualExemptAmountModel](testModel)
        status(AnnualExemptAmountTestDataItem.result) shouldBe 400
      }
    }

    "submitting an invalid form below the minimum" should {
      object AnnualExemptAmountTestDataItem extends fakeRequestToPost(
        "allowance",
        TestCalculationController.submitAnnualExemptAmount,
        ("annualExemptAmount", "-1000")
      )
      val testModel = new AnnualExemptAmountModel(-1000)

      "return a 400" in {
        keystoreCacheCondition[AnnualExemptAmountModel](testModel)
        status(AnnualExemptAmountTestDataItem.result) shouldBe 400
      }
    }
  }

  //############## Acquisition Value tests ######################
  "In CalculationController calling the .acquisitionValue action " when {
    "not supplied with a pre-existing stored model" should {
      object AcquisitionValueTestDataItem extends fakeRequestTo("acquisition-value", TestCalculationController.acquisitionValue)

      "return a 200" in {
        keystoreFetchCondition[AcquisitionValueModel](None)
        status(AcquisitionValueTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          keystoreFetchCondition[AcquisitionValueModel](None)
          contentType(AcquisitionValueTestDataItem.result) shouldBe Some("text/html")
          charset(AcquisitionValueTestDataItem.result) shouldBe Some("utf-8")
        }

        "have the title 'How much did you pay for the property?'" in {
          keystoreFetchCondition[AcquisitionValueModel](None)
          AcquisitionValueTestDataItem.jsoupDoc.title shouldEqual Messages("calc.acquisitionValue.question")
        }

        "have the heading Calculate your tax (non-residents) " in {
          keystoreFetchCondition[AcquisitionValueModel](None)
          AcquisitionValueTestDataItem.jsoupDoc.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a 'Back' link " in {
          keystoreFetchCondition[AcquisitionValueModel](None)
          AcquisitionValueTestDataItem.jsoupDoc.body.getElementById("link-back").text shouldEqual Messages("calc.base.back")
        }

        "have the question 'How much did you pay for the property?'" in {
          keystoreFetchCondition[AcquisitionValueModel](None)
          AcquisitionValueTestDataItem.jsoupDoc.body.getElementsByTag("label").text shouldEqual Messages("calc.acquisitionValue.question")
        }

        "display an input box for the Acquisition Value" in {
          keystoreFetchCondition[AcquisitionValueModel](None)
          AcquisitionValueTestDataItem.jsoupDoc.body.getElementById("acquisitionValue").tagName shouldEqual "input"
        }
        "have no value auto-filled into the input box" in {
          keystoreFetchCondition[AcquisitionValueModel](None)
          AcquisitionValueTestDataItem.jsoupDoc.getElementById("acquisitionValue").attr("value") shouldEqual ""
        }
        "display a 'Continue' button " in {
          keystoreFetchCondition[AcquisitionValueModel](None)
          AcquisitionValueTestDataItem.jsoupDoc.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
        }
      }
    }

    "supplied with a pre-existing stored model" should {
      val testModel = new AcquisitionValueModel(1000)
      object AcquisitionValueTestDataItem extends fakeRequestTo("acquisition-value", TestCalculationController.acquisitionValue)

      "return a 200" in {
        keystoreFetchCondition[AcquisitionValueModel](Some(testModel))
        status(AcquisitionValueTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          keystoreFetchCondition[AcquisitionValueModel](Some(testModel))
          contentType(AcquisitionValueTestDataItem.result) shouldBe Some("text/html")
          charset(AcquisitionValueTestDataItem.result) shouldBe Some("utf-8")
        }

        "have the value 1000 auto-filled into the input box" in {
          keystoreFetchCondition[AcquisitionValueModel](Some(testModel))
          AcquisitionValueTestDataItem.jsoupDoc.getElementById("acquisitionValue").attr("value") shouldEqual "1000"
        }
      }
    }
  }

  "In CalculationController calling the .submitAcquisitionValue action" when {
    def keystoreCacheCondition[T](data: AcquisitionValueModel): Unit = {
      lazy val returnedCacheMap = CacheMap("form-id", Map("data" -> Json.toJson(data)))
      when(mockCalcConnector.saveFormData[T](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(returnedCacheMap))
    }

    "submitting a valid form" should {
      val testModel = new AcquisitionValueModel(1000)
      object AcquisitionValueTestDataItem extends fakeRequestToPost (
        "acquisition-value",
        TestCalculationController.submitAcquisitionValue,
        ("acquisitionValue", "1000")
      )

      "return a 303" in {
        keystoreCacheCondition(testModel)
        status(AcquisitionValueTestDataItem.result) shouldBe 303
      }
    }

    "submitting an invalid form with no value" should {
      val testModel = new AcquisitionValueModel(0)
      object AcquisitionValueTestDataItem extends fakeRequestToPost (
        "acquisition-value",
        TestCalculationController.submitAcquisitionValue,
        ("acquisitionValue", "")
      )

      "return a 400" in {
        keystoreCacheCondition(testModel)
        status(AcquisitionValueTestDataItem.result) shouldBe 400
      }
    }

    "submitting an invalid form with a negative value" should {
      val testModel = new AcquisitionValueModel(-1000)
      object AcquisitionValueTestDataItem extends fakeRequestToPost (
        "acquisition-value",
        TestCalculationController.submitAcquisitionValue,
        ("acquisitionValue", "-1000")
      )

      "return a 400" in {
        keystoreCacheCondition(testModel)
        status(AcquisitionValueTestDataItem.result) shouldBe 400
      }
    }
  }

  //################### Improvements tests #######################
  "In CalculationController calling the .improvements action " when {
    "not supplied with a pre-existing stored model" should {
      object ImprovementsTestDataItem extends fakeRequestTo("improvements", TestCalculationController.improvements)

      "return a 200" in {
        keystoreFetchCondition[ImprovementsModel](None)
        status(ImprovementsTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          contentType(ImprovementsTestDataItem.result) shouldBe Some("text/html")
          charset(ImprovementsTestDataItem.result) shouldBe Some("utf-8")
        }

        "have the title 'Who owned the property?'" in {
          ImprovementsTestDataItem.jsoupDoc.title shouldEqual Messages("calc.improvements.question")
        }

        "have the heading Calculate your tax (non-residents)" in {
          ImprovementsTestDataItem.jsoupDoc.body.getElementsByTag("H1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "display the correct wording for radio option `yes`" in {
          ImprovementsTestDataItem.jsoupDoc.body.getElementById("isClaimingImprovements-yes").parent.text shouldEqual Messages("calc.base.yes")
        }

        "display the correct wording for radio option `no`" in {
          ImprovementsTestDataItem.jsoupDoc.body.getElementById("isClaimingImprovements-no").parent.text shouldEqual Messages("calc.base.no")
        }

        "contain a hidden component with an input box" in {
          ImprovementsTestDataItem.jsoupDoc.body.getElementById("improvementsAmt").parent.parent.parent.id shouldBe "hidden"
        }
      }
    }
    "supplied with a pre-existing model with 'Yes' checked and value already entered" should {
      val testImprovementsModelYes = new ImprovementsModel("Yes", Some(10000))

      "return a 200" in {
        object ImprovementsTestDataItem extends fakeRequestTo("improvements", TestCalculationController.improvements)
        keystoreFetchCondition[ImprovementsModel](Some(testImprovementsModelYes))
        status(ImprovementsTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "be pre populated with Yes box selected and a value of 10000 entered" in {
          object ImprovementsTestDataItem extends fakeRequestTo("improvements", TestCalculationController.improvements)
          keystoreFetchCondition[ImprovementsModel](Some(testImprovementsModelYes))

          ImprovementsTestDataItem.jsoupDoc.getElementById("isClaimingImprovements-yes").attr("checked") shouldEqual "checked"
          ImprovementsTestDataItem.jsoupDoc.getElementById("improvementsAmt").attr("value") shouldEqual "10000"
        }
      }
    }
    "supplied with a pre-existing model with 'No' checked and value already entered" should {
      val testImprovementsModelNo = new ImprovementsModel("No", Some(0))

      "return a 200" in {
        object ImprovementsTestDataItem extends fakeRequestTo("improvements", TestCalculationController.improvements)
        keystoreFetchCondition[ImprovementsModel](Some(testImprovementsModelNo))
        status(ImprovementsTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "be pre populated with No box selected and a value of 0" in {
          object ImprovementsTestDataItem extends fakeRequestTo("improvements", TestCalculationController.improvements)
          keystoreFetchCondition[ImprovementsModel](Some(testImprovementsModelNo))

          ImprovementsTestDataItem.jsoupDoc.getElementById("isClaimingImprovements-no").attr("checked") shouldEqual "checked"
          ImprovementsTestDataItem.jsoupDoc.getElementById("improvementsAmt").attr("value") shouldEqual "0"
        }
      }
    }
  }

  "In CalculationController calling the .submitImprovements action " when {
    def keystoreCacheCondition[T](data: ImprovementsModel): Unit = {
      lazy val returnedCacheMap = CacheMap("form-id", Map("data" -> Json.toJson(data)))
      when(mockCalcConnector.saveFormData[T](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(returnedCacheMap))
    }
    
    "submitting a valid form with 'Yes' and a value of 12045" should {
      object ImprovementsTestDataItem extends fakeRequestToPost("improvments", TestCalculationController.submitImprovements, ("isClaimingImprovements", "Yes"), ("improvementsAmt", "12045"))
      val improvementsTestModel = new ImprovementsModel("Yes", Some(12045))

      "return a 303" in {
        keystoreCacheCondition[ImprovementsModel](improvementsTestModel)
        status(ImprovementsTestDataItem.result) shouldBe 303
      }
    }

    "submitting an invalid form with 'Yes' and a value of 'fhu39awd8'" should {
      object ImprovementsTestDataItem extends fakeRequestToPost("improvments", TestCalculationController.submitImprovements, ("isClaimingImprovements", "Yes"), ("improvementsAmt", "fhu39awd8"))
      //This model actually has no bearing on the tes but the cachemap it produces is required.
      val improvementsTestModel = new ImprovementsModel("Yes", Some(9878))

      "return a 400" in {
        keystoreCacheCondition[ImprovementsModel](improvementsTestModel)
        status(ImprovementsTestDataItem.result) shouldBe 400
      }
    }
  }

  //################### Disposal Date tests #######################
  "In CalculationController calling the .disposalDate action " when {
    "not supplied with a pre-existing stored model" should {

      object DisposalDateTestDataItem extends fakeRequestTo("disposal-date", TestCalculationController.disposalDate)

      "return a 200" in {
        keystoreFetchCondition[DisposalDateModel](None)
        status(DisposalDateTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          contentType(DisposalDateTestDataItem.result) shouldBe Some("text/html")
          charset(DisposalDateTestDataItem.result) shouldBe Some("utf-8")
        }

        "have the title 'When did you sign the contract that made someone else the owner?'" in {
          DisposalDateTestDataItem.jsoupDoc.title shouldEqual Messages("calc.disposalDate.question")
        }

        "have the heading Calculate your tax (non-residents) " in {
          DisposalDateTestDataItem.jsoupDoc.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a 'Back' link " in {
          DisposalDateTestDataItem.jsoupDoc.body.getElementById("link-back").text shouldEqual Messages("calc.base.back")
        }

        "have the question 'Who owned the property?' as the legend of the input" in {
          DisposalDateTestDataItem.jsoupDoc.body.getElementsByTag("legend").text shouldEqual Messages("calc.disposalDate.question")
        }

        "display three input boxes with labels Day, Month and Year respectively" in {
          DisposalDateTestDataItem.jsoupDoc.select("label[for=disposalDate.day]").text shouldEqual Messages("calc.common.date.fields.day")
          DisposalDateTestDataItem.jsoupDoc.select("label[for=disposalDate.month]").text shouldEqual Messages("calc.common.date.fields.month")
          DisposalDateTestDataItem.jsoupDoc.select("label[for=disposalDate.year]").text shouldEqual Messages("calc.common.date.fields.year")
        }

        "display a 'Continue' button " in {
          DisposalDateTestDataItem.jsoupDoc.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
        }
      }
    }

    "supplied with a model already filled with data" should {
      object DisposalDateTestDataItem extends fakeRequestTo("disposal-date", TestCalculationController.disposalDate)
      val testDisposalDateModel = new DisposalDateModel(10, 12, 2016)

      "return a 200" in {
        keystoreFetchCondition[DisposalDateModel](Some(testDisposalDateModel))
        status(DisposalDateTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {
        "contain some text and use the character set utf-8" in {
          contentType(DisposalDateTestDataItem.result) shouldBe Some("text/html")
          charset(DisposalDateTestDataItem.result) shouldBe Some("utf-8")
        }

        "be pre-populated with the date 10, 12, 2016" in {
          keystoreFetchCondition[DisposalDateModel](Some(testDisposalDateModel))
          DisposalDateTestDataItem.jsoupDoc.body.getElementById("disposalDate.day").attr("value") shouldEqual testDisposalDateModel.day.toString
          DisposalDateTestDataItem.jsoupDoc.body.getElementById("disposalDate.month").attr("value") shouldEqual testDisposalDateModel.month.toString
          DisposalDateTestDataItem.jsoupDoc.body.getElementById("disposalDate.year").attr("value") shouldEqual testDisposalDateModel.year.toString
        }
      }
    }
  }

  //################### Disposal Value tests #######################
  "In CalculationController calling the .disposalValue action " when {
    "not supplied with a pre-existing stored model" should {
      object DisposalValueTestDataItem extends fakeRequestTo("disposal-value", TestCalculationController.disposalValue)

      "return a 200" in {
        keystoreFetchCondition[DisposalValueModel](None)
        status(DisposalValueTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          keystoreFetchCondition[DisposalValueModel](None)
          contentType(DisposalValueTestDataItem.result) shouldBe Some("text/html")
          charset(DisposalValueTestDataItem.result) shouldBe Some("utf-8")
        }

        "have the title 'How much did you sell or give away the property for?'" in {
          keystoreFetchCondition[DisposalValueModel](None)
          DisposalValueTestDataItem.jsoupDoc.title shouldEqual Messages("calc.disposalValue.question")
        }

        "have the heading Calculate your tax (non-residents) " in {
          keystoreFetchCondition[DisposalValueModel](None)
          DisposalValueTestDataItem.jsoupDoc.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a 'Back' link " in {
          keystoreFetchCondition[DisposalValueModel](None)
          DisposalValueTestDataItem.jsoupDoc.body.getElementById("link-back").text shouldEqual Messages("calc.base.back")
        }

        "have the question 'How much did you sell or give away the property for?' as the legend of the input" in {
          keystoreFetchCondition[DisposalValueModel](None)
          DisposalValueTestDataItem.jsoupDoc.body.getElementsByTag("label").text shouldEqual Messages("calc.disposalValue.question")
        }

        "display an input box for the Annual Exempt Amount" in {
          keystoreFetchCondition[DisposalValueModel](None)
          DisposalValueTestDataItem.jsoupDoc.body.getElementById("disposalValue").tagName() shouldEqual "input"
        }

        "display a 'Continue' button " in {
          keystoreFetchCondition[DisposalValueModel](None)
          DisposalValueTestDataItem.jsoupDoc.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
        }
      }
    }
    "supplied with a pre-existing stored model" should {
      object DisposalValueTestDataItem extends fakeRequestTo("disposal-value", TestCalculationController.disposalValue)
      val testModel = new DisposalValueModel(1000)
      "return a 200" in {
        keystoreFetchCondition[DisposalValueModel](Some(testModel))
        status(DisposalValueTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          keystoreFetchCondition[DisposalValueModel](Some(testModel))
          contentType(DisposalValueTestDataItem.result) shouldBe Some("text/html")
          charset(DisposalValueTestDataItem.result) shouldBe Some("utf-8")
        }

        "have the value 1000 auto-filled into the input box" in {
          keystoreFetchCondition[DisposalValueModel](Some(testModel))
          DisposalValueTestDataItem.jsoupDoc.getElementById("disposalValue").attr("value") shouldEqual ("1000")
        }
      }
    }
  }

  //################### Acquisition Costs tests #######################
  "In CalculationController calling the .acquisitionCosts action " should {

    object AcquisitionCostsTestDataItem extends fakeRequestTo("acquisition-costs", CalculationController.acquisitionCosts)

    "return a 200" in {
      status(AcquisitionCostsTestDataItem.result) shouldBe 200
    }

    "return some HTML that" should {

      "contain some text and use the character set utf-8" in {
        contentType(AcquisitionCostsTestDataItem.result) shouldBe Some("text/html")
        charset(AcquisitionCostsTestDataItem.result) shouldBe Some("utf-8")
      }

      "have the title 'How much did you pay in costs when you became the property owner'" in {
        AcquisitionCostsTestDataItem.jsoupDoc.getElementsByTag("title").text shouldEqual Messages("calc.acquisitionCosts.question")
      }

      "have a back link" in {
        AcquisitionCostsTestDataItem.jsoupDoc.getElementById("link-back").text shouldEqual Messages("calc.base.back")
      }

      "have the page heading 'Calculate your tax (non-residents)'" in {
        AcquisitionCostsTestDataItem.jsoupDoc.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
      }

      "have a monetary field that" should {

        "have the title 'How much did you pay in costs when you became the property owner?'" in {
          AcquisitionCostsTestDataItem.jsoupDoc.select("label[for=acquisitionCosts]").text.contains(Messages("calc.acquisitionCosts.question")) shouldBe true
        }

        "have the help text 'Costs include agent fees, legal fees and surveys'" in {
          AcquisitionCostsTestDataItem.jsoupDoc.select("span.form-hint").text shouldEqual Messages("calc.acquisitionCosts.helpText")
        }

        "have an input box for the acquisition costs" in {
          AcquisitionCostsTestDataItem.jsoupDoc.getElementById("acquisitionCosts").tagName shouldBe "input"
        }
      }

      "have a continue button that" should {

        "be a button element" in {
          AcquisitionCostsTestDataItem.jsoupDoc.getElementById("continue-button").tagName shouldBe "button"
        }

        "have the text 'Continue'" in {
          AcquisitionCostsTestDataItem.jsoupDoc.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
        }
      }
    }
  }

  "In CalculationController calling the .submitAcquisitionCosts action" when {
    def keystoreCacheCondition[T](data: AcquisitionCostsModel): Unit = {
      lazy val returnedCacheMap = CacheMap("form-id", Map("data" -> Json.toJson(data)))
      when(mockCalcConnector.saveFormData[T](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(returnedCacheMap))
    }

    "submitting a valid form" should {
      val testModel = new AcquisitionCostsModel(1000)
      object AcquisitionCostsTestDataItem extends fakeRequestToPost(
        "acquisition-costs",
        TestCalculationController.submitAcquisitionCosts,
        ("acquisitionCosts", "1000")
      )

      "return a 303" in {
        keystoreCacheCondition(testModel)
        status(AcquisitionCostsTestDataItem.result) shouldBe 303
      }
    }

    "submitting an invalid form with no value" should {
      val testModel = new AcquisitionCostsModel(0)
      object AcquisitionCostsTestDataItem extends fakeRequestToPost(
        "acquisition-costs",
        TestCalculationController.submitAcquisitionCosts,
        ("acquisitionCosts", "")
      )

      "return a 400" in {
        keystoreCacheCondition(testModel)
        status(AcquisitionCostsTestDataItem.result) shouldBe 400
      }
    }
  }

  //################### Disposal Costs tests #######################
  "In CalculationController calling the .disposalCosts action " should {
    "not supplied with a pre-existing stored model" should {
      object DisposalCostsTestDataItem extends fakeRequestTo("disposal-costs", TestCalculationController.disposalCosts)

      "return a 200" in {
        keystoreFetchCondition[DisposalCostsModel](None)
        status(DisposalCostsTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          contentType(DisposalCostsTestDataItem.result) shouldBe Some("text/html")
          charset(DisposalCostsTestDataItem.result) shouldBe Some("utf-8")
        }

        "have the title 'How much did you pay in costs when you stopped being the property owner?'" in {
          DisposalCostsTestDataItem.jsoupDoc.getElementsByTag("title").text shouldBe Messages("calc.disposalCosts.question")
        }

        "have a back link" in {
          DisposalCostsTestDataItem.jsoupDoc.getElementById("link-back").text shouldEqual Messages("calc.base.back")
        }

        "have the heading 'Calculate your tax (non-residents)'" in {
          DisposalCostsTestDataItem.jsoupDoc.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a monetary field that" should {

          "have the title 'How much did you pay in costs when you became the property owner?'" in {
            DisposalCostsTestDataItem.jsoupDoc.select("label[for=disposalCosts]").text shouldEqual Messages("calc.disposalCosts.question")
          }

          "have an input box for the disposal costs" in {
            DisposalCostsTestDataItem.jsoupDoc.getElementById("disposalCosts").tagName shouldBe "input"
          }
        }

        "have a continue button that" should {

          "be a button element" in {
            DisposalCostsTestDataItem.jsoupDoc.getElementById("continue-button").tagName shouldBe "button"
          }

          "have the text 'Continue'" in {
            DisposalCostsTestDataItem.jsoupDoc.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
          }
        }
      }
    }
    "supplied with a pre-existing stored model" should {
      object DisposalCostsTestDataItem extends fakeRequestTo("disposal-costs", TestCalculationController.disposalCosts)
      val disposalCostsTestModel = new DisposalCostsModel(1000)

      "return a 200" in {
        keystoreFetchCondition[DisposalCostsModel](Some(disposalCostsTestModel))
        status(DisposalCostsTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          keystoreFetchCondition[DisposalCostsModel](Some(disposalCostsTestModel))
          contentType(DisposalCostsTestDataItem.result) shouldBe Some("text/html")
          charset(DisposalCostsTestDataItem.result) shouldBe Some("utf-8")
        }

        "have the value 1000 auto-filled into the input box" in {
          keystoreFetchCondition[DisposalCostsModel](Some(disposalCostsTestModel))
          DisposalCostsTestDataItem.jsoupDoc.getElementById("disposalCosts").attr("value") shouldEqual ("1000")
        }
      }

    }
  }

  //################### Entrepreneurs Relief tests #######################
  "In CalculationController calling the .entrepreneursRelief action " should {

    "not supplied with a pre-existing stored model" should {
      object EntrepreneursReliefTestDataItem extends fakeRequestTo("entrepreneurs-relief", TestCalculationController.entrepreneursRelief)

      "return a 200" in {
        keystoreFetchCondition[EntrepreneursReliefModel](None)
        status(EntrepreneursReliefTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          keystoreFetchCondition[EntrepreneursReliefModel](None)
          contentType(EntrepreneursReliefTestDataItem.result) shouldBe Some("text/html")
          charset(EntrepreneursReliefTestDataItem.result) shouldBe Some("utf-8")
        }

        "have the title 'Are you claiming Entrepreneurs Relief?'" in {
          keystoreFetchCondition[EntrepreneursReliefModel](None)
          EntrepreneursReliefTestDataItem.jsoupDoc.title shouldEqual Messages("calc.entrepreneursRelief.question")
        }

        "have the heading Calculate your tax (non-residents) " in {
          keystoreFetchCondition[EntrepreneursReliefModel](None)
          EntrepreneursReliefTestDataItem.jsoupDoc.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a 'Back' link " in {
          keystoreFetchCondition[EntrepreneursReliefModel](None)
          EntrepreneursReliefTestDataItem.jsoupDoc.body.getElementById("link-back").text shouldEqual Messages("calc.base.back")
        }

        "have the question 'Are you claiming Entrepreneurs Relief?' as the legend of the input" in {
          keystoreFetchCondition[EntrepreneursReliefModel](None)
          EntrepreneursReliefTestDataItem.jsoupDoc.body.getElementsByTag("legend").text shouldEqual Messages("calc.entrepreneursRelief.question")
        }

        "display a 'Continue' button " in {
          keystoreFetchCondition[EntrepreneursReliefModel](None)
          EntrepreneursReliefTestDataItem.jsoupDoc.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
        }

        "have a sidebar with additional links" in {
          keystoreFetchCondition[EntrepreneursReliefModel](None)
          EntrepreneursReliefTestDataItem.jsoupDoc.body.getElementsByClass("sidebar")
        }
      }
    }

    "supplied with a pre-existing stored model" should {

      "return a 200" in {
        object EntrepreneursReliefTestDataItem extends fakeRequestTo("entrepreneurs-relief", TestCalculationController.entrepreneursRelief)
        keystoreFetchCondition[EntrepreneursReliefModel](Some(EntrepreneursReliefModel("Yes")))
        status(EntrepreneursReliefTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "have the radio option `Yes` selected if `Yes` is supplied in the model" in {
          object EntrepreneursReliefTestDataItem extends fakeRequestTo("entrepreneurs-relief", TestCalculationController.entrepreneursRelief)
          keystoreFetchCondition[EntrepreneursReliefModel](Some(EntrepreneursReliefModel("Yes")))
          EntrepreneursReliefTestDataItem.jsoupDoc.body.getElementById("entrepreneursRelief-yes").parent.classNames().contains("selected") shouldBe true
        }

        "have the radio option `No` selected if `No` is supplied in the model" in {
          object EntrepreneursReliefTestDataItem extends fakeRequestTo("entrepreneurs-relief", TestCalculationController.entrepreneursRelief)
          keystoreFetchCondition[EntrepreneursReliefModel](Some(EntrepreneursReliefModel("No")))
          EntrepreneursReliefTestDataItem.jsoupDoc.body.getElementById("entrepreneursRelief-no").parent.classNames().contains("selected") shouldBe true
        }
      }
    }
  }


  //################### Allowable Losses tests #######################
  "In CalculationController calling the .allowableLosses action " when {

    "not supplied with a pre-existing stored value" should {

      object AllowableLossesTestDataItem extends fakeRequestTo("allowable-losses", TestCalculationController.allowableLosses)

      "return a 200" in {
        keystoreFetchCondition[AllowableLossesModel](None)
        status(AllowableLossesTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          keystoreFetchCondition[AllowableLossesModel](None)
          contentType(AllowableLossesTestDataItem.result) shouldBe Some("text/html")
          charset(AllowableLossesTestDataItem.result) shouldBe Some("utf-8")
        }

        "have a back button" in {
          keystoreFetchCondition[AllowableLossesModel](None)
          AllowableLossesTestDataItem.jsoupDoc.body.getElementById("link-back").text shouldEqual Messages("calc.base.back")
        }

        "have the title 'Are you claiming any allowable losses?'" in {
          keystoreFetchCondition[AllowableLossesModel](None)
          AllowableLossesTestDataItem.jsoupDoc.title shouldEqual Messages("calc.allowableLosses.question.one")
        }

        "have the heading 'Calculate your tax (non-residents)'" in {
          keystoreFetchCondition[AllowableLossesModel](None)
          AllowableLossesTestDataItem.jsoupDoc.body.getElementsByTag("H1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a yes no helper with hidden content and question 'Are you claiming any allowable losses?'" in {
          keystoreFetchCondition[AllowableLossesModel](None)
          AllowableLossesTestDataItem.jsoupDoc.body.getElementById("isClaimingAllowableLosses-yes").parent.text shouldBe Messages("calc.base.yes")
          AllowableLossesTestDataItem.jsoupDoc.body.getElementById("isClaimingAllowableLosses-no").parent.text shouldBe Messages("calc.base.no")
          AllowableLossesTestDataItem.jsoupDoc.body.getElementsByTag("legend").text shouldBe Messages("calc.allowableLosses.question.one")
        }

        "have a hidden monetary input with question 'Whats the total value of your allowable losses?'" in {
          keystoreFetchCondition[AllowableLossesModel](None)
          AllowableLossesTestDataItem.jsoupDoc.body.getElementById("allowableLossesAmt").tagName shouldEqual "input"
          AllowableLossesTestDataItem.jsoupDoc.select("label[for=allowableLossesAmt]").text shouldEqual Messages("calc.allowableLosses.question.two")
        }

        "have no value auto-filled into the input box" in {
          keystoreFetchCondition[AcquisitionValueModel](None)
          AllowableLossesTestDataItem.jsoupDoc.getElementById("allowableLossesAmt").attr("value") shouldBe empty
        }

        "have a hidden help text section with summary 'What are allowable losses?' and correct content" in {
          keystoreFetchCondition[AllowableLossesModel](None)
          AllowableLossesTestDataItem.jsoupDoc.select("div#allowableLossesHiddenHelp").text should
            include(Messages("calc.allowableLosses.helpText.title"))
            include(Messages("calc.allowableLosses.helpText.paragraph.one"))
            include(Messages("calc.allowableLosses.helpText.bullet.one"))
            include(Messages("calc.allowableLosses.helpText.bullet.two"))
            include(Messages("calc.allowableLosses.helpText.bullet.three"))
        }

        "has a Continue button" in {
          keystoreFetchCondition[AllowableLossesModel](None)
          AllowableLossesTestDataItem.jsoupDoc.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
        }
      }
    }
    "supplied with a pre-existing stored model" should {

      object AllowableLossesTestDataItem extends fakeRequestTo("allowable-losses", TestCalculationController.allowableLosses)
      val testModel = new AllowableLossesModel("Yes",9999.54)

      "return a 200" in {
        keystoreFetchCondition[AllowableLossesModel](Some(testModel))
        status(AllowableLossesTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          keystoreFetchCondition[AllowableLossesModel](Some(testModel))
          contentType(AllowableLossesTestDataItem.result) shouldBe Some("text/html")
          charset(AllowableLossesTestDataItem.result) shouldBe Some("utf-8")
        }

        "have the 'Yes' Radio option selected" in {
          keystoreFetchCondition[AllowableLossesModel](Some(testModel))
          AllowableLossesTestDataItem.jsoupDoc.getElementById("isClaimingAllowableLosses-yes").parent.classNames().contains("selected") shouldBe true
        }

        "have the value 9999.54 auto-filled into the input box" in {
          keystoreFetchCondition[AllowableLossesModel](Some(testModel))
          AllowableLossesTestDataItem.jsoupDoc.getElementById("allowableLossesAmt").attr("value") shouldEqual ("9999.54")
        }
      }
    }
  }

  "In CalculationController calling the .submitAllowableLosses action" when {
    def keystoreCacheCondition[T](data: AllowableLossesModel): Unit = {
      lazy val returnedCacheMap = CacheMap("form-id", Map("data" -> Json.toJson(data)))
      when(mockCalcConnector.saveFormData[T](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(returnedCacheMap))
    }
    "submitting a valid form with 'Yes' and an amount" should {
      object OtherPropertiesTestDataItem extends fakeRequestToPost(
        "allowable-losses",
        TestCalculationController.submitAllowableLosses,
        ("isClaimingAllowableLosses", "Yes"), ("allowableLossesAmt", "1000")
      )
      val testModel = new AllowableLossesModel("Yes", 1000)

      "return a 303" in {
        keystoreCacheCondition[AllowableLossesModel](testModel)
        status(OtherPropertiesTestDataItem.result) shouldBe 303
      }
    }

    "submitting an invalid form with no selection and an invalid amount" should {
      object OtherPropertiesTestDataItem extends fakeRequestToPost(
        "allowable-losses",
        TestCalculationController.submitAllowableLosses,
        ("isClaimingAllowableLosses", ""), ("allowableLossesAmt", "")
      )
      val testModel = new AllowableLossesModel("Yes", 1000)

      "return a 400" in {
        keystoreCacheCondition[AllowableLossesModel](testModel)
        status(OtherPropertiesTestDataItem.result) shouldBe 400
      }
    }
  }

  //################### Other Reliefs tests #######################
  "In CalculationController calling the .otherReliefs action " when {
    "not supplied with a pre-existing stored model" should {
      object OtherReliefsTestDataItem extends fakeRequestTo("other-reliefs", TestCalculationController.otherReliefs)

      "return a 200" in {
        keystoreFetchCondition[OtherReliefsModel](None)
        status(OtherReliefsTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          contentType(OtherReliefsTestDataItem.result) shouldBe Some("text/html")
          charset(OtherReliefsTestDataItem.result) shouldBe Some("utf-8")
        }
        "have the title 'How much extra tax relief are you claiming?'" in {
          OtherReliefsTestDataItem.jsoupDoc.title shouldEqual Messages("calc.otherReliefs.question")
        }

        "have the heading Calculate your tax (non-residents) " in {
          OtherReliefsTestDataItem.jsoupDoc.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a 'Back' link " in {
          OtherReliefsTestDataItem.jsoupDoc.body.getElementById("link-back").text shouldEqual Messages("calc.base.back")
        }

        "have the question 'How much extra tax relief are you claiming?' as the legend of the input" in {
          OtherReliefsTestDataItem.jsoupDoc.body.getElementsByTag("label").text shouldEqual Messages("calc.otherReliefs.question")
        }

        "display an input box for the Other Tax Reliefs" in {
          OtherReliefsTestDataItem.jsoupDoc.body.getElementById("otherReliefs").tagName() shouldEqual "input"
        }

        "display a 'Continue' button " in {
          OtherReliefsTestDataItem.jsoupDoc.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
        }
      }
    }
    "supplied with a pre-existing stored model" should {
      object OtherReliefsTestDataItem extends fakeRequestTo("other-reliefs", TestCalculationController.otherReliefs)
      val testOtherReliefsModel = new OtherReliefsModel(5000)

      "return a 200" in {
        keystoreFetchCondition[OtherReliefsModel](Some(testOtherReliefsModel))
        status(OtherReliefsTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          contentType(OtherReliefsTestDataItem.result) shouldBe Some("text/html")
          charset(OtherReliefsTestDataItem.result) shouldBe Some("utf-8")
        }

        "have the value 5000 auto-filled into the input box" in {
          keystoreFetchCondition[OtherReliefsModel](Some(testOtherReliefsModel))
          OtherReliefsTestDataItem.jsoupDoc.getElementById("otherReliefs").attr("value") shouldEqual "5000"
        }
      }
    }
  }

  //################### Summary tests #######################
  "In CalculationController calling the .summary action " should {

    object SummaryTestDataItem extends fakeRequestTo("summary", CalculationController.summary)

    "return a 200" in {
      status(SummaryTestDataItem.result) shouldBe 200
    }

    "return some HTML that" should {

      "contain some text and use the character set utf-8" in {
        contentType(SummaryTestDataItem.result) shouldBe Some("text/html")
        charset(SummaryTestDataItem.result) shouldBe Some("utf-8")
      }

      "should have the title 'Summary'" in {
        SummaryTestDataItem.jsoupDoc.getElementsByTag("title").text shouldEqual Messages("calc.summary.title")
      }

      "have a back button" in {
        SummaryTestDataItem.jsoupDoc.getElementById("back-link").text shouldEqual Messages("calc.base.back")
      }

      "have the correct sub-heading 'You owe'" in {
        SummaryTestDataItem.jsoupDoc.select("h1 span").text shouldEqual Messages("calc.summary.secondaryHeading")
      }

      "have a result amount currently set to £NNNN.pp" in {
        SummaryTestDataItem.jsoupDoc.select("h1 b").text shouldEqual "£NNNN.pp"
      }

      "have a 'Calculation details' section that" should {

        "include the section heading 'Calculation details" in {
          SummaryTestDataItem.jsoupDoc.select("#calcDetails").text should include(Messages("calc.summary.calculation.details.title"))
        }

        "include 'Your total gain'" in {
          SummaryTestDataItem.jsoupDoc.select("#calcDetails").text should include(Messages("calc.summary.calculation.details.totalGain"))
        }

        "include 'Your taxable gain'" in {
          SummaryTestDataItem.jsoupDoc.select("#calcDetails").text should include(Messages("calc.summary.calculation.details.taxableGain"))
        }

        "include 'Your tax rate'" in {
          SummaryTestDataItem.jsoupDoc.select("#calcDetails").text should include(Messages("calc.summary.calculation.details.taxRate"))
        }
      }

      "have a 'Personal details' section that" should {

        "include the section heading 'Personal details" in {
          SummaryTestDataItem.jsoupDoc.select("#personalDetails").text should include(Messages("calc.summary.personal.details.title"))
        }

        "include the question 'Who owned the property?'" in {
          SummaryTestDataItem.jsoupDoc.select("#personalDetails").text should include(Messages("calc.customerType.question"))
        }

        "include the question 'Are you a trustee for someone who's vulnerable'" in {
          SummaryTestDataItem.jsoupDoc.select("#personalDetails").text should include(Messages("calc.disabledTrustee.question"))
        }

        "include the question 'How much of your Capital Gains Tax allowance have you got left'" in {
          SummaryTestDataItem.jsoupDoc.select("#personalDetails").text should include(Messages("calc.annualExemptAmount.question"))
        }
      }

      "have a 'Purchase details' section that" should {

        "include the section heading 'Purchase details" in {
          SummaryTestDataItem.jsoupDoc.select("#purchaseDetails").text should include(Messages("calc.summary.purchase.details.title"))
        }

        "include the question 'How much did you pay for the property?'" in {
          SummaryTestDataItem.jsoupDoc.select("#purchaseDetails").text should include(Messages("calc.acquisitionValue.question"))
        }

        "include the question 'How much did you pay in costs when you became the property owner?'" in {
          SummaryTestDataItem.jsoupDoc.select("#purchaseDetails").text should include(Messages("calc.acquisitionCosts.question"))
        }
      }

      "have a 'Property details' section that" should {

        "include the section heading 'Property details" in {
          SummaryTestDataItem.jsoupDoc.select("#propertyDetails").text should include(Messages("calc.summary.property.details.title"))
        }

        "include the question 'How much did you pay for the property?'" in {
          SummaryTestDataItem.jsoupDoc.select("#propertyDetails").text should include(Messages("calc.improvements.question"))
        }
      }

      "have a 'Sale details' section that" should {

        "include the section heading 'Sale details" in {
          SummaryTestDataItem.jsoupDoc.select("#saleDetails").text should include(Messages("calc.summary.sale.details.title"))
        }

        "include the question 'When did you sign the contract that made someone else the owner?'" in {
          SummaryTestDataItem.jsoupDoc.select("#saleDetails").text should include(Messages("calc.disposalDate.question"))
        }

        "include the question 'How much did you sell or give away the property for?'" in {
          SummaryTestDataItem.jsoupDoc.select("#saleDetails").text should include(Messages("calc.disposalValue.question"))
        }

        "include the question 'How much did you pay in costs when you stopped being the property owner?'" in {
          SummaryTestDataItem.jsoupDoc.select("#saleDetails").text should include(Messages("calc.disposalCosts.question"))
        }
      }

      "have a 'Deductions details' section that" should {

        "include the section heading 'Deductions" in {
          SummaryTestDataItem.jsoupDoc.select("#deductions").text should include(Messages("calc.summary.deductions.title"))
        }

        "include the question 'Are you claiming Entrepreneurs' Relief?'" in {
          SummaryTestDataItem.jsoupDoc.select("#deductions").text should include(Messages("calc.entrepreneursRelief.question"))
        }

        "include the question 'Whats the total value of your allowable losses?'" in {
          SummaryTestDataItem.jsoupDoc.select("#deductions").text should include(Messages("calc.allowableLosses.question.two"))
        }
      }

      "have a 'What to do next' section that" should {

        "have the heading 'What to do next'" in {
          SummaryTestDataItem.jsoupDoc.select("#whatToDoNext H2").text shouldEqual (Messages("calc.common.next.actions.heading"))
        }

        "include the text 'You need to tell HMRC about the property'" in {
          SummaryTestDataItem.jsoupDoc.select("#whatToDoNext").text should
            include(Messages("calc.summary.next.actions.text"))
          include(Messages("calc.summary.next.actions.link"))
        }
      }

      "have a link to 'Start again'" in {
        SummaryTestDataItem.jsoupDoc.select("#startAgain").text shouldEqual Messages("calc.summary.startAgain")
      }
    }
  }



  //############## Current Income tests ######################
  "In CalculationController calling the .currentIncome action " when {
    "not supplied with a pre-existing stored model" should {
      object CurrentIncomeTestDataItem extends fakeRequestTo("currentIncome", TestCalculationController.currentIncome)

      "return a 200" in {
        keystoreFetchCondition[CurrentIncomeModel](None)
        status(CurrentIncomeTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          keystoreFetchCondition[CurrentIncomeModel](None)
          contentType(CurrentIncomeTestDataItem.result) shouldBe Some("text/html")
          charset(CurrentIncomeTestDataItem.result) shouldBe Some("utf-8")
        }

        "have the title 'In the tax year when you stopped owning the property, what was your total UK income?'" in {
          keystoreFetchCondition[CurrentIncomeModel](None)
          CurrentIncomeTestDataItem.jsoupDoc.title shouldEqual Messages("calc.currentIncome.question")
        }

        "have the heading Calculate your tax (non-residents) " in {
          keystoreFetchCondition[CurrentIncomeModel](None)
          CurrentIncomeTestDataItem.jsoupDoc.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a 'Back' link " in {
          keystoreFetchCondition[CurrentIncomeModel](None)
          CurrentIncomeTestDataItem.jsoupDoc.body.getElementById("link-back").text shouldEqual Messages("calc.base.back")
        }

        "have the question 'In the tax year when you stopped owning the property, what was your total UK income?' as the legend of the input" in {
          keystoreFetchCondition[CurrentIncomeModel](None)
          CurrentIncomeTestDataItem.jsoupDoc.body.getElementsByTag("label").text shouldEqual Messages("calc.currentIncome.question")
        }

        "display an input box for the Current Income Amount" in {
          keystoreFetchCondition[CurrentIncomeModel](None)
          CurrentIncomeTestDataItem.jsoupDoc.body.getElementById("currentIncome").tagName() shouldEqual "input"
        }

        "have no value auto-filled into the input box" in {
          keystoreFetchCondition[CurrentIncomeModel](None)
          CurrentIncomeTestDataItem.jsoupDoc.getElementById("currentIncome").attr("value") shouldBe empty
        }

        "display a 'Continue' button " in {
          keystoreFetchCondition[CurrentIncomeModel](None)
          CurrentIncomeTestDataItem.jsoupDoc.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
        }

        "should contain a Read more sidebar with a link to CGT allowances" in {
          keystoreFetchCondition[CurrentIncomeModel](None)
          CurrentIncomeTestDataItem.jsoupDoc.select("aside h2").text shouldBe Messages("calc.common.readMore")
          CurrentIncomeTestDataItem.jsoupDoc.select("aside a").first.text shouldBe Messages("calc.currentIncome.link.one")
          CurrentIncomeTestDataItem.jsoupDoc.select("aside a").last.text shouldBe Messages("calc.currentIncome.link.two")
        }
      }
    }
  }





}
