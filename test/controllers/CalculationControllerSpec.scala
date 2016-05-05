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
import common.TestModels
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

//    override def createSummary(implicit hc: HeaderCarrier) = {
//      new SummaryModel(
//        CustomerTypeModel("individual"),
//        None,
//        Some(CurrentIncomeModel(1000)),
//        Some(PersonalAllowanceModel(11100)),
//        OtherPropertiesModel("No"),
//        None,
//        AcquisitionValueModel(100000),
//        ImprovementsModel("No", None),
//        DisposalDateModel(10, 10, 2010),
//        DisposalValueModel(150000),
//        AcquisitionCostsModel(None),
//        DisposalCostsModel(None),
//        EntrepreneursReliefModel("No"),
//        AllowableLossesModel("No", None),
//        OtherReliefsModel(None)
//      )
//    }
  }

  implicit val hc = new HeaderCarrier()

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

  def keystoreSummaryValue(data: SummaryModel): Unit = {
    when(mockCalcConnector.createSummary(Matchers.any()))
      .thenReturn(data)
  }

  def keystoreFlatCalculateValue(data: Option[CalculationResultModel]): Unit = {
    when(mockCalcConnector.calculateFlat(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(data))
  }

  def keystoreTACalculateValue(data: Option[CalculationResultModel]): Unit = {
    when(mockCalcConnector.calculateTA(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(data))
  }

  def keystoreFetchValue[T](data: Option[T]): Unit = {
    when(mockCalcConnector.fetchAndGetValue[T](Matchers.anyString())(Matchers.any(), Matchers.any()))
      .thenReturn(data)
  }

    val sumModelFlat = SummaryModel(
      CustomerTypeModel("individual"),
      None,
      Some(CurrentIncomeModel(1000)),
      Some(PersonalAllowanceModel(11100)),
      OtherPropertiesModel("No"),
      None,
      AcquisitionDateModel("No", None, None, None),
      AcquisitionValueModel(100000),
      ImprovementsModel("No", None),
      DisposalDateModel(10, 10, 2010),
      DisposalValueModel(150000),
      AcquisitionCostsModel(None),
      DisposalCostsModel(None),
      EntrepreneursReliefModel("No"),
      AllowableLossesModel("No", None),
      CalculationElectionModel("flat"),
      OtherReliefsModel(None),
      OtherReliefsModel(None)
    )

  val sumModelTA = SummaryModel(
    CustomerTypeModel("individual"),
    None,
    Some(CurrentIncomeModel(1000)),
    Some(PersonalAllowanceModel(11100)),
    OtherPropertiesModel("Yes"),
    Some(AnnualExemptAmountModel(9000)),
    AcquisitionDateModel("Yes", Some(9), Some(9), Some(9)),
    AcquisitionValueModel(100000),
    ImprovementsModel("Yes", Some(500)),
    DisposalDateModel(10, 10, 2010),
    DisposalValueModel(150000),
    AcquisitionCostsModel(Some(650)),
    DisposalCostsModel(Some(850)),
    EntrepreneursReliefModel("No"),
    AllowableLossesModel("No", None),
    CalculationElectionModel("time"),
    OtherReliefsModel(Some(2000)),
    OtherReliefsModel(Some(1000))
  )

  val calcModelTwoRates = CalculationResultModel(8000, 40000, 32000, 18, Some(8000), Some(28))
  val calcModelOneRate = CalculationResultModel(8000, 40000, 32000, 18, None, None)

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
          CustomerTypeTestDataItem.jsoupDoc.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
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
          DisabledTrusteeTestDataItem.jsoupDoc.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
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
          PersonalAllowanceTestDataItem.jsoupDoc.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
        }

        "have the question 'In the tax year when you stopped owning the property, what was your UK Personal Allowance?' as the label of the input" in {
          keystoreFetchCondition[PersonalAllowanceModel](None)
          PersonalAllowanceTestDataItem.jsoupDoc.body.getElementsByTag("label").text should include (Messages("calc.personalAllowance.question"))
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

  "In CalculationController calling the .submitPersonalAllowance action" when {
    def keystoreCacheCondition[T](data: PersonalAllowanceModel): Unit = {
      lazy val returnedCacheMap = CacheMap("form-id", Map("data" -> Json.toJson(data)))
      when(mockCalcConnector.saveFormData[T](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(returnedCacheMap))
    }

    "submitting a valid form" should {
      object PersonalAllowanceTestDataItem extends fakeRequestToPost(
        "personal-allowance",
        TestCalculationController.submitPersonalAllowance,
        ("personalAllowance", "1000")
      )
      val testModel = new PersonalAllowanceModel(1000)

      "return a 303" in {
        keystoreCacheCondition[PersonalAllowanceModel](testModel)
        status(PersonalAllowanceTestDataItem.result) shouldBe 303
      }

      s"redirect to ${routes.CalculationController.otherProperties()}" in {
        keystoreCacheCondition[PersonalAllowanceModel](testModel)
        redirectLocation(PersonalAllowanceTestDataItem.result) shouldBe Some(s"${routes.CalculationController.otherProperties()}")
      }
    }

    "submitting an invalid form with no value" should {
      object PersonalAllowanceTestDataItem extends fakeRequestToPost(
        "personal-allowance",
        TestCalculationController.submitPersonalAllowance,
        ("personalAllowance", "")
      )
      val testModel = new PersonalAllowanceModel(0)

      "return a 400" in {
        keystoreCacheCondition[PersonalAllowanceModel](testModel)
        status(PersonalAllowanceTestDataItem.result) shouldBe 400
      }
    }

    "submitting an invalid form with a negative value of -342" should {
      object PersonalAllowanceTestDataItem extends fakeRequestToPost(
        "personal-allowance",
        TestCalculationController.submitPersonalAllowance,
        ("personalAllowance", "-342")
      )
      val testModel = new PersonalAllowanceModel(-342)

      "return a 400" in {
        keystoreCacheCondition[PersonalAllowanceModel](testModel)
        status(PersonalAllowanceTestDataItem.result) shouldBe 400
      }
    }

    "submitting an invalid form with value 1.111" should {
      object PersonalAllowanceTestDataItem extends fakeRequestToPost(
        "personal-allowance",
        TestCalculationController.submitPersonalAllowance,
        ("personalAllowance", "1.111")
      )
      val testModel = new PersonalAllowanceModel(1.111)

      "return a 400" in {
        keystoreCacheCondition[PersonalAllowanceModel](testModel)
        status(PersonalAllowanceTestDataItem.result) shouldBe 400
      }

      s"fail with message ${Messages("calc.personalAllowance.errorDecimalPlaces")}" in {
        keystoreCacheCondition(testModel)
        PersonalAllowanceTestDataItem.jsoupDoc.getElementsByClass("error-notification").text should include (Messages("calc.personalAllowance.errorDecimalPlaces"))
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
          OtherPropertiesTestDataItem.jsoupDoc.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
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
        "other-properties",
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
        "other-properties",
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
        "other-properties",
        TestCalculationController.submitOtherProperties,
        ("otherProperties", "")
      )
      val testModel = new OtherPropertiesModel("")

      "return a 400" in {
        keystoreCacheCondition[OtherPropertiesModel](testModel)
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
          AnnualExemptAmountTestDataItem.jsoupDoc.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
        }

        "have the question 'How much of your Capital Gains Tax allowance have you got left?' as the legend of the input" in {
          keystoreFetchCondition[AnnualExemptAmountModel](None)
          AnnualExemptAmountTestDataItem.jsoupDoc.body.getElementsByTag("label").text should include (Messages("calc.annualExemptAmount.question"))
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

    "submitting an invalid form with value 1.111" should {
      object AnnualExemptAmountTestDataItem extends fakeRequestToPost(
        "allowance",
        TestCalculationController.submitAnnualExemptAmount,
        ("annualExemptAmount", "1.111")
      )
      val testModel = new AnnualExemptAmountModel(-1000)

      "return a 400" in {
        keystoreCacheCondition[AnnualExemptAmountModel](testModel)
        status(AnnualExemptAmountTestDataItem.result) shouldBe 400
      }

      s"fail with message ${Messages("calc.annualExemptAmount.errorDecimalPlaces")}" in {
        keystoreCacheCondition(testModel)
        AnnualExemptAmountTestDataItem.jsoupDoc.getElementsByClass("error-notification").text should include (Messages("calc.annualExemptAmount.errorDecimalPlaces"))
      }
    }
  }

  //############## Acquisition Date tests ######################
  "In CalculationController calling the .acquisitionDate action " should {

    "not supplied with a pre-existing model" should {
      object AcquisitionDateTestDataItem extends fakeRequestTo("acquisition-date", TestCalculationController.acquisitionDate)

      "return a 200" in {
        keystoreFetchCondition[AcquisitionDateModel](None)
        status(AcquisitionDateTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          contentType(AcquisitionDateTestDataItem.result) shouldBe Some("text/html")
          charset(AcquisitionDateTestDataItem.result) shouldBe Some("utf-8")
        }

        s"have the title '${Messages("calc.acquisitionDate.question")}'" in {
          AcquisitionDateTestDataItem.jsoupDoc.title shouldEqual Messages("calc.acquisitionDate.question")
        }

        "have the heading Calculate your tax (non-residents) " in {
          AcquisitionDateTestDataItem.jsoupDoc.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a 'Back' link " in {
          AcquisitionDateTestDataItem.jsoupDoc.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
        }

        s"have the question '${Messages("calc.acquisitionDate.question")}" in {
          AcquisitionDateTestDataItem.jsoupDoc.body.getElementsByTag("legend").text should include(Messages("calc.acquisitionDate.question"))
        }

        "display the correct wording for radio option `yes`" in {
          AcquisitionDateTestDataItem.jsoupDoc.body.getElementById("hasAcquisitionDate-yes").parent.text shouldEqual Messages("calc.base.yes")
        }

        "display the correct wording for radio option `no`" in {
          AcquisitionDateTestDataItem.jsoupDoc.body.getElementById("hasAcquisitionDate-no").parent.text shouldEqual Messages("calc.base.no")
        }

        "contain a hidden component with an input box" in {
          AcquisitionDateTestDataItem.jsoupDoc.body.getElementById("hidden").html should include("input")
        }

        "display three input boxes with labels Day, Month and Year respectively" in {
          AcquisitionDateTestDataItem.jsoupDoc.select("label[for=acquisitionDate.day]").text shouldEqual Messages("calc.common.date.fields.day")
          AcquisitionDateTestDataItem.jsoupDoc.select("label[for=acquisitionDate.month]").text shouldEqual Messages("calc.common.date.fields.month")
          AcquisitionDateTestDataItem.jsoupDoc.select("label[for=acquisitionDate.year]").text shouldEqual Messages("calc.common.date.fields.year")
        }

        "display a 'Continue' button " in {
          AcquisitionDateTestDataItem.jsoupDoc.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
        }
      }
    }

    "supplied with a model already filled with data" should {
      object AcquisitionDateTestDataItem extends fakeRequestTo("acquisition-date", TestCalculationController.acquisitionDate)

      "return a 200" in {
        val testAcquisitionDateModel = new AcquisitionDateModel("Yes", Some(10), Some(12), Some(2016))
        keystoreFetchCondition[AcquisitionDateModel](Some(testAcquisitionDateModel))
        status(AcquisitionDateTestDataItem.result) shouldBe 200
      }

    "return some HTML that" should {
      val testAcquisitionDateModel = new AcquisitionDateModel("Yes", Some(10), Some(12), Some(2016))
      keystoreFetchCondition[AcquisitionDateModel](Some(testAcquisitionDateModel))

        "have the radio option `Yes` selected if `Yes` is supplied in the model" in {
          AcquisitionDateTestDataItem.jsoupDoc.body.getElementById("hasAcquisitionDate-yes").parent.classNames().contains("selected") shouldBe true
        }

        "have the date 10, 12, 2016 pre-populated" in {
          AcquisitionDateTestDataItem.jsoupDoc.body.getElementById("acquisitionDate.day").attr("value") shouldEqual testAcquisitionDateModel.day.get.toString
          AcquisitionDateTestDataItem.jsoupDoc.body.getElementById("acquisitionDate.month").attr("value") shouldEqual testAcquisitionDateModel.month.get.toString
          AcquisitionDateTestDataItem.jsoupDoc.body.getElementById("acquisitionDate.year").attr("value") shouldEqual testAcquisitionDateModel.year.get.toString
        }
      }

      "have the radio option `No` selected if `No` is supplied in the model" in {
        object AcquisitionDateTestDataItem extends fakeRequestTo("acquisition-date", TestCalculationController.acquisitionDate)
        val testAcquisitionDateModel = new AcquisitionDateModel("No", None, None, None)
        keystoreFetchCondition[AcquisitionDateModel](Some(testAcquisitionDateModel))
        AcquisitionDateTestDataItem.jsoupDoc.body.getElementById("hasAcquisitionDate-no").parent.classNames().contains("selected") shouldBe true
      }
    }
  }

  "In CalculationController calling the submitAcquisitionDate action" when {
    def keystoreCacheCondition[T](data: AcquisitionDateModel): Unit = {
      lazy val returnedCacheMap = CacheMap("form-id", Map("data" -> Json.toJson(data)))
      when(mockCalcConnector.saveFormData[T](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(returnedCacheMap))
    }

    "submitting a valid form" should {
      val acquisitionDateTestModel = new AcquisitionDateModel("Yes", Some(10), Some(2), Some(2015))
      object AcquisitionDateTestDataItem extends fakeRequestToPost(
        "acquisition-date",
        TestCalculationController.submitAcquisitionDate,
        ("hasAcquisitionDate", "Yes"),
        ("acquisitionDate.day", "10"),
        ("acquisitionDate.month", "2"),
        ("acquisitionDate.year", "2015")
      )

      "return a 303" in {
        keystoreCacheCondition[AcquisitionDateModel](acquisitionDateTestModel)
        status(AcquisitionDateTestDataItem.result) shouldBe 303
      }
    }

    "submitting a valid form with 'No' and no date value" should {
      val acquisitionDateTestModel = new AcquisitionDateModel("No", None, None, None)
      object AcquisitionDateTestDataItem extends fakeRequestToPost(
        "acquisitionDate",
        TestCalculationController.submitAcquisitionDate,
        ("hasAcquisitionDate", "No"),
        ("acquisitionDate.day", ""),
        ("acquisitionDate.month", ""),
        ("acquisitionDate.year", "")
      )

      "return a 303" in {
        keystoreCacheCondition[AcquisitionDateModel](acquisitionDateTestModel)
        status(AcquisitionDateTestDataItem.result) shouldBe 303
      }
    }

    "submitting a valid leap year date 29/02/2016" should {
      val acquisitionDateTestModel = new AcquisitionDateModel("Yes", Some(29), Some(2), Some(2016))
      object AcquisitionDateTestDataItem extends fakeRequestToPost(
        "acquisition-date",
        TestCalculationController.submitAcquisitionDate,
        ("hasAcquisitionDate", "Yes"),
        ("acquisitionDate.day", "29"),
        ("acquisitionDate.month","2"),
        ("acquisitionDate.year","2016")
      )

      "return a 303" in {
        keystoreCacheCondition[AcquisitionDateModel](acquisitionDateTestModel)
        status(AcquisitionDateTestDataItem.result) shouldBe 303
      }

      s"redirect to ${routes.CalculationController.acquisitionValue()}" in {
        keystoreCacheCondition[AcquisitionDateModel](acquisitionDateTestModel)
        redirectLocation(AcquisitionDateTestDataItem.result) shouldBe Some(s"${routes.CalculationController.acquisitionValue()}")
      }
    }

    "submitting an invalid leap year date 29/02/2017" should {
      val acquisitionDateTestModel = new AcquisitionDateModel("Yes", Some(29), Some(2), Some(2017))
      object AcquisitionDateTestDataItem extends fakeRequestToPost(
        "acquisition-date",
        TestCalculationController.submitAcquisitionDate,
        ("hasAcquisitionDate", "Yes"),
        ("acquisitionDate.day", "29"),
        ("acquisitionDate.month","2"),
        ("acquisitionDate.year","2017")
      )

      "return a 400" in {
        keystoreCacheCondition[AcquisitionDateModel](acquisitionDateTestModel)
        status(AcquisitionDateTestDataItem.result) shouldBe 400
      }

      s"should error with message ${Messages("calc.common.date.error.invalidDate")}" in {
        keystoreCacheCondition[AcquisitionDateModel](acquisitionDateTestModel)
        AcquisitionDateTestDataItem.jsoupDoc.select(".error-notification").text should include (Messages("calc.common.date.error.invalidDate"))
      }
    }

    "submitting a day less than 1" should {
      val acquisitionDateTestModel = new AcquisitionDateModel("Yes", Some(0), Some(2), Some(2017))
      object AcquisitionDateTestDataItem extends fakeRequestToPost(
        "acquisition-date",
        TestCalculationController.submitAcquisitionDate,
        ("hasAcquisitionDate", "Yes"),
        ("acquisitionDate.day", "0"),
        ("acquisitionDate.month","2"),
        ("acquisitionDate.year","2017")
      )

      "return a 400" in {
        keystoreCacheCondition[AcquisitionDateModel](acquisitionDateTestModel)
        status(AcquisitionDateTestDataItem.result) shouldBe 400
      }

      s"should error with message ${Messages("calc.common.date.error.lessThan1")}" in {
        keystoreCacheCondition[AcquisitionDateModel](acquisitionDateTestModel)
        AcquisitionDateTestDataItem.jsoupDoc.select(".error-notification").text should include (Messages("calc.common.date.error.invalidDate"))
      }
    }

    "submitting a day greater than 31" should {
      val acquisitionDateTestModel = new AcquisitionDateModel("Yes", Some(32), Some(2), Some(2017))
      object AcquisitionDateTestDataItem extends fakeRequestToPost(
        "acquisition-date",
        TestCalculationController.submitAcquisitionDate,
        ("hasAcquisitionDate", "Yes"),
        ("acquisitionDate.day", "32"),
        ("acquisitionDate.month","2"),
        ("acquisitionDate.year","2017")
      )

      "return a 400" in {
        keystoreCacheCondition[AcquisitionDateModel](acquisitionDateTestModel)
        status(AcquisitionDateTestDataItem.result) shouldBe 400
      }

      s"should error with message ${Messages("calc.common.date.error.greaterThan31")}" in {
        keystoreCacheCondition[AcquisitionDateModel](acquisitionDateTestModel)
        AcquisitionDateTestDataItem.jsoupDoc.select(".error-notification").text should include (Messages("calc.common.date.error.invalidDate"))
      }
    }

    "submitting a month greater than 12" should {
      val acquisitionDateTestModel = new AcquisitionDateModel("Yes", Some(31), Some(13), Some(2017))
      object AcquisitionDateTestDataItem extends fakeRequestToPost(
        "acquisition-date",
        TestCalculationController.submitAcquisitionDate,
        ("hasAcquisitionDate", "Yes"),
        ("acquisitionDate.day", "31"),
        ("acquisitionDate.month","13"),
        ("acquisitionDate.year","2017")
      )

      "return a 400" in {
        keystoreCacheCondition[AcquisitionDateModel](acquisitionDateTestModel)
        status(AcquisitionDateTestDataItem.result) shouldBe 400
      }

      s"should error with message ${Messages("calc.common.date.error.greaterThan12")}" in {
        keystoreCacheCondition[AcquisitionDateModel](acquisitionDateTestModel)
        AcquisitionDateTestDataItem.jsoupDoc.select(".error-notification").text should include (Messages("calc.common.date.error.invalidDate"))
      }
    }

    "submitting a month less than 1" should {
      val acquisitionDateTestModel = new AcquisitionDateModel("Yes", Some(31), Some(0), Some(2017))
      object AcquisitionDateTestDataItem extends fakeRequestToPost(
        "acquisition-date",
        TestCalculationController.submitAcquisitionDate,
        ("hasAcquisitionDate", "Yes"),
        ("acquisitionDate.day", "31"),
        ("acquisitionDate.month","0"),
        ("acquisitionDate.year","2017")
      )

      "return a 400" in {
        keystoreCacheCondition[AcquisitionDateModel](acquisitionDateTestModel)
        status(AcquisitionDateTestDataItem.result) shouldBe 400
      }

      s"should error with message ${Messages("calc.common.date.error.lessThan1")}" in {
        keystoreCacheCondition[AcquisitionDateModel](acquisitionDateTestModel)
        AcquisitionDateTestDataItem.jsoupDoc.select(".error-notification").text should include (Messages("calc.common.date.error.invalidDate"))
      }
    }

    "submitting a day with no value" should {
      val acquisitionDateTestModel = new AcquisitionDateModel("Yes", Some(0), Some(12), Some(2017))
      object AcquisitionDateTestDataItem extends fakeRequestToPost(
        "acquisition-date",
        TestCalculationController.submitAcquisitionDate,
        ("hasAcquisitionDate", "Yes"),
        ("acquisitionDate.day", ""),
        ("acquisitionDate.month","12"),
        ("acquisitionDate.year","2017")
      )

      "return a 400" in {
        keystoreCacheCondition[AcquisitionDateModel](acquisitionDateTestModel)
        status(AcquisitionDateTestDataItem.result) shouldBe 400
      }

      "should error with message 'You must supply a valid date'" in {
        keystoreCacheCondition[AcquisitionDateModel](acquisitionDateTestModel)
        AcquisitionDateTestDataItem.jsoupDoc.select(".error-notification").text should include (Messages("You must supply a valid date"))
      }
    }

    "submitting a month with no value" should {
      val acquisitionDateTestModel = new AcquisitionDateModel("Yes", Some(31), Some(0), Some(2017))
      object AcquisitionDateTestDataItem extends fakeRequestToPost(
        "acquisition-date",
        TestCalculationController.submitAcquisitionDate,
        ("hasAcquisitionDate", "Yes"),
        ("acquisitionDate.day", "31"),
        ("acquisitionDate.month",""),
        ("acquisitionDate.year","2017")
      )

      "return a 400" in {
        keystoreCacheCondition[AcquisitionDateModel](acquisitionDateTestModel)
        status(AcquisitionDateTestDataItem.result) shouldBe 400
      }

      "should error with message 'You must supply a valid date'" in {
        keystoreCacheCondition[AcquisitionDateModel](acquisitionDateTestModel)
        AcquisitionDateTestDataItem.jsoupDoc.select(".error-notification").text should include (Messages("You must supply a valid date"))
      }
    }

    "submitting a year with no value" should {
      val acquisitionDateTestModel = new AcquisitionDateModel("Yes", Some(31), Some(12), Some(0))
      object AcquisitionDateTestDataItem extends fakeRequestToPost(
        "acquisition-date",
        TestCalculationController.submitAcquisitionDate,
        ("hasAcquisitionDate", "Yes"),
        ("acquisitionDate.day", "31"),
        ("acquisitionDate.month","12"),
        ("acquisitionDate.year","")
      )

      "return a 400" in {
        keystoreCacheCondition[AcquisitionDateModel](acquisitionDateTestModel)
        status(AcquisitionDateTestDataItem.result) shouldBe 400
      }

      "should error with message 'You must supply a valid date'" in {
        keystoreCacheCondition[AcquisitionDateModel](acquisitionDateTestModel)
        AcquisitionDateTestDataItem.jsoupDoc.select(".error-notification").text should include (Messages("You must supply a valid date"))
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
          AcquisitionValueTestDataItem.jsoupDoc.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
        }

        "have the question 'How much did you pay for the property?'" in {
          keystoreFetchCondition[AcquisitionValueModel](None)
          AcquisitionValueTestDataItem.jsoupDoc.body.getElementsByTag("label").text should include (Messages("calc.acquisitionValue.question"))
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

    "submitting an invalid form with value 1.111" should {
      val testModel = new AcquisitionValueModel(1.111)
      object AcquisitionValueTestDataItem extends fakeRequestToPost (
        "acquisition-value",
        TestCalculationController.submitAcquisitionValue,
        ("acquisitionValue", "1.111")
      )

      "return a 400" in {
        keystoreCacheCondition(testModel)
        status(AcquisitionValueTestDataItem.result) shouldBe 400
      }

      s"fail with message ${Messages("calc.acquisitionValue.errorDecimalPlaces")}" in {
        keystoreCacheCondition(testModel)
        AcquisitionValueTestDataItem.jsoupDoc.getElementsByClass("error-notification").text should include (Messages("calc.acquisitionValue.errorDecimalPlaces"))
      }
    }
  }

  //################### Rebased Value Tests #######################
  "In CalculationController calling the .rebasedValue action " should {
    object RebasedValueDataItem extends fakeRequestTo("rebased-value", TestCalculationController.rebasedValue)

    "return a 200" in {
      keystoreFetchCondition[RebasedValueModel](None)
      status(RebasedValueDataItem.result) shouldBe 200
    }

    "return some HTML that" should {

      "contain some text and use the character set utf-8" in {
        keystoreFetchCondition[RebasedValueModel](None)
        contentType(RebasedValueDataItem.result) shouldBe Some("text/html")
        charset(RebasedValueDataItem.result) shouldBe Some("utf-8")
      }

      "Have the title 'Calculate your Capital Gains Tax" in {
        keystoreFetchCondition[RebasedValueModel](None)
        RebasedValueDataItem.jsoupDoc.getElementsByTag("h1").text shouldBe "Calculate your Capital Gains Tax"
      }

      s"Have the question ${Messages("calc.rebasedValue.question")}" in {
        keystoreFetchCondition[RebasedValueModel](None)
        RebasedValueDataItem.jsoupDoc.getElementsByTag("legend").text should include(Messages("calc.rebasedValue.question"))
      }

      "display the correct wording for radio option `yes`" in {
        keystoreFetchCondition[RebasedValueModel](None)
        RebasedValueDataItem.jsoupDoc.body.getElementById("hasRebasedValue-yes").parent.text shouldEqual Messages("calc.base.yes")
      }

      "display the correct wording for radio option `no`" in {
        keystoreFetchCondition[RebasedValueModel](None)
        RebasedValueDataItem.jsoupDoc.body.getElementById("hasRebasedValue-no").parent.text shouldEqual Messages("calc.base.no")
      }

      "contain a hidden component with an input box" in {
        keystoreFetchCondition[RebasedValueModel](None)
        RebasedValueDataItem.jsoupDoc.body.getElementById("hidden").html should include("input")
      }

      s"contain a hidden component with the question ${Messages("calc.rebasedValue.questionTwo")}" in {
        keystoreFetchCondition[RebasedValueModel](None)
        RebasedValueDataItem.jsoupDoc.getElementById("rebasedValueAmt").parent.text should include(Messages("calc.rebasedValue.questionTwo"))
      }

      "Have a back link" in {
        keystoreFetchCondition[RebasedValueModel](None)
        RebasedValueDataItem.jsoupDoc.getElementById("back-link").tagName() shouldBe "a"
      }

      "Have a continue button" in {
        keystoreFetchCondition[RebasedValueModel](None)
        RebasedValueDataItem.jsoupDoc.getElementById("continue-button").tagName() shouldBe "button"
      }
    }

    "supplied with a pre-existing model with 'Yes' checked and value already entered" should {
      val testRebasedValueModelYes = new RebasedValueModel("Yes", Some(10000))

      "return a 200" in {
        object RebasedValueTestDataItem extends fakeRequestTo("rebased-value", TestCalculationController.rebasedValue)
        keystoreFetchCondition[RebasedValueModel](Some(testRebasedValueModelYes))
        status(RebasedValueTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "be pre populated with Yes box selected and a value of 10000 entered" in {
          object RebasedValueTestDataItem extends fakeRequestTo("rebased-value", TestCalculationController.rebasedValue)
          keystoreFetchCondition[RebasedValueModel](Some(testRebasedValueModelYes))

          RebasedValueTestDataItem.jsoupDoc.getElementById("hasRebasedValue-yes").attr("checked") shouldEqual "checked"
          RebasedValueTestDataItem.jsoupDoc.getElementById("rebasedValueAmt").attr("value") shouldEqual "10000"
        }
      }
    }

    "supplied with a pre-existing model with 'No' checked and value not entered" should {
      val testRebasedValueModelNo = new RebasedValueModel("No", Some(0))

      "return a 200" in {
        object RebasedValueTestDataItem extends fakeRequestTo("rebased-value", TestCalculationController.rebasedValue)
        keystoreFetchCondition[RebasedValueModel](Some(testRebasedValueModelNo))
        status(RebasedValueTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "be pre populated with No box selected and a value of 0" in {
          object RebasedValueTestDataItem extends fakeRequestTo("rebased-value", TestCalculationController.rebasedValue)
          keystoreFetchCondition[RebasedValueModel](Some(testRebasedValueModelNo))

          RebasedValueTestDataItem.jsoupDoc.getElementById("hasRebasedValue-no").attr("checked") shouldEqual "checked"
          RebasedValueTestDataItem.jsoupDoc.getElementById("rebasedValueAmt").attr("value") shouldEqual "0"
        }
      }
    }

    "In CalculationController calling the .submitRebasedValue action " when {
      def keystoreCacheCondition[T](data: RebasedValueModel): Unit = {
        lazy val returnedCacheMap = CacheMap("form-id", Map("data" -> Json.toJson(data)))
        when(mockCalcConnector.saveFormData[T](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(returnedCacheMap))
      }

      "submitting a valid form with 'Yes' and a value of 12045" should {
        object RebasedValueTestDataItem extends fakeRequestToPost("rebasedCost",
          TestCalculationController.submitRebasedValue,
          ("hasRebasedValue", "Yes"),
          ("rebasedValueAmt", "12045"))
        val rebasedValueTestModel = new RebasedValueModel("Yes", Some(12045))

        "return a 303" in {
          keystoreCacheCondition[RebasedValueModel](rebasedValueTestModel)
          status(RebasedValueTestDataItem.result) shouldBe 303
        }
      }

      "submitting a valid form with 'No' and no value" should {
        object RebasedValueTestDataItem extends fakeRequestToPost("improvements",
          TestCalculationController.submitRebasedValue,
          ("hasRebasedValue", "No"),
          ("rebasedValueAmt", ""))
        val rebasedValueTestModel = new RebasedValueModel("No", None)

        "return a 303" in {
          keystoreCacheCondition[RebasedValueModel](rebasedValueTestModel)
          status(RebasedValueTestDataItem.result) shouldBe 303
        }
      }
    }
  }

  //################### Rebased Costs Tests #######################
  "In CalculationController calling the .rebasedCosts action " should {
    object RebasedCostsDataItem extends fakeRequestTo("rebased-costs", TestCalculationController.rebasedCosts)

    "return a 200" in {
      status(RebasedCostsDataItem.result) shouldBe 200
    }

    "return some HTML that" should {

      "contain some text and use the character set utf-8" in{
        contentType(RebasedCostsDataItem.result) shouldBe Some("text/html")
        charset(RebasedCostsDataItem.result) shouldBe Some("utf-8")
      }

      "Have the title 'Calculate your Capital Gains Tax" in {
        RebasedCostsDataItem.jsoupDoc.getElementsByTag("h1").text shouldBe "Calculate your Capital Gains Tax"
      }

      "Have the question 'Did you pay for the valuation?" in {
        RebasedCostsDataItem.jsoupDoc.getElementsByTag("legend").text shouldBe "Did you pay for the valuation?"
      }

      "display the correct wording for radio option `yes`" in {
        RebasedCostsDataItem.jsoupDoc.body.getElementById("rebasedCostsYes").parent.text shouldEqual Messages("calc.base.yes")
      }

      "display the correct wording for radio option `no`" in {
        RebasedCostsDataItem.jsoupDoc.body.getElementById("rebasedCostsNo").parent.text shouldEqual Messages("calc.base.no")
      }

      "contain a hidden component with an input box" in {
        RebasedCostsDataItem.jsoupDoc.body.getElementById("hidden").html should include ("input")
      }

      "Have a back link" in {
        RebasedCostsDataItem.jsoupDoc.getElementById("back-link").tagName() shouldBe "a"
      }

      "Have a continue button" in {
        RebasedCostsDataItem.jsoupDoc.getElementById("continue-button").tagName() shouldBe "button"
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
          ImprovementsTestDataItem.jsoupDoc.body.getElementById("hidden").html should include ("input")
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
      object ImprovementsTestDataItem extends fakeRequestToPost("improvements", TestCalculationController.submitImprovements, ("isClaimingImprovements", "Yes"), ("improvementsAmt", "12045"))
      val improvementsTestModel = new ImprovementsModel("Yes", Some(12045))

      "return a 303" in {
        keystoreCacheCondition[ImprovementsModel](improvementsTestModel)
        status(ImprovementsTestDataItem.result) shouldBe 303
      }
    }

    "submitting a valid form with 'No' and no value" should {
      object ImprovementsTestDataItem extends fakeRequestToPost("improvements", TestCalculationController.submitImprovements, ("isClaimingImprovements", "No"), ("improvementsAmt", ""))
      //This model actually has no bearing on the tes but the cachemap it produces is required.
      val improvementsTestModel = new ImprovementsModel("No", None)

      "return a 303" in {
        keystoreCacheCondition[ImprovementsModel](improvementsTestModel)
        status(ImprovementsTestDataItem.result) shouldBe 303
      }
    }

    "submitting an invalid form with 'Yes' and a value of 'fhu39awd8'" should {
      object ImprovementsTestDataItem extends fakeRequestToPost("improvements", TestCalculationController.submitImprovements, ("isClaimingImprovements", "Yes"), ("improvementsAmt", "fhu39awd8"))
      //This model actually has no bearing on the tes but the cachemap it produces is required.
      val improvementsTestModel = new ImprovementsModel("Yes", Some(9878))

      "return a 400" in {
        keystoreCacheCondition[ImprovementsModel](improvementsTestModel)
        status(ImprovementsTestDataItem.result) shouldBe 400
      }

      "return HTML that displays the error message " in {
        ImprovementsTestDataItem.jsoupDoc.select("div#hidden span.error-notification").text shouldEqual "Real number value expected"
      }
    }

    "submitting an invalid form with 'Yes' and a negative value of -100'" should {
      object ImprovementsTestDataItem extends fakeRequestToPost("improvements", TestCalculationController.submitImprovements, ("isClaimingImprovements", "Yes"), ("improvementsAmt", "-100"))
      //This model actually has no bearing on the tes but the cachemap it produces is required.
      val improvementsTestModel = new ImprovementsModel("Yes", Some(-100))

      "return a 400" in {
        keystoreCacheCondition[ImprovementsModel](improvementsTestModel)
        status(ImprovementsTestDataItem.result) shouldBe 400
      }

      "return HTML that displays the error message " in {
        ImprovementsTestDataItem.jsoupDoc.select("div#hidden span.error-notification").text shouldEqual Messages("calc.improvements.errorNegative")
      }
    }


    "submitting an invalid form with 'Yes' and an empty value'" should {
      object ImprovementsTestDataItem extends fakeRequestToPost("improvements", TestCalculationController.submitImprovements, ("isClaimingImprovements", "Yes"), ("improvementsAmt", ""))
      //This model actually has no bearing on the tes but the cachemap it produces is required.
      val improvementsTestModel = new ImprovementsModel("Yes", Some(-100))

      "return a 400" in {
        keystoreCacheCondition[ImprovementsModel](improvementsTestModel)
        status(ImprovementsTestDataItem.result) shouldBe 400
      }

      "return HTML that displays the error message " in {
        ImprovementsTestDataItem.jsoupDoc.select("div#hidden span.error-notification").text shouldEqual Messages("calc.improvements.error.no.value.supplied")
      }
    }

    "submitting an invalid form with 'Yes' and value 1.111'" should {
      object ImprovementsTestDataItem extends fakeRequestToPost("improvements", TestCalculationController.submitImprovements,
        ("isClaimingImprovements", "Yes"),
        ("improvementsAmt", "1.111"))
      //This model actually has no bearing on the tes but the cachemap it produces is required.
      val improvementsTestModel = new ImprovementsModel("Yes", Some(1.111))

      "return a 400" in {
        keystoreCacheCondition[ImprovementsModel](improvementsTestModel)
        status(ImprovementsTestDataItem.result) shouldBe 400
      }

      "return HTML that displays the error message " in {
        ImprovementsTestDataItem.jsoupDoc.select("div#hidden span.error-notification").text shouldEqual Messages("calc.improvements.errorDecimalPlaces")
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
          DisposalDateTestDataItem.jsoupDoc.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
        }

        s"have the question '${Messages("calc.disposalDate.question")}'" in {
          DisposalDateTestDataItem.jsoupDoc.body.getElementsByTag("fieldset").text should include (Messages("calc.disposalDate.question"))
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

  "In CalculationController calling the .submitDisposalDate action" when {
    def keystoreCacheCondition[T](data: DisposalDateModel): Unit = {
      lazy val returnedCacheMap = CacheMap("form-id", Map("data" -> Json.toJson(data)))
      when(mockCalcConnector.saveFormData[T](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(returnedCacheMap))
    }
    "submitting a valid date 31/01/2016" should {
      object DisposalDateTestDataItem extends fakeRequestToPost(
        "disposal-date",
        TestCalculationController.submitDisposalDate,
        ("disposalDate.day", "31"), ("disposalDate.month","1"), ("disposalDate.year","2016")
      )
      val testModel = new DisposalDateModel(31,1,2016)

      "return a 303" in {
        keystoreCacheCondition[DisposalDateModel](testModel)
        status(DisposalDateTestDataItem.result) shouldBe 303
      }

      s"redirect to ${routes.CalculationController.disposalValue()}" in {
        keystoreCacheCondition[DisposalDateModel](testModel)
        redirectLocation(DisposalDateTestDataItem.result) shouldBe Some(s"${routes.CalculationController.disposalValue()}")
      }
    }
    "submitting a valid leap year date 29/02/2016" should {
      object DisposalDateTestDataItem extends fakeRequestToPost(
        "disposal-date",
        TestCalculationController.submitDisposalDate,
        ("disposalDate.day", "29"), ("disposalDate.month","2"), ("disposalDate.year","2016")
      )
      val testModel = new DisposalDateModel(29,2,2016)

      "return a 303" in {
        keystoreCacheCondition[DisposalDateModel](testModel)
        status(DisposalDateTestDataItem.result) shouldBe 303
      }

      s"redirect to ${routes.CalculationController.disposalValue()}" in {
        keystoreCacheCondition[DisposalDateModel](testModel)
        redirectLocation(DisposalDateTestDataItem.result) shouldBe Some(s"${routes.CalculationController.disposalValue()}")
      }
    }
    "submitting an invalid leap year date 29/02/2017" should {
      object DisposalDateTestDataItem extends fakeRequestToPost(
        "disposal-date",
        TestCalculationController.submitDisposalDate,
        ("disposalDate.day", "29"), ("disposalDate.month","2"), ("disposalDate.year","2017")
      )
      val testModel = new DisposalDateModel(29,2,2017)

      "return a 400" in {
        keystoreCacheCondition[DisposalDateModel](testModel)
        status(DisposalDateTestDataItem.result) shouldBe 400
      }

      s"should error with message '${Messages("calc.common.date.error.invalidDate")}'" in {
        keystoreCacheCondition[DisposalDateModel](testModel)
        DisposalDateTestDataItem.jsoupDoc.select(".error-notification").text should include (Messages("calc.common.date.error.invalidDate"))
      }
    }
    "submitting a day less than 1" should {
      object DisposalDateTestDataItem extends fakeRequestToPost(
        "disposal-date",
        TestCalculationController.submitDisposalDate,
        ("disposalDate.day", "0"), ("disposalDate.month","2"), ("disposalDate.year","2017")
      )
      val testModel = new DisposalDateModel(0,2,2017)

      "return a 400" in {
        keystoreCacheCondition[DisposalDateModel](testModel)
        status(DisposalDateTestDataItem.result) shouldBe 400
      }

      s"should error with message '${Messages("calc.common.date.error.day.lessThan1")}'" in {
        keystoreCacheCondition[DisposalDateModel](testModel)
        DisposalDateTestDataItem.jsoupDoc.select(".error-notification").text should include (Messages("calc.common.date.error.invalidDate"))
      }
    }
    "submitting a day greater than 31" should {
      object DisposalDateTestDataItem extends fakeRequestToPost(
        "disposal-date",
        TestCalculationController.submitDisposalDate,
        ("disposalDate.day", "32"), ("disposalDate.month","2"), ("disposalDate.year","2017")
      )
      val testModel = new DisposalDateModel(32,2,2017)

      "return a 400" in {
        keystoreCacheCondition[DisposalDateModel](testModel)
        status(DisposalDateTestDataItem.result) shouldBe 400
      }

      s"should error with message '${Messages("calc.common.date.error.day.greaterThan31")}'" in {
        keystoreCacheCondition[DisposalDateModel](testModel)
        DisposalDateTestDataItem.jsoupDoc.select(".error-notification").text should include (Messages("calc.common.date.error.invalidDate"))
      }
    }
    "submitting a month greater than 12" should {
      object DisposalDateTestDataItem extends fakeRequestToPost(
        "disposal-date",
        TestCalculationController.submitDisposalDate,
        ("disposalDate.day", "31"), ("disposalDate.month","13"), ("disposalDate.year","2017")
      )
      val testModel = new DisposalDateModel(31,13,2017)

      "return a 400" in {
        keystoreCacheCondition[DisposalDateModel](testModel)
        status(DisposalDateTestDataItem.result) shouldBe 400
      }

      s"should error with message '${Messages("calc.common.date.error.month.greaterThan12")}'" in {
        keystoreCacheCondition[DisposalDateModel](testModel)
        DisposalDateTestDataItem.jsoupDoc.select(".error-notification").text should include (Messages("calc.common.date.error.invalidDate"))
      }
    }
    "submitting a month less than 1" should {
      object DisposalDateTestDataItem extends fakeRequestToPost(
        "disposal-date",
        TestCalculationController.submitDisposalDate,
        ("disposalDate.day", "31"), ("disposalDate.month","0"), ("disposalDate.year","2017")
      )
      val testModel = new DisposalDateModel(31,0,2017)

      "return a 400" in {
        keystoreCacheCondition[DisposalDateModel](testModel)
        status(DisposalDateTestDataItem.result) shouldBe 400
      }

      s"should error with message '${Messages("calc.common.date.error.month.lessThan1")}'" in {
        keystoreCacheCondition[DisposalDateModel](testModel)
        DisposalDateTestDataItem.jsoupDoc.select(".error-notification").text should include (Messages("calc.common.date.error.invalidDate"))
      }
    }
    "submitting a day with no value" should {
      object DisposalDateTestDataItem extends fakeRequestToPost(
        "disposal-date",
        TestCalculationController.submitDisposalDate,
        ("disposalDate.day", ""), ("disposalDate.month","12"), ("disposalDate.year","2017")
      )
      val testModel = new DisposalDateModel(0,12,2017)

      "return a 400" in {
        keystoreCacheCondition[DisposalDateModel](testModel)
        status(DisposalDateTestDataItem.result) shouldBe 400
      }

      "should error with message 'Numeric vaue expected'" in {
        keystoreCacheCondition[DisposalDateModel](testModel)
        DisposalDateTestDataItem.jsoupDoc.select(".error-notification").text should include ("You must supply a valid date")
      }
    }
    "submitting a month with no value" should {
      object DisposalDateTestDataItem extends fakeRequestToPost(
        "disposal-date",
        TestCalculationController.submitDisposalDate,
        ("disposalDate.day", "31"), ("disposalDate.month",""), ("disposalDate.year","2017")
      )
      val testModel = new DisposalDateModel(31,0,2017)

      "return a 400" in {
        keystoreCacheCondition[DisposalDateModel](testModel)
        status(DisposalDateTestDataItem.result) shouldBe 400
      }

      "should error with message 'Numeric vaue expected'" in {
        keystoreCacheCondition[DisposalDateModel](testModel)
        DisposalDateTestDataItem.jsoupDoc.select(".error-notification").text should include ("You must supply a valid date")
      }
    }
    "submitting a year with no value" should {
      object DisposalDateTestDataItem extends fakeRequestToPost(
        "disposal-date",
        TestCalculationController.submitDisposalDate,
        ("disposalDate.day", "31"), ("disposalDate.month","12"), ("disposalDate.year","")
      )
      val testModel = new DisposalDateModel(31,12,0)

      "return a 400" in {
        keystoreCacheCondition[DisposalDateModel](testModel)
        status(DisposalDateTestDataItem.result) shouldBe 400
      }

      "should error with message 'You must supply a valid date'" in {
        keystoreCacheCondition[DisposalDateModel](testModel)
        DisposalDateTestDataItem.jsoupDoc.select(".error-notification").text should include ("You must supply a valid date")
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
          DisposalValueTestDataItem.jsoupDoc.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
        }

        "have the question 'How much did you sell or give away the property for?' as the legend of the input" in {
          keystoreFetchCondition[DisposalValueModel](None)
          DisposalValueTestDataItem.jsoupDoc.body.getElementsByTag("label").text should include (Messages("calc.disposalValue.question"))
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

  "In CalculationController calling the .submitDisposalValue action" when {
    def keystoreCacheCondition[T](data: DisposalValueModel): Unit = {
      lazy val returnedCacheMap = CacheMap("form-id", Map("data" -> Json.toJson(data)))
      when(mockCalcConnector.saveFormData[T](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(returnedCacheMap))
    }

    "submitting a valid form" should {
      val testModel = new DisposalValueModel(1000)
      object DisposalValueTestDataItem extends fakeRequestToPost (
        "disposal-value",
        TestCalculationController.submitDisposalValue,
        ("disposalValue", "1000")
      )

      "return a 303" in {
        keystoreCacheCondition(testModel)
        status(DisposalValueTestDataItem.result) shouldBe 303
      }
    }

    "submitting an invalid form with no value" should {
      val testModel = new DisposalValueModel(0)
      object DisposalValueTestDataItem extends fakeRequestToPost (
        "disposal-value",
        TestCalculationController.submitDisposalValue,
        ("disposalValue", "")
      )

      "return a 400" in {
        keystoreCacheCondition(testModel)
        status(DisposalValueTestDataItem.result) shouldBe 400
      }
    }

    "submitting an invalid form with a negative value" should {
      val testModel = new DisposalValueModel(-1000)
      object DisposalValueTestDataItem extends fakeRequestToPost (
        "disposal-value",
        TestCalculationController.submitDisposalValue,
        ("disposalValue", "-1000")
      )

      "return a 400" in {
        keystoreCacheCondition(testModel)
        status(DisposalValueTestDataItem.result) shouldBe 400
      }
    }

    "submitting an invalid form with value 1.111" should {
      val testModel = new DisposalValueModel(1.111)
      object DisposalValueTestDataItem extends fakeRequestToPost (
        "disposal-value",
        TestCalculationController.submitDisposalValue,
        ("disposalValue", "1.111")
      )

      "return a 400" in {
        keystoreCacheCondition(testModel)
        status(DisposalValueTestDataItem.result) shouldBe 400
      }

      s"fail with message ${Messages("calc.disposalValue.errorDecimalPlaces")}" in {
        keystoreCacheCondition(testModel)
        DisposalValueTestDataItem.jsoupDoc.getElementsByClass("error-notification").text should include (Messages("calc.disposalValue.errorDecimalPlaces"))
      }
    }
  }

  //################### Acquisition Costs tests #######################
  "In CalculationController calling the .acquisitionCosts action " should {
    "not supplied with a pre-existing stored model" should {
      object AcquisitionCostsTestDataItem extends fakeRequestTo("acquisition-costs", TestCalculationController.acquisitionCosts)

      "return a 200" in {
        keystoreFetchCondition[AcquisitionCostsModel](None)
        status(AcquisitionCostsTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          keystoreFetchCondition[AcquisitionCostsModel](None)
          contentType(AcquisitionCostsTestDataItem.result) shouldBe Some("text/html")
          charset(AcquisitionCostsTestDataItem.result) shouldBe Some("utf-8")
        }

        "have the title 'How much did you pay in costs when you became the property owner'" in {
          keystoreFetchCondition[AcquisitionCostsModel](None)
          AcquisitionCostsTestDataItem.jsoupDoc.getElementsByTag("title").text shouldEqual Messages("calc.acquisitionCosts.question")
        }

        "have a back link" in {
          keystoreFetchCondition[AcquisitionCostsModel](None)
          AcquisitionCostsTestDataItem.jsoupDoc.getElementById("back-link").text shouldEqual Messages("calc.base.back")
        }

        "have the page heading 'Calculate your tax (non-residents)'" in {
          keystoreFetchCondition[AcquisitionCostsModel](None)
          AcquisitionCostsTestDataItem.jsoupDoc.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a monetary field that" should {

          "have the title 'How much did you pay in costs when you became the property owner?'" in {
            keystoreFetchCondition[AcquisitionCostsModel](None)
            AcquisitionCostsTestDataItem.jsoupDoc.select("label[for=acquisitionCosts]").text should include (Messages("calc.acquisitionCosts.question"))
          }

          "have the help text 'Costs include agent fees, legal fees and surveys'" in {
            keystoreFetchCondition[AcquisitionCostsModel](None)
            AcquisitionCostsTestDataItem.jsoupDoc.select("span.form-hint").text shouldEqual Messages("calc.acquisitionCosts.helpText")
          }

          "have an input box for the acquisition costs" in {
            keystoreFetchCondition[AcquisitionCostsModel](None)
            AcquisitionCostsTestDataItem.jsoupDoc.getElementById("acquisitionCosts").tagName shouldBe "input"
          }
        }

        "have a continue button that" should {

          "be a button element" in {
            keystoreFetchCondition[AcquisitionCostsModel](None)
            AcquisitionCostsTestDataItem.jsoupDoc.getElementById("continue-button").tagName shouldBe "button"
          }

          "have the text 'Continue'" in {
            keystoreFetchCondition[AcquisitionCostsModel](None)
            AcquisitionCostsTestDataItem.jsoupDoc.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
          }
        }
      }
    }

    "supplied with a pre-existing stored model" should {
      object AcquisitionCostsTestDataItem extends fakeRequestTo("acquisition-costs", TestCalculationController.acquisitionCosts)
      val testModel = new AcquisitionCostsModel(Some(1000))

      "return a 200" in {
        keystoreFetchCondition[AcquisitionCostsModel](Some(testModel))
        status(AcquisitionCostsTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {
        "have the value 1000 auto-filled into the input box" in {
          keystoreFetchCondition[AcquisitionCostsModel](Some(testModel))
          AcquisitionCostsTestDataItem.jsoupDoc.getElementById("acquisitionCosts").attr("value") shouldEqual ("1000")
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

      "with value 1000" should {
        val testModel = new AcquisitionCostsModel(Some(1000))
        object AcquisitionCostsTestDataItem extends fakeRequestToPost(
          "acquisition-costs",
          TestCalculationController.submitAcquisitionCosts,
          ("acquisitionCosts", "1000")
        )

        "return a 303" in {
          keystoreCacheCondition(testModel)
          status(AcquisitionCostsTestDataItem.result) shouldBe 303
        }

        s"redirect to ${routes.CalculationController.disposalCosts()}" in {
          keystoreCacheCondition[AcquisitionCostsModel](testModel)
          redirectLocation(AcquisitionCostsTestDataItem.result) shouldBe Some(s"${routes.CalculationController.disposalCosts()}")
        }
      }

      "with no value" should {
        val testModel = new AcquisitionCostsModel(Some(0))
        object AcquisitionCostsTestDataItem extends fakeRequestToPost(
          "acquisition-costs",
          TestCalculationController.submitAcquisitionCosts,
          ("acquisitionCosts", "")
        )

        "return a 303" in {
          keystoreCacheCondition(testModel)
          status(AcquisitionCostsTestDataItem.result) shouldBe 303
        }

        s"redirect to ${routes.CalculationController.disposalCosts()}" in {
          keystoreCacheCondition[AcquisitionCostsModel](testModel)
          redirectLocation(AcquisitionCostsTestDataItem.result) shouldBe Some(s"${routes.CalculationController.disposalCosts()}")
        }
      }
    }

    "submitting an invalid form" should {
      val testModel = new AcquisitionCostsModel(Some(0))

      "with value -1" should {

        object AcquisitionCostsTestDataItem extends fakeRequestToPost(
          "acquisition-costs",
          TestCalculationController.submitAcquisitionCosts,
          ("acquisitionCosts", "-1")
        )

        "return a 400" in {
          keystoreCacheCondition(testModel)
          status(AcquisitionCostsTestDataItem.result) shouldBe 400
        }

        s"fail with message ${Messages("calc.acquisitionCosts.errorNegative")}" in {
          keystoreCacheCondition(testModel)
          AcquisitionCostsTestDataItem.jsoupDoc.getElementsByClass("error-notification").text should include (Messages("calc.acquisitionCosts.errorNegative"))
        }
      }

      "with value 1.111" should {

        object AcquisitionCostsTestDataItem extends fakeRequestToPost(
          "acquisition-costs",
          TestCalculationController.submitAcquisitionCosts,
          ("acquisitionCosts", "1.111")
        )

        "return a 400" in {
          keystoreCacheCondition(testModel)
          status(AcquisitionCostsTestDataItem.result) shouldBe 400
        }

        s"fail with message ${Messages("calc.acquisitionCosts.errorDecimalPlaces")}" in {
          keystoreCacheCondition(testModel)
          AcquisitionCostsTestDataItem.jsoupDoc.getElementsByClass("error-notification").text should include(Messages("calc.acquisitionCosts.errorDecimalPlaces"))
        }
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
          DisposalCostsTestDataItem.jsoupDoc.getElementById("back-link").text shouldEqual Messages("calc.base.back")
        }

        "have the heading 'Calculate your tax (non-residents)'" in {
          DisposalCostsTestDataItem.jsoupDoc.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a monetary field that" should {

          "have the title 'How much did you pay in costs when you became the property owner?'" in {
            DisposalCostsTestDataItem.jsoupDoc.select("label[for=disposalCosts]").text should include (Messages("calc.disposalCosts.question"))
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
      val disposalCostsTestModel = new DisposalCostsModel(Some(1000))

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

    "In CalculationController calling the .submitDisposalCosts action" when {
      def keystoreCacheCondition[T](data: DisposalCostsModel): Unit = {
        lazy val returnedCacheMap = CacheMap("form-id", Map("data" -> Json.toJson(data)))
        when(mockCalcConnector.saveFormData[T](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(returnedCacheMap))
      }

      "submitting a valid form" should {
        val disposalCostsTestModel = new DisposalCostsModel(Some(1000))
        object DisposalCostsTestDataItem extends fakeRequestToPost(
          "disposal-costs",
          TestCalculationController.submitDisposalCosts,
          ("disposalCosts", "1000")
        )

        "return a 303" in {
          keystoreCacheCondition(disposalCostsTestModel)
          status(DisposalCostsTestDataItem.result) shouldBe 303
        }

        s"redirect to ${routes.CalculationController.entrepreneursRelief()}" in {
          keystoreCacheCondition[DisposalCostsModel](disposalCostsTestModel)
          redirectLocation(DisposalCostsTestDataItem.result) shouldBe Some(s"${routes.CalculationController.entrepreneursRelief()}")
        }
      }

      "submitting an valid form with no value" should {
        val disposalCostsTestModel = new DisposalCostsModel(Some(0))
        object DisposalCostsTestDataItem extends fakeRequestToPost(
          "disposal-costs",
          TestCalculationController.submitDisposalCosts,
          ("disposalCosts", "")
        )

        "return a 303" in {
          keystoreCacheCondition(disposalCostsTestModel)
          status(DisposalCostsTestDataItem.result) shouldBe 303
        }
      }
      "submitting an invalid form with a negative value of -432" should {
        val disposalCostsTestModel = new DisposalCostsModel(Some(0))
        object DisposalCostsTestDataItem extends fakeRequestToPost(
          "disposal-costs",
          TestCalculationController.submitDisposalCosts,
          ("disposalCosts", "-432")
        )

        "return a 400" in {
          keystoreCacheCondition(disposalCostsTestModel)
          status(DisposalCostsTestDataItem.result) shouldBe 400
        }

        "display the error message 'Disposal costs can't be negative'" in {
          keystoreFetchCondition[DisposalCostsModel](None)
          DisposalCostsTestDataItem.jsoupDoc.select("div label span.error-notification").text shouldEqual Messages("calc.disposalCosts.errorNegativeNumber")
        }
      }

      "submitting an invalid form with a value that has more than two decimal places" should {
        val disposalCostsTestModel = new DisposalCostsModel(Some(0))
        object DisposalCostsTestDataItem extends fakeRequestToPost(
          "disposal-costs",
          TestCalculationController.submitDisposalCosts,
          ("disposalCosts", "432.00023")
        )

        "return a 400" in {
          keystoreCacheCondition(disposalCostsTestModel)
          status(DisposalCostsTestDataItem.result) shouldBe 400
        }

        "display the error message 'The costs have too many decimal places'" in {
          keystoreFetchCondition[DisposalCostsModel](None)
          DisposalCostsTestDataItem.jsoupDoc.select("div label span.error-notification").text shouldEqual Messages("calc.disposalCosts.errorDecimalPlaces")
        }
      }

      "submitting an invalid form with a value that is negative and has more than two decimal places" should {
        val disposalCostsTestModel = new DisposalCostsModel(Some(0))
        object DisposalCostsTestDataItem extends fakeRequestToPost(
          "disposal-costs",
          TestCalculationController.submitDisposalCosts,
          ("disposalCosts", "-432.00023")
        )

        "return a 400" in {
          keystoreCacheCondition(disposalCostsTestModel)
          status(DisposalCostsTestDataItem.result) shouldBe 400
        }

        "display the error message 'Disposal costs cannot be negative' and 'The costs have too many decimal places'" in {
          keystoreFetchCondition[DisposalCostsModel](None)
          DisposalCostsTestDataItem.jsoupDoc.select("div label span.error-notification").text shouldEqual (Messages("calc.disposalCosts.errorNegativeNumber") + " " + Messages("calc.disposalCosts.errorDecimalPlaces"))
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
          EntrepreneursReliefTestDataItem.jsoupDoc.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
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

  "In CalculationController calling the .submitEntrepreneursRelief action" when {
    def keystoreCacheCondition[T](data: EntrepreneursReliefModel): Unit = {
      lazy val returnedCacheMap = CacheMap("form-id", Map("data" -> Json.toJson(data)))
      when(mockCalcConnector.saveFormData[T](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(returnedCacheMap))
    }
    "submitting a valid form with 'Yes'" should {
      object EntrepreneursReliefTestDataItem extends fakeRequestToPost(
        "entrepreneurs-relief",
        TestCalculationController.submitEntrepreneursRelief,
        ("entrepreneursRelief", "yes")
      )
      val testModel = new EntrepreneursReliefModel("Yes")

      "return a 303" in {
        keystoreCacheCondition[EntrepreneursReliefModel](testModel)
        status(EntrepreneursReliefTestDataItem.result) shouldBe 303
      }

      s"redirect to ${routes.CalculationController.allowableLosses()}" in {
        keystoreCacheCondition[EntrepreneursReliefModel](testModel)
        redirectLocation(EntrepreneursReliefTestDataItem.result) shouldBe Some(s"${routes.CalculationController.allowableLosses()}")
      }
    }

    "submitting a valid form with 'No'" should {
      object EntrepreneursReliefTestDataItem extends fakeRequestToPost(
        "entrepreneurs-relief",
        TestCalculationController.submitEntrepreneursRelief,
        ("entrepreneursRelief", "no")
      )
      val testModel = new EntrepreneursReliefModel("no")

      "return a 303" in {
        keystoreCacheCondition[EntrepreneursReliefModel](testModel)
        status(EntrepreneursReliefTestDataItem.result) shouldBe 303
      }

      s"redirect to ${routes.CalculationController.allowableLosses()}" in {
        keystoreCacheCondition[EntrepreneursReliefModel](testModel)
        redirectLocation(EntrepreneursReliefTestDataItem.result) shouldBe Some(s"${routes.CalculationController.allowableLosses()}")
      }
    }

    "submitting an invalid form with no data" should {
      object EntrepreneursReliefTestDataItem extends fakeRequestToPost(
        "entrepreneurs-relief",
        TestCalculationController.submitEntrepreneursRelief,
        ("entrepreneursRelief", "")
      )
      val testModel = new EntrepreneursReliefModel("")

      "return a 400" in {
        keystoreCacheCondition[EntrepreneursReliefModel](testModel)
        status(EntrepreneursReliefTestDataItem.result) shouldBe 400
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
          AllowableLossesTestDataItem.jsoupDoc.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
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
          AllowableLossesTestDataItem.jsoupDoc.select("label[for=allowableLossesAmt]").text should include (Messages("calc.allowableLosses.question.two"))
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
      val testModel = new AllowableLossesModel("Yes", Some(9999.54))

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
    "submitting a valid form with 'Yes' and an amount with no acquisition date" should {
      object AllowableLossesTestDataItem extends fakeRequestToPost(
        "allowable-losses",
        TestCalculationController.submitAllowableLosses,
        ("isClaimingAllowableLosses", "Yes"), ("allowableLossesAmt", "1000")
      )
      val testModel = new AllowableLossesModel("Yes", Some(1000))
      val acqDateModel = AcquisitionDateModel("No", None, None, None)

      "return a 303" in {
        keystoreFetchValue(Some(acqDateModel))
        keystoreCacheCondition[AllowableLossesModel](testModel)
        status(AllowableLossesTestDataItem.result) shouldBe 303
      }
    }

    "submitting a valid form with 'Yes' and an amount with two decimal places with an acquisition date after the tax start date" should {
      object AllowableLossesTestDataItem extends fakeRequestToPost(
        "allowable-losses",
        TestCalculationController.submitAllowableLosses,
        ("isClaimingAllowableLosses", "Yes"), ("allowableLossesAmt", "1000.11")
      )
      val testModel = new AllowableLossesModel("Yes", Some(1000.11))
      val acqDateModel = AcquisitionDateModel("Yes", Some(1), Some(1), Some(2016))

      "return a 303" in {
        keystoreFetchValue(Some(acqDateModel))
        keystoreCacheCondition[AllowableLossesModel](testModel)
        status(AllowableLossesTestDataItem.result) shouldBe 303
      }
    }

    "submitting a valid form with 'No' and a null amount with an acquisition date before the tax start date" should {
      object AllowableLossesTestDataItem extends fakeRequestToPost(
        "allowable-losses",
        TestCalculationController.submitAllowableLosses,
        ("isClaimingAllowableLosses", "No"), ("allowableLossesAmt", "")
      )
      val testModel = new AllowableLossesModel("No", None)
      val acqDateModel = AcquisitionDateModel("Yes", Some(1), Some(1), Some(2010))

      "return a 303" in {
        keystoreFetchValue(Some(acqDateModel))
        keystoreCacheCondition[AllowableLossesModel](testModel)
        status(AllowableLossesTestDataItem.result) shouldBe 303
      }
    }

    "submitting a valid form with 'No' and a negative amount with no returned acquisition date model" should {
      object AllowableLossesTestDataItem extends fakeRequestToPost(
        "allowable-losses",
        TestCalculationController.submitAllowableLosses,
        ("isClaimingAllowableLosses", "No"), ("allowableLossesAmt", "-1000")
      )
      val testModel = new AllowableLossesModel("No", Some(-1000))

      "return a 303" in {
        keystoreFetchValue(None)
        keystoreCacheCondition[AllowableLossesModel](testModel)
        status(AllowableLossesTestDataItem.result) shouldBe 303
      }
    }

    "submitting an invalid form with no selection and a null amount" should {
      object AllowableLossesTestDataItem extends fakeRequestToPost(
        "allowable-losses",
        TestCalculationController.submitAllowableLosses,
        ("isClaimingAllowableLosses", ""), ("allowableLossesAmt", "")
      )
      val testModel = new AllowableLossesModel("Yes", Some(1000))

      "return a 400" in {
        keystoreCacheCondition[AllowableLossesModel](testModel)
        status(AllowableLossesTestDataItem.result) shouldBe 400
      }
    }

    "submitting an invalid form with 'Yes' selection and a null amount" should {
      object AllowableLossesTestDataItem extends fakeRequestToPost(
        "allowable-losses",
        TestCalculationController.submitAllowableLosses,
        ("isClaimingAllowableLosses", "Yes"), ("allowableLossesAmt", "")
      )
      val testModel = new AllowableLossesModel("Yes", None)

      "return a 400" in {
        keystoreCacheCondition[AllowableLossesModel](testModel)
        status(AllowableLossesTestDataItem.result) shouldBe 400
      }
    }

    "submitting an invalid form with 'Yes' selection and an amount with three decimal places" should {
      object AllowableLossesTestDataItem extends fakeRequestToPost(
        "allowable-losses",
        TestCalculationController.submitAllowableLosses,
        ("isClaimingAllowableLosses", "Yes"), ("allowableLossesAmt", "1000.111")
      )
      val testModel = new AllowableLossesModel("Yes", Some(1000.111))

      "return a 400" in {
        keystoreCacheCondition[AllowableLossesModel](testModel)
        status(AllowableLossesTestDataItem.result) shouldBe 400
      }
    }

    "submitting an invalid form with 'Yes' selection and a negative amount" should {
      object AllowableLossesTestDataItem extends fakeRequestToPost(
        "allowable-losses",
        TestCalculationController.submitAllowableLosses,
        ("isClaimingAllowableLosses", "Yes"), ("allowableLossesAmt", "-1000")
      )
      val testModel = new AllowableLossesModel("Yes", Some(-1000))

      "return a 400" in {
        keystoreCacheCondition[AllowableLossesModel](testModel)
        status(AllowableLossesTestDataItem.result) shouldBe 400
      }
    }
  }

  //################### Calculation Election tests #########################

  "In CalculationController calling the .calculationElection action" when {

    "supplied with no pre-existing data" should {

      object CalculationElectionTestDataItem extends fakeRequestTo("calculation-election", TestCalculationController.calculationElection)

      "return a 200" in {
        keystoreFetchCondition[CalculationElectionModel](None)
        status(CalculationElectionTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set UTF-8" in {
          keystoreFetchCondition[CalculationElectionModel](None)
          contentType(CalculationElectionTestDataItem.result) shouldBe Some("text/html")
          charset(CalculationElectionTestDataItem.result) shouldBe Some("utf-8")
        }

        "have the title Which method of calculation would you like?" in {
          keystoreFetchCondition[CalculationElectionModel](None)
          CalculationElectionTestDataItem.jsoupDoc.title shouldEqual Messages("calc.calculationElection.question")
        }

        "have the heading Calculate your tax (non-residents) " in {
          keystoreFetchCondition[CalculationElectionModel](None)
          CalculationElectionTestDataItem.jsoupDoc.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a 'Back' link " in {
          keystoreFetchCondition[CalculationElectionModel](None)
          CalculationElectionTestDataItem.jsoupDoc.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
        }

        "have the paragraph You can decide what to base your Capital Gains Tax on. It affects how much you'll pay." in {
          keystoreFetchCondition[CalculationElectionModel](None)
          CalculationElectionTestDataItem.jsoupDoc.body.getElementById("question-information").text shouldEqual Messages("calc.calculationElection.message")
        }

        "have a calculationElectionHelper for the option of a time apportioned calculation rendered on the page" in {
          keystoreFetchCondition[CalculationElectionModel](None)
          CalculationElectionTestDataItem.jsoupDoc.body.getElementById("calculationElection-time").attr("value") shouldEqual "time"
          CalculationElectionTestDataItem.jsoupDoc.body.getElementById("time-para").text shouldEqual "Based on " + Messages("calc.calculationElection.message.time") + " " + Messages("calc.calculationElection.message.timeDate")
        }

        "display a 'Continue' button " in {
          keystoreFetchCondition[CalculationElectionModel](None)
          CalculationElectionTestDataItem.jsoupDoc.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
        }

        "display a concertina information box with 'They sometimes qualify for larger tax reliefs. This can lower the amount you owe or even reduce it to zero' as the content" in {
          keystoreFetchCondition[CalculationElectionModel](None)
          CalculationElectionTestDataItem.jsoupDoc.select("summary span.summary").text shouldEqual Messages("calc.calculationElection.message.whyMore")
          CalculationElectionTestDataItem.jsoupDoc.select("div#details-content-0 p").text shouldEqual Messages("calc.calculationElection.message.whyMoreDetails")
        }
        "have no pre-selected option" in {
          keystoreFetchCondition[CalculationElectionModel](None)
          CalculationElectionTestDataItem.jsoupDoc.body.getElementById("calculationElection-flat").parent.classNames().contains("selected") shouldBe false
        }
      }
    }

    "supplied with pre-existing data" should {

      object CalculationElectionTestDataItem extends fakeRequestTo("calculation-election", TestCalculationController.calculationElection)
      val calculationElectionTestModel = new CalculationElectionModel("time")

      "return a 200" in {
        keystoreFetchCondition[CalculationElectionModel](Some(calculationElectionTestModel))
        status(CalculationElectionTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          keystoreFetchCondition[CalculationElectionModel](Some(calculationElectionTestModel))
          contentType(CalculationElectionTestDataItem.result) shouldBe Some("text/html")
          charset(CalculationElectionTestDataItem.result) shouldBe Some("utf-8")
        }

        "have the stored value of time selected" in {
          keystoreFetchCondition[CalculationElectionModel](Some(calculationElectionTestModel))
          CalculationElectionTestDataItem.jsoupDoc.body.getElementById("calculationElection-time").parent.classNames().contains("selected") shouldBe true
        }
      }
    }
  }

  "In CalculationController calling the .submitCalculationElection action" when {

    def keystoreCacheCondition[T](data: CalculationElectionModel): Unit = {
      lazy val returnedCacheMap = CacheMap("form-id", Map("data" -> Json.toJson(data)))
      when(mockCalcConnector.saveFormData[T](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(returnedCacheMap))
    }

    "submitting a valid form with 'flat' selected" should {
      object CalculationElectionTestDataItem extends fakeRequestToPost("calculation-election", TestCalculationController.submitCalculationElection, ("calculationElection", "flat"))
      val calculationElectionTestModel = new CalculationElectionModel("flat")

      "return a 303" in {
        keystoreSummaryValue(sumModelFlat)
        keystoreFlatCalculateValue(Some(calcModelOneRate))
        keystoreCacheCondition[CalculationElectionModel](calculationElectionTestModel)
        status(CalculationElectionTestDataItem.result) shouldBe 303
      }

      "redirect to the summary page" in {
        redirectLocation(CalculationElectionTestDataItem.result) shouldBe Some(s"${routes.CalculationController.summary}")
      }
    }

    "submitting a valid form with 'time' selected" should {
      object CalculationElectionTestDataItem extends fakeRequestToPost("calculation-election", TestCalculationController.submitCalculationElection, ("calculationElection", "time"))
      val calculationElectionTestModel = new CalculationElectionModel("time")

      "return a 303" in {
        keystoreSummaryValue(sumModelTA)
        keystoreFlatCalculateValue(Some(calcModelOneRate))
        keystoreCacheCondition[CalculationElectionModel](calculationElectionTestModel)
        status(CalculationElectionTestDataItem.result) shouldBe 303
      }
    }

    "submitting a valid form with 'rebased' selected" should {
      object CalculationElectionTestDataItem extends fakeRequestToPost("calculation-election", TestCalculationController.submitCalculationElection, ("calculationElection", "rebased"))
      val calculationElectionTestModel = new CalculationElectionModel("rebased")

      "return a 303" in {
        keystoreSummaryValue(sumModelTA)
        keystoreFlatCalculateValue(Some(calcModelOneRate))
        keystoreCacheCondition[CalculationElectionModel](calculationElectionTestModel)
        status(CalculationElectionTestDataItem.result) shouldBe 303
      }
    }

    "submitting a form with no data" should  {
      object CalculationElectionTestDataItem extends fakeRequestToPost("calculation-election", TestCalculationController.submitCalculationElection)
      val calculationElectionTestModel = new CalculationElectionModel("")

      "return a 400" in {
        keystoreSummaryValue(sumModelFlat)
        keystoreFlatCalculateValue(Some(calcModelOneRate))
        keystoreCacheCondition[CalculationElectionModel](calculationElectionTestModel)
        status(CalculationElectionTestDataItem.result) shouldBe 400
      }
    }

    "submitting a form with completely unrelated 'ew1234qwer'" should  {
      object CalculationElectionTestDataItem extends fakeRequestToPost("calculation-election", TestCalculationController.submitCalculationElection, ("calculationElection", "ew1234qwer"))
      val calculationElectionTestModel = new CalculationElectionModel("ew1234qwer")

      "return a 400" in {
        keystoreSummaryValue(sumModelFlat)
        keystoreFlatCalculateValue(Some(calcModelOneRate))
        keystoreCacheCondition[CalculationElectionModel](calculationElectionTestModel)
        status(CalculationElectionTestDataItem.result) shouldBe 400
      }
    }
  }

  //################### Other Reliefs tests #######################
  "In CalculationController calling the .otherReliefs action " when {
    "not supplied with a pre-existing stored model" should {
      object OtherReliefsTestDataItem extends fakeRequestTo("other-reliefs", TestCalculationController.otherReliefs)

      "return a 200 with a valid calculation result" in {
        keystoreSummaryValue(sumModelFlat)
        keystoreFlatCalculateValue(Some(calcModelTwoRates))
        keystoreFetchCondition[OtherReliefsModel](None)
        status(OtherReliefsTestDataItem.result) shouldBe 200
      }

      "return a 200 with an invalid calculation result" in {
        object OtherReliefsTestDataItem extends fakeRequestTo("other-reliefs", TestCalculationController.otherReliefs)
        keystoreSummaryValue(sumModelFlat)
        keystoreFlatCalculateValue(None)
        keystoreFetchCondition[OtherReliefsModel](None)
        status(OtherReliefsTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          keystoreSummaryValue(sumModelFlat)
          keystoreFlatCalculateValue(Some(calcModelTwoRates))
          contentType(OtherReliefsTestDataItem.result) shouldBe Some("text/html")
          charset(OtherReliefsTestDataItem.result) shouldBe Some("utf-8")
        }
        "have the title 'How much extra tax relief are you claiming?'" in {
          keystoreSummaryValue(sumModelFlat)
          keystoreFlatCalculateValue(Some(calcModelTwoRates))
          OtherReliefsTestDataItem.jsoupDoc.title shouldEqual Messages("calc.otherReliefs.question")
        }

        "have the heading Calculate your tax (non-residents) " in {
          keystoreSummaryValue(sumModelFlat)
          keystoreFlatCalculateValue(Some(calcModelTwoRates))
          OtherReliefsTestDataItem.jsoupDoc.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a 'Back' link " in {
          keystoreSummaryValue(sumModelFlat)
          keystoreFlatCalculateValue(Some(calcModelTwoRates))
          OtherReliefsTestDataItem.jsoupDoc.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
        }

        "have the question 'How much extra tax relief are you claiming?' as the legend of the input" in {
          keystoreSummaryValue(sumModelFlat)
          keystoreFlatCalculateValue(Some(calcModelTwoRates))
          OtherReliefsTestDataItem.jsoupDoc.body.getElementsByTag("label").text should include (Messages("calc.otherReliefs.question"))
        }

        "display an input box for the Other Tax Reliefs" in {
          keystoreSummaryValue(sumModelFlat)
          keystoreFlatCalculateValue(Some(calcModelTwoRates))
          OtherReliefsTestDataItem.jsoupDoc.body.getElementById("otherReliefs").tagName() shouldEqual "input"
        }

        "display an 'Add relief' button " in {
          keystoreSummaryValue(sumModelFlat)
          keystoreFlatCalculateValue(Some(calcModelTwoRates))
          OtherReliefsTestDataItem.jsoupDoc.body.getElementById("add-relief-button").text shouldEqual Messages("calc.otherReliefs.button.addRelief")
        }

        "include helptext for 'Total gain'" in {
          keystoreSummaryValue(sumModelFlat)
          keystoreFlatCalculateValue(Some(calcModelTwoRates))
          OtherReliefsTestDataItem.jsoupDoc.body.getElementById("totalGain").text should include (Messages("calc.otherReliefs.totalGain"))
        }

        "include helptext for 'Taxable gain'" in {
          keystoreSummaryValue(sumModelFlat)
          keystoreFlatCalculateValue(Some(calcModelTwoRates))
          OtherReliefsTestDataItem.jsoupDoc.body.getElementById("taxableGain").text should include (Messages("calc.otherReliefs.taxableGain"))
        }
      }
    }
    "supplied with a pre-existing stored model" should {
      object OtherReliefsTestDataItem extends fakeRequestTo("other-reliefs", TestCalculationController.otherReliefs)
      val testOtherReliefsModel = new OtherReliefsModel(Some(5000))

      "return a 200 with a valid calculation call" in {
        keystoreSummaryValue(sumModelFlat)
        keystoreFlatCalculateValue(Some(calcModelTwoRates))
        keystoreFetchCondition[OtherReliefsModel](Some(testOtherReliefsModel))
        status(OtherReliefsTestDataItem.result) shouldBe 200
      }

      "return a 200 with an invalid calculation call" in {
        object OtherReliefsTestDataItem extends fakeRequestTo("other-reliefs", TestCalculationController.otherReliefs)
        keystoreSummaryValue(sumModelFlat)
        keystoreFlatCalculateValue(None)
        keystoreFetchCondition[OtherReliefsModel](Some(testOtherReliefsModel))
        status(OtherReliefsTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          keystoreSummaryValue(sumModelFlat)
          keystoreFlatCalculateValue(Some(calcModelTwoRates))
          contentType(OtherReliefsTestDataItem.result) shouldBe Some("text/html")
          charset(OtherReliefsTestDataItem.result) shouldBe Some("utf-8")
        }

        "have the value 5000 auto-filled into the input box" in {
          keystoreSummaryValue(sumModelFlat)
          keystoreFlatCalculateValue(Some(calcModelTwoRates))
          keystoreFetchCondition[OtherReliefsModel](Some(testOtherReliefsModel))
          OtherReliefsTestDataItem.jsoupDoc.getElementById("otherReliefs").attr("value") shouldEqual "5000"
        }


      }
    }
  }

  "In CalculationController calling the .submitOtherReliefs action" when {
    def keystoreCacheCondition[T](data: OtherReliefsModel): Unit = {
      lazy val returnedCacheMap = CacheMap("form-id", Map("data" -> Json.toJson(data)))
      when(mockCalcConnector.saveFormData[T](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(returnedCacheMap))
    }

    "submitting a valid form with and an amount of 1000" should {
      object OtherReliefsTestDataItem extends fakeRequestToPost("other-reliefs", TestCalculationController.submitOtherReliefs, ("otherReliefs", "1000"))
      val otherReliefsTestModel = new OtherReliefsModel(Some(1000))

      "return a 303" in {
        keystoreSummaryValue(sumModelFlat)
        keystoreFlatCalculateValue(Some(calcModelTwoRates))
        keystoreCacheCondition[OtherReliefsModel](otherReliefsTestModel)
        status(OtherReliefsTestDataItem.result) shouldBe 303
      }
    }

    "submitting a valid form with and an amount with two decimal places" should {
      object OtherReliefsTestDataItem extends fakeRequestToPost("other-reliefs", TestCalculationController.submitOtherReliefs, ("otherReliefs", "1000.11"))
      val otherReliefsTestModel = new OtherReliefsModel(Some(1000.11))

      "return a 303" in {
        keystoreSummaryValue(sumModelFlat)
        keystoreFlatCalculateValue(Some(calcModelTwoRates))
        keystoreCacheCondition[OtherReliefsModel](otherReliefsTestModel)
        status(OtherReliefsTestDataItem.result) shouldBe 303
      }
    }

    "submitting an valid form with no value" should {
      object OtherReliefsTestDataItem extends fakeRequestToPost("other-reliefs", TestCalculationController.submitOtherReliefs, ("otherReliefs", ""))
      val otherReliefsTestModel = new OtherReliefsModel(Some(0))

      "return a 303" in {
        keystoreSummaryValue(sumModelFlat)
        keystoreFlatCalculateValue(Some(calcModelTwoRates))
        keystoreCacheCondition[OtherReliefsModel](otherReliefsTestModel)
        status(OtherReliefsTestDataItem.result) shouldBe 303
      }
    }

    "submitting an invalid form with an amount with three decimal places" should {
      object OtherReliefsTestDataItem extends fakeRequestToPost("other-reliefs", TestCalculationController.submitOtherReliefs, ("otherReliefs", "1000.111"))
      val otherReliefsTestModel = new OtherReliefsModel(Some(1000.111))

      "return a 400" in {
        keystoreSummaryValue(sumModelFlat)
        keystoreFlatCalculateValue(Some(calcModelTwoRates))
        keystoreCacheCondition[OtherReliefsModel](otherReliefsTestModel)
        status(OtherReliefsTestDataItem.result) shouldBe 400
      }
    }

    "submitting an invalid form with a negative value" should {
      object OtherReliefsTestDataItem extends fakeRequestToPost("other-reliefs", TestCalculationController.submitOtherReliefs, ("otherReliefs", "-1000"))
      val otherReliefsTestModel = new OtherReliefsModel(Some(-1000))

      "return a 400" in {
        keystoreSummaryValue(sumModelFlat)
        keystoreFlatCalculateValue(Some(calcModelTwoRates))
        keystoreCacheCondition[OtherReliefsModel](otherReliefsTestModel)
        status(OtherReliefsTestDataItem.result) shouldBe 400
      }
    }

    "submitting an invalid form with an value of shdgsaf" should {
      object OtherReliefsTestDataItem extends fakeRequestToPost("other-reliefs", TestCalculationController.submitOtherReliefs, ("otherReliefs", "shdgsaf"))
      val otherReliefsTestModel = new OtherReliefsModel(Some(1000))

      "return a 400" in {
        keystoreSummaryValue(sumModelFlat)
        keystoreFlatCalculateValue(Some(calcModelTwoRates))
        keystoreCacheCondition[OtherReliefsModel](otherReliefsTestModel)
        status(OtherReliefsTestDataItem.result) shouldBe 400
      }
    }
  }

  //################### Time Apportioned Other Relief tests ###################
  "In CalculationController calling the .otherReliefsTA action " should  {
    keystoreFetchCondition[OtherReliefsModel](None)
    object OtherReliefsTATestDataItem extends fakeRequestTo("other-reliefs-time-apportioned", TestCalculationController.otherReliefsTA)

    "return a 200" in {
      status(OtherReliefsTATestDataItem.result) shouldBe 200
    }

    "return some HTML that" should {

      "contain some text and use the character set utf-8" in {
        contentType(OtherReliefsTATestDataItem.result) shouldBe Some("text/html")
        charset(OtherReliefsTATestDataItem.result) shouldBe Some("utf-8")
      }

      "have the title 'How much extra tax relief are you claiming?'" in {
        OtherReliefsTATestDataItem.jsoupDoc.title shouldEqual Messages("calc.otherReliefs.question")
      }

      "have the heading Calculate your tax (non-residents) " in {
        OtherReliefsTATestDataItem.jsoupDoc.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
      }

      "have a 'Back' link " in {
        OtherReliefsTATestDataItem.jsoupDoc.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
      }

      "have the question 'How much extra tax relief are you claiming?' as the legend of the input" in {
        OtherReliefsTATestDataItem.jsoupDoc.body.getElementsByTag("label").text should include (Messages("calc.otherReliefs.question"))
      }

      "display an input box for the Other Tax Reliefs" in {
        OtherReliefsTATestDataItem.jsoupDoc.body.getElementById("otherReliefs").tagName() shouldEqual "input"
      }

      "display an 'Add relief' button " in {
        OtherReliefsTATestDataItem.jsoupDoc.body.getElementById("add-relief-button").text shouldEqual Messages("calc.otherReliefs.button.addRelief")
      }

      "include helptext for 'Total gain'" in {
        OtherReliefsTATestDataItem.jsoupDoc.body.getElementById("totalGain").text should include (Messages("calc.otherReliefs.totalGain"))
      }

      "include helptext for 'Taxable gain'" in {
        OtherReliefsTATestDataItem.jsoupDoc.body.getElementById("taxableGain").text should include (Messages("calc.otherReliefs.taxableGain"))
      }
    }

    "when not supplied with any previous value" should {
      object OtherReliefsTATestDataItem extends fakeRequestTo("other-reliefs-time-apportioned", TestCalculationController.otherReliefsTA)

      "contain no pre-filled data" in {
        keystoreFetchCondition[OtherReliefsModel](None)
        OtherReliefsTATestDataItem.jsoupDoc.body.getElementById("otherReliefs").attr("value") shouldBe ""
      }
    }

    "when supplied with a previous value" should {
      val testModel = OtherReliefsModel(Some(1000))
      object OtherReliefsTATestDataItem extends fakeRequestTo("other-reliefs-time-apportioned", TestCalculationController.otherReliefsTA)

      "contain the pre-supplied data" in {
        keystoreFetchCondition[OtherReliefsModel](Some(testModel))
        OtherReliefsTATestDataItem.jsoupDoc.body.getElementById("otherReliefs").attr("value") shouldBe "1000"
      }
    }
  }

  "In CalculationController calling the .submitOtherReliefsTA action" when {
    def keystoreCacheCondition[T](data: OtherReliefsModel): Unit = {
      lazy val returnedCacheMap = CacheMap("form-id", Map("data" -> Json.toJson(data)))
      when(mockCalcConnector.saveFormData[T](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(returnedCacheMap))
    }

    "submitting a valid form with and an amount of 1000" should {
      object OtherReliefsTATestDataItem extends fakeRequestToPost("other-reliefs-time-apportioned", TestCalculationController.submitOtherReliefsTA, ("otherReliefs", "1000"))
      val otherReliefsTATestModel = new OtherReliefsModel(Some(1000))

      "return a 303" in {
        keystoreCacheCondition[OtherReliefsModel](otherReliefsTATestModel)
        status(OtherReliefsTATestDataItem.result) shouldBe 303
      }
    }

    "submitting a valid form with and an amount with two decimal places" should {
      object OtherReliefsTATestDataItem extends fakeRequestToPost("other-reliefs-time-apportioned", TestCalculationController.submitOtherReliefsTA, ("otherReliefs", "1000.11"))
      val otherReliefsTATestModel = new OtherReliefsModel(Some(1000.11))

      "return a 303" in {
        keystoreCacheCondition[OtherReliefsModel](otherReliefsTATestModel)
        status(OtherReliefsTATestDataItem.result) shouldBe 303
      }
    }

    "submitting an valid form with no value" should {
      object OtherReliefsTATestDataItem extends fakeRequestToPost("other-reliefs-time-apportioned", TestCalculationController.submitOtherReliefsTA, ("otherReliefs", ""))
      val otherReliefsTATestModel = new OtherReliefsModel(Some(0))

      "return a 303" in {
        keystoreCacheCondition[OtherReliefsModel](otherReliefsTATestModel)
        status(OtherReliefsTATestDataItem.result) shouldBe 303
      }
    }

    "submitting an invalid form with an amount with three decimal places" should {
      object OtherReliefsTATestDataItem extends fakeRequestToPost("other-reliefs-time-apportioned", TestCalculationController.submitOtherReliefsTA, ("otherReliefs", "1000.111"))
      val otherReliefsTATestModel = new OtherReliefsModel(Some(1000.111))

      "return a 400" in {
        keystoreCacheCondition[OtherReliefsModel](otherReliefsTATestModel)
        status(OtherReliefsTATestDataItem.result) shouldBe 400
      }
    }

    "submitting an invalid form with a negative value" should {
      object OtherReliefsTATestDataItem extends fakeRequestToPost("other-reliefs-time-apportioned", TestCalculationController.submitOtherReliefsTA, ("otherReliefs", "-1000"))
      val otherReliefsTATestModel = new OtherReliefsModel(Some(-1000))

      "return a 400" in {
        keystoreCacheCondition[OtherReliefsModel](otherReliefsTATestModel)
        status(OtherReliefsTATestDataItem.result) shouldBe 400
      }
    }

    "submitting an invalid form with an value of shdgsaf" should {
      object OtherReliefsTATestDataItem extends fakeRequestToPost("other-reliefs-time-apportioned", TestCalculationController.submitOtherReliefsTA, ("otherReliefs", "shdgsaf"))
      val otherReliefsTATestModel = new OtherReliefsModel(Some(1000))

      "return a 400" in {
        keystoreCacheCondition[OtherReliefsModel](otherReliefsTATestModel)
        status(OtherReliefsTATestDataItem.result) shouldBe 400
      }
    }
  }

  //################### Rebased Other Relief tests ###################
  "In CalculationController calling the .otherReliefsRebased action " should  {

    object OtherReliefsRebasedTestDataItem extends fakeRequestTo("other-reliefs-rebased", TestCalculationController.otherReliefsRebased)

    "return a 200" in {
      status(OtherReliefsRebasedTestDataItem.result) shouldBe 200
    }

    "return some HTML that" should {

      "contain some text and use the character set utf-8" in {
        contentType(OtherReliefsRebasedTestDataItem.result) shouldBe Some("text/html")
        charset(OtherReliefsRebasedTestDataItem.result) shouldBe Some("utf-8")
      }

      "have the title 'How much extra tax relief are you claiming?'" in {
        OtherReliefsRebasedTestDataItem.jsoupDoc.title shouldEqual Messages("calc.otherReliefs.question")
      }

      "have the heading Calculate your tax (non-residents) " in {
        OtherReliefsRebasedTestDataItem.jsoupDoc.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
      }

      "have a 'Back' link " in {
        OtherReliefsRebasedTestDataItem.jsoupDoc.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
      }

      "have the question 'How much extra tax relief are you claiming?' as the legend of the input" in {
        OtherReliefsRebasedTestDataItem.jsoupDoc.body.getElementsByTag("label").text should include (Messages("calc.otherReliefs.question"))
      }

      "display an input box for the Other Tax Reliefs" in {
        OtherReliefsRebasedTestDataItem.jsoupDoc.body.getElementById("otherReliefs").tagName() shouldEqual "input"
      }

      "display an 'Add relief' button " in {
        OtherReliefsRebasedTestDataItem.jsoupDoc.body.getElementById("add-relief-button").text shouldEqual Messages("calc.otherReliefs.button.addRelief")
      }

      "include helptext for 'Total gain'" in {
        OtherReliefsRebasedTestDataItem.jsoupDoc.body.getElementById("totalGain").text should include (Messages("calc.otherReliefs.totalGain"))
      }

      "include helptext for 'Taxable gain'" in {
        OtherReliefsRebasedTestDataItem.jsoupDoc.body.getElementById("taxableGain").text should include (Messages("calc.otherReliefs.taxableGain"))
      }
    }
  }

  //################### Summary tests #######################
  "In CalculationController calling the .summary action" when {

    "individual is chosen with a flat calculation" when {

      "the user has provided a value for the AEA" should {
        keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
        keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
        object SummaryTestDataItem extends fakeRequestTo("summary", TestCalculationController.summary)

        "return a 200" in {
          keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
          keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
          status(SummaryTestDataItem.result) shouldBe 200
        }

        "return some HTML that" should {

          "should have the title 'Summary'" in {
            keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
            keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
            SummaryTestDataItem.jsoupDoc.getElementsByTag("title").text shouldEqual Messages("calc.summary.title")
          }

          "have a back button" in {
            keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
            keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
            SummaryTestDataItem.jsoupDoc.getElementById("back-link").text shouldEqual Messages("calc.base.back")
          }

          "have the correct sub-heading 'You owe'" in {
            keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
            keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
            SummaryTestDataItem.jsoupDoc.select("h1 span").text shouldEqual Messages("calc.summary.secondaryHeading")
          }

          "have a result amount currently set to £8000.00" in {
            keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
            keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
            SummaryTestDataItem.jsoupDoc.select("h1 b").text shouldEqual "£8000.00"
          }

          "have a 'Calculation details' section that" should {

            "include the section heading 'Calculation details" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#calcDetails").text should include(Messages("calc.summary.calculation.details.title"))
            }

            "include 'How would you like to work out your tax?'" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#calcDetails").text should include(Messages("calc.summary.calculation.details.calculationElection"))
            }

            "have an election description of 'How much of your total gain you've made since 5 April 2015'" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body().getElementById("calcDetails(0)").text() shouldBe Messages("calc.summary.calculation.details.flatCalculation")
            }

            "include 'Your total gain'" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#calcDetails").text should include(Messages("calc.summary.calculation.details.totalGain"))
            }

            "have a total gain equal to £40000.00" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body().getElementById("calcDetails(1)").text() shouldBe "£40000.00"
            }

            "include 'Your taxable gain'" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#calcDetails").text should include(Messages("calc.summary.calculation.details.taxableGain"))
            }

            "have a taxable gain equal to £40000.00" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body().getElementById("calcDetails(2)").text() shouldBe "£40000.00"
            }

            "include 'Your tax rate'" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#calcDetails").text should include(Messages("calc.summary.calculation.details.taxRate"))
            }

            "have a base tax rate of £32000" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body().getElementById("calcDetails(3)").text() shouldBe "£32000.00 at 18%"
            }

            "have an upper tax rate of £8000" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body().getElementById("calcDetails(4)").text() shouldBe "£8000.00 at 28%"
            }

          }

          "have a 'Personal details' section that" should {

            "include the section heading 'Personal details" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#personalDetails").text should include(Messages("calc.summary.personal.details.title"))
            }

            "include the question 'Who owned the property?'" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#personalDetails").text should include(Messages("calc.customerType.question"))
            }

            "have an 'individual' owner" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body().getElementById("personalDetails(0)").text() shouldBe "Individual"
            }

            "include the question 'What’s your total income for this tax year?'" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#personalDetails").text should include(Messages("calc.currentIncome.question"))
            }

            "have an total income of £1000" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body().getElementById("personalDetails(1)").text() shouldBe "£1000.00"
            }

            "include the question 'What's your Personal Allowance for this tax year?'" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#personalDetails").text should include(Messages("calc.personalAllowance.question"))
            }

            "have a personal allowance of £9000" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body().getElementById("personalDetails(2)").text() shouldBe "£9000.00"
            }

            "include the question 'How much of your Capital Gains Tax allowance have you got left'" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#personalDetails").text should include(Messages("calc.annualExemptAmount.question"))
            }

            "have a remaining CGT Allowance of £1500" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body().getElementById("personalDetails(3)").text() shouldBe "£1500.00"
            }
          }

          "have a 'Purchase details' section that" should {

            "include the section heading 'Purchase details" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#purchaseDetails").text should include(Messages("calc.summary.purchase.details.title"))
            }

            "include the question 'How much did you pay for the property?'" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#purchaseDetails").text should include(Messages("calc.acquisitionValue.question"))
            }

            "have an acquisition value of £100000" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body().getElementById("purchaseDetails(0)").text() shouldBe "£100000.00"
            }

            "include the question 'How much did you pay in costs when you became the property owner?'" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#purchaseDetails").text should include(Messages("calc.acquisitionCosts.question"))
            }

            "have a acquisition costs of £0" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body().getElementById("purchaseDetails(1)").text() shouldBe "£0.00"
            }
          }

          "have a 'Property details' section that" should {

            "include the section heading 'Property details" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#propertyDetails").text should include(Messages("calc.summary.property.details.title"))
            }

            "include the question 'Did you make any improvements to the property?'" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#propertyDetails").text should include(Messages("calc.improvements.question"))
            }

            "the answer to the improvements question should be No" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body.getElementById("propertyDetails(0)").text shouldBe "No"
            }
          }

          "have a 'Sale details' section that" should {

            "include the section heading 'Sale details" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#saleDetails").text should include(Messages("calc.summary.sale.details.title"))
            }

            "include the question 'When did you sign the contract that made someone else the owner?'" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#saleDetails").text should include(Messages("calc.disposalDate.question"))
            }

            "the date of disposal should be '10 October 2010" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body().getElementById("saleDetails(0)").text shouldBe "10 October 2010"
            }

            "include the question 'How much did you sell or give away the property for?'" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#saleDetails").text should include(Messages("calc.disposalValue.question"))
            }

            "the value of the sale should be £150000" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body().getElementById("saleDetails(1)").text shouldBe "£150000.00"
            }

            "include the question 'How much did you pay in costs when you stopped being the property owner?'" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#saleDetails").text should include(Messages("calc.disposalCosts.question"))
            }

            "the value of the costs should be £0" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body().getElementById("saleDetails(2)").text shouldBe "£0.00"
            }
          }

          "have a 'Deductions details' section that" should {

            "include the section heading 'Deductions" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#deductions").text should include(Messages("calc.summary.deductions.title"))
            }

            "include the question 'Are you claiming Entrepreneurs' Relief?'" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#deductions").text should include(Messages("calc.entrepreneursRelief.question"))
            }

            "have the answer to entrepreneurs relief question be 'No'" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body().getElementById("deductions(0)").text shouldBe "No"
            }

            "include the question 'Whats the total value of your allowable losses?'" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#deductions").text should include(Messages("calc.allowableLosses.question.two"))
            }

            "the value of allowable losses should be £0" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body().getElementById("deductions(1)").text shouldBe "£0.00"
            }

            "include the question 'What other reliefs are you claiming?'" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#deductions").text should include(Messages("calc.otherReliefs.question"))
            }

            "the value of other reliefs should be £0" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body().getElementById("deductions(2)").text shouldBe "£0.00"
            }

          }

          "have a 'What to do next' section that" should {

            "have the heading 'What to do next'" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#whatToDoNext H2").text shouldEqual (Messages("calc.common.next.actions.heading"))
            }

            "include the text 'You need to tell HMRC about the property'" in {
              keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
              keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#whatToDoNext").text should
                include(Messages("calc.summary.next.actions.text"))
              include(Messages("calc.summary.next.actions.link"))
            }
          }

          "have a link to 'Start again'" in {
            keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
            keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
            SummaryTestDataItem.jsoupDoc.select("#startAgain").text shouldEqual Messages("calc.summary.startAgain")
          }
        }
      }

      "the user has provided no value for the AEA" should {
        keystoreSummaryValue(TestModels.summaryIndividualFlatWithoutAEA)
        keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
        object SummaryTestDataItem extends fakeRequestTo("summary", TestCalculationController.summary)

        "have a remaining CGT Allowance of £11100" in {
          keystoreSummaryValue(TestModels.summaryIndividualFlatWithoutAEA)
          keystoreFlatCalculateValue(Some(TestModels.calcModelOneRate))
          SummaryTestDataItem.jsoupDoc.body().getElementById("personalDetails(3)").text() shouldBe "£11100.00"
        }

        "the answer to the improvements question should be Yes" in {
          keystoreSummaryValue(TestModels.summaryIndividualFlatWithAEA)
          keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
          SummaryTestDataItem.jsoupDoc.body.getElementById("propertyDetails(0)").text shouldBe "Yes"
        }

        "the value of the improvements should be £8000" in {
          keystoreSummaryValue(TestModels.summaryIndividualFlatWithoutAEA)
          keystoreFlatCalculateValue(Some(TestModels.calcModelOneRate))
          SummaryTestDataItem.jsoupDoc.body.getElementById("propertyDetails(1)").text shouldBe "£8000.00"
        }

        "the value of the disposal costs should be £600" in {
          keystoreSummaryValue(TestModels.summaryIndividualFlatWithoutAEA)
          keystoreFlatCalculateValue(Some(TestModels.calcModelOneRate))
          SummaryTestDataItem.jsoupDoc.body().getElementById("saleDetails(2)").text shouldBe "£600.00"
        }

        "have a acquisition costs of £300" in {
          keystoreSummaryValue(TestModels.summaryIndividualFlatWithoutAEA)
          keystoreFlatCalculateValue(Some(TestModels.calcModelOneRate))
          SummaryTestDataItem.jsoupDoc.body().getElementById("purchaseDetails(1)").text() shouldBe "£300.00"
        }

        "the value of allowable losses should be £50000" in {
          keystoreSummaryValue(TestModels.summaryIndividualFlatWithoutAEA)
          keystoreFlatCalculateValue(Some(TestModels.calcModelOneRate))
          SummaryTestDataItem.jsoupDoc.body().getElementById("deductions(1)").text shouldBe "£50000.00"
        }

        "the value of other reliefs should be £999" in {
          keystoreSummaryValue(TestModels.summaryIndividualFlatWithoutAEA)
          keystoreFlatCalculateValue(Some(TestModels.calcModelOneRate))
          SummaryTestDataItem.jsoupDoc.body().getElementById("deductions(2)").text shouldBe "£999.00"
        }

        "have a base tax rate of 20%" in {
          keystoreSummaryValue(TestModels.summaryIndividualFlatWithoutAEA)
          keystoreFlatCalculateValue(Some(TestModels.calcModelOneRate))
          SummaryTestDataItem.jsoupDoc.body().getElementById("calcDetails(3)").text() shouldBe "20%"
        }
      }
    }

    "regular trustee is chosen with a time apportioned calculation" when {

      "the user has provided a value for the AEA" should {
        keystoreSummaryValue(TestModels.summaryTrusteeTAWithAEA)
        keystoreTACalculateValue(Some(TestModels.calcModelOneRate))
        object SummaryTestDataItem extends fakeRequestTo("summary", TestCalculationController.summary)

        "have an election description of time apportionment method" in {
          keystoreSummaryValue(TestModels.summaryTrusteeTAWithAEA)
          keystoreTACalculateValue(Some(TestModels.calcModelOneRate))
          SummaryTestDataItem.jsoupDoc.body().getElementById("calcDetails(0)").text() shouldBe Messages("calc.summary.calculation.details.timeCalculation")
        }

        "have an acquisition date of '9 September 1990'" in{
          keystoreSummaryValue(TestModels.summaryTrusteeTAWithAEA)
          keystoreTACalculateValue(Some(TestModels.calcModelOneRate))
          SummaryTestDataItem.jsoupDoc.body().getElementById("purchaseDetails(0)").text() shouldBe ("09 September 1999")
        }

        "have a 'trustee' owner" in {
          keystoreSummaryValue(TestModels.summaryTrusteeTAWithAEA)
          keystoreTACalculateValue(Some(TestModels.calcModelOneRate))
          SummaryTestDataItem.jsoupDoc.body().getElementById("personalDetails(0)").text() shouldBe "Trustee"
        }

        "have an answer of 'No to the disabled trustee question" in {
          keystoreSummaryValue(TestModels.summaryTrusteeTAWithAEA)
          keystoreTACalculateValue(Some(TestModels.calcModelOneRate))
          SummaryTestDataItem.jsoupDoc.body().getElementById("personalDetails(1)").text() shouldBe "No"
        }

        "have a remaining CGT Allowance of £1500" in {
          keystoreSummaryValue(TestModels.summaryTrusteeTAWithAEA)
          keystoreTACalculateValue(Some(TestModels.calcModelOneRate))
          SummaryTestDataItem.jsoupDoc.body().getElementById("personalDetails(2)").text() shouldBe "£1500.00"
        }

        "have a base tax rate of 20%" in {
          keystoreSummaryValue(TestModels.summaryTrusteeTAWithAEA)
          keystoreTACalculateValue(Some(TestModels.calcModelOneRate))
          SummaryTestDataItem.jsoupDoc.body().getElementById("calcDetails(3)").text() shouldBe "20%"
        }
      }

      "the user has provided no value for the AEA" should {
        keystoreSummaryValue(TestModels.summaryTrusteeTAWithoutAEA)
        keystoreTACalculateValue(Some(TestModels.calcModelTwoRates))
        object SummaryTestDataItem extends fakeRequestTo("summary", TestCalculationController.summary)

        "have an answer of 'No to the disabled trustee question" in {
          keystoreSummaryValue(TestModels.summaryTrusteeTAWithoutAEA)
          keystoreTACalculateValue(Some(TestModels.calcModelTwoRates))
          SummaryTestDataItem.jsoupDoc.body().getElementById("personalDetails(1)").text() shouldBe "No"
        }

        "have a remaining CGT Allowance of £5050" in {
          keystoreSummaryValue(TestModels.summaryTrusteeTAWithoutAEA)
          keystoreTACalculateValue(Some(TestModels.calcModelTwoRates))
          SummaryTestDataItem.jsoupDoc.body().getElementById("personalDetails(2)").text() shouldBe "£5050.00"
        }
      }
    }

    "disabled trustee is chosen with a time apportioned calculation" when {

      "the user has provided a value for the AEA" should {
        keystoreSummaryValue(TestModels.summaryDisabledTrusteeTAWithAEA)
        keystoreTACalculateValue(Some(TestModels.calcModelTwoRates))
        object SummaryTestDataItem extends fakeRequestTo("summary", TestCalculationController.summary)

        "have an answer of 'Yes' to the disabled trustee question" in {
          keystoreSummaryValue(TestModels.summaryDisabledTrusteeTAWithAEA)
          keystoreTACalculateValue(Some(TestModels.calcModelTwoRates))
          SummaryTestDataItem.jsoupDoc.body().getElementById("personalDetails(1)").text() shouldBe "Yes"
        }

        "have a remaining CGT Allowance of £1500" in {
          keystoreSummaryValue(TestModels.summaryDisabledTrusteeTAWithAEA)
          keystoreTACalculateValue(Some(TestModels.calcModelTwoRates))
          SummaryTestDataItem.jsoupDoc.body().getElementById("personalDetails(2)").text() shouldBe "£1500.00"
        }
      }

      "the user has provided no value for the AEA" should {
        keystoreSummaryValue(TestModels.summaryDisabledTrusteeTAWithoutAEA)
        keystoreTACalculateValue(Some(TestModels.calcModelTwoRates))
        object SummaryTestDataItem extends fakeRequestTo("summary", TestCalculationController.summary)

        "have an answer of 'Yes' to the disabled trustee question" in {
          keystoreSummaryValue(TestModels.summaryDisabledTrusteeTAWithoutAEA)
          keystoreTACalculateValue(Some(TestModels.calcModelTwoRates))
          SummaryTestDataItem.jsoupDoc.body().getElementById("personalDetails(1)").text() shouldBe "Yes"
        }

        "have a remaining CGT Allowance of £11100" in {
          keystoreSummaryValue(TestModels.summaryDisabledTrusteeTAWithoutAEA)
          keystoreTACalculateValue(Some(TestModels.calcModelTwoRates))
          SummaryTestDataItem.jsoupDoc.body().getElementById("personalDetails(2)").text() shouldBe "£11100.00"
        }
      }
    }

    "personal representative is chosen with a flat calculation" when {

      "the user has provided a value for the AEA" should {
        keystoreSummaryValue(TestModels.summaryRepresentativeFlatWithAEA)
        keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
        object SummaryTestDataItem extends fakeRequestTo("summary", TestCalculationController.summary)

        "have a 'Personal Representative' owner" in {
          keystoreSummaryValue(TestModels.summaryRepresentativeFlatWithAEA)
          keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
          SummaryTestDataItem.jsoupDoc.body().getElementById("personalDetails(0)").text() shouldBe "Personal Representative"
        }

        "have a remaining CGT Allowance of £1500" in {
          keystoreSummaryValue(TestModels.summaryRepresentativeFlatWithAEA)
          keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
          SummaryTestDataItem.jsoupDoc.body().getElementById("personalDetails(1)").text() shouldBe "£1500.00"
        }
      }

      "the user has provided no value for the AEA" should {
        keystoreSummaryValue(TestModels.summaryRepresentativeFlatWithoutAEA)
        keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
        object SummaryTestDataItem extends fakeRequestTo("summary", TestCalculationController.summary)

        "have a 'Personal Representative' owner" in {
          keystoreSummaryValue(TestModels.summaryRepresentativeFlatWithoutAEA)
          keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
          SummaryTestDataItem.jsoupDoc.body().getElementById("personalDetails(0)").text() shouldBe "Personal Representative"
        }

        "have a remaining CGT Allowance of £11100" in {
          keystoreSummaryValue(TestModels.summaryRepresentativeFlatWithoutAEA)
          keystoreFlatCalculateValue(Some(TestModels.calcModelTwoRates))
          SummaryTestDataItem.jsoupDoc.body().getElementById("personalDetails(1)").text() shouldBe "£11100.00"
        }
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
          CurrentIncomeTestDataItem.jsoupDoc.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
        }

        "have the question 'In the tax year when you stopped owning the property, what was your total UK income?' as the label of the input" in {
          keystoreFetchCondition[CurrentIncomeModel](None)
          CurrentIncomeTestDataItem.jsoupDoc.body.getElementsByTag("label").text.contains(Messages("calc.currentIncome.question")) shouldBe true
        }

        "have the help text 'Tax years start on 6 April' as the form-hint of the input" in {
          keystoreFetchCondition[CurrentIncomeModel](None)
          CurrentIncomeTestDataItem.jsoupDoc.body.getElementsByClass("form-hint").text shouldEqual Messages("calc.currentIncome.helpText")
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

    "supplied with a pre-existing stored model" should {
      object CurrentIncomeTestDataItem extends fakeRequestTo("currentIncome", TestCalculationController.currentIncome)
      val testModel = new CurrentIncomeModel(1000)

      "return a 200" in {
        keystoreFetchCondition[CurrentIncomeModel](Some(testModel))
        status(CurrentIncomeTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {
        "have some value auto-filled into the input box" in {
          keystoreFetchCondition[CurrentIncomeModel](Some(testModel))
          CurrentIncomeTestDataItem.jsoupDoc.getElementById("currentIncome").attr("value") shouldBe "1000"
        }
      }
    }
  }

  "In CalculationController calling the .submitCurrentIncome action " when {

    def keystoreCacheCondition[T](data: CurrentIncomeModel): Unit = {
      lazy val returnedCacheMap = CacheMap("form-id", Map("data" -> Json.toJson(data)))
      when(mockCalcConnector.saveFormData[T](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(returnedCacheMap))
    }

    "submitting a valid form" should {
      val testModel = new CurrentIncomeModel(1000)
      object CurrentIncomeTestDataItem extends fakeRequestToPost(
        "current-income",
        TestCalculationController.submitCurrentIncome,
        ("currentIncome", "1000")
      )

      "return a 303" in {
        keystoreCacheCondition(testModel)
        status(CurrentIncomeTestDataItem.result) shouldBe 303
      }

      s"redirect to ${routes.CalculationController.personalAllowance()}" in {
        keystoreCacheCondition[CurrentIncomeModel](testModel)
        redirectLocation(CurrentIncomeTestDataItem.result) shouldBe Some(s"${routes.CalculationController.personalAllowance()}")
      }
    }

    "submitting an invalid form with no data" should {
      val testModel = new CurrentIncomeModel(0)
      object CurrentIncomeTestDataItem extends fakeRequestToPost(
        "current-income",
        TestCalculationController.submitCurrentIncome,
        ("currentIncome", "")
      )

      "return a 400" in {
        status(CurrentIncomeTestDataItem.result) shouldBe 400
      }
    }

    "submitting an invalid form with a negative value" should {
      val testModel = new CurrentIncomeModel(-1000)
      object CurrentIncomeTestDataItem extends fakeRequestToPost(
        "current-income",
        TestCalculationController.submitCurrentIncome,
        ("currentIncome", "-1000")
      )

      "return a 400" in {
        status(CurrentIncomeTestDataItem.result) shouldBe 400
      }
    }

    "submitting an invalid form with value 1.111" should {
      val testModel = new CurrentIncomeModel(1.111)
      object CurrentIncomeTestDataItem extends fakeRequestToPost(
        "current-income",
        TestCalculationController.submitCurrentIncome,
        ("currentIncome", "1.111")
      )

      "return a 400" in {
        status(CurrentIncomeTestDataItem.result) shouldBe 400
      }

      s"fail with message ${Messages("calc.currentIncome.errorDecimalPlaces")}" in {
        keystoreCacheCondition(testModel)
        CurrentIncomeTestDataItem.jsoupDoc.getElementsByClass("error-notification").text should include (Messages("calc.currentIncome.errorDecimalPlaces"))
      }
    }
  }
}
