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


class CalculationControllerSpec extends UnitSpec with WithFakeApplication with MockitoSugar with BeforeAndAfterEach{

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

  //################### Disabled Trustee tests #######################
  "In CalculationController calling the .disabledTrustee action " should {

    object DisabledTrusteeTestDataItem extends fakeRequestTo("disabled-trustee", CalculationController.disabledTrustee)

    "return a 200" in {
      status(DisabledTrusteeTestDataItem.result) shouldBe 200
    }

    "return some HTML that" should {

      "contain some text and use the character set utf-8" in {
        contentType(DisabledTrusteeTestDataItem.result) shouldBe Some("text/html")
        charset(DisabledTrusteeTestDataItem.result) shouldBe Some("utf-8")
      }

      "have the title Are you a trustee for someone who’s vulnerable?" in {
        DisabledTrusteeTestDataItem.jsoupDoc.title shouldEqual Messages("calc.disabledTrustee.question")
      }

      "have the heading Calculate your tax (non-residents) " in {
        DisabledTrusteeTestDataItem.jsoupDoc.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
      }

      "have a 'Back' link " in {
        DisabledTrusteeTestDataItem.jsoupDoc.body.getElementById("link-back").text shouldEqual Messages("calc.base.back")
      }

      "have the question 'When did you sign the contract that made someone else the owner?' as the legend of the input" in {
        DisabledTrusteeTestDataItem.jsoupDoc.body.getElementsByTag("legend").text shouldEqual Messages("calc.disabledTrustee.question")
      }

      "display a radio button with the option 'Yes'" in {
        DisabledTrusteeTestDataItem.jsoupDoc.body.getElementById("isVulnerableYes").parent.text shouldEqual Messages("calc.base.yes")
      }

      "display a radio button with the option 'No'" in {
        DisabledTrusteeTestDataItem.jsoupDoc.body.getElementById("isVulnerableNo").parent.text shouldEqual Messages("calc.base.no")
      }

      "display a 'Continue' button " in {
        DisabledTrusteeTestDataItem.jsoupDoc.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
      }
    }

    //################### Current Income tests #######################
    "In CalculationController calling the .currentIncome action " should {

      "be Action(parser=BodyParser(anyContent)) for currentIncome" in {
        val result = CalculationController.currentIncome.toString()
        result shouldBe s
      }
    }

    //############## Personal Allowance tests ######################
    "In CalculationController calling the .personalAllowance action " should {

      object PersonalAllowanceTestDataItem extends fakeRequestTo("personal-allowance", CalculationController.personalAllowance)

      "return a 200" in {
        status(PersonalAllowanceTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          contentType(PersonalAllowanceTestDataItem.result) shouldBe Some("text/html")
          charset(PersonalAllowanceTestDataItem.result) shouldBe Some("utf-8")
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
            OtherPropertiesTestDataItem.jsoupDoc.body.getElementById("otherProperties-yes").parent.classNames().contains("selected") shouldBe true          }
        }
      }

    }

    //############## Annual Exempt Amount tests ######################
    "In CalculationController calling the .annualExemptAmount action " should {

      object AnnualExemptAmountTestDataItem extends fakeRequestTo("allowance", CalculationController.annualExemptAmount)

      "return a 200" in {
        status(AnnualExemptAmountTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          contentType(AnnualExemptAmountTestDataItem.result) shouldBe Some("text/html")
          charset(AnnualExemptAmountTestDataItem.result) shouldBe Some("utf-8")
        }

        "have the title 'How much of your Capital Gains Tax allowance have you got left?'" in {
          AnnualExemptAmountTestDataItem.jsoupDoc.title shouldEqual Messages("calc.annualExemptAmount.question")
        }

        "have the heading Calculate your tax (non-residents) " in {
          AnnualExemptAmountTestDataItem.jsoupDoc.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a 'Back' link " in {
          AnnualExemptAmountTestDataItem.jsoupDoc.body.getElementById("link-back").text shouldEqual Messages("calc.base.back")
        }

        "have the question 'How much of your Capital Gains Tax allowance have you got left?' as the legend of the input" in {
          AnnualExemptAmountTestDataItem.jsoupDoc.body.getElementsByTag("label").text shouldEqual Messages("calc.annualExemptAmount.question")
        }

        "display an input box for the Annual Exempt Amount" in {
          AnnualExemptAmountTestDataItem.jsoupDoc.body.getElementById("annualExemptAmount").tagName() shouldEqual "input"
        }

        "display a 'Continue' button " in {
          AnnualExemptAmountTestDataItem.jsoupDoc.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
        }

        "should contain a Read more sidebar with a link to CGT allowances" in {
          AnnualExemptAmountTestDataItem.jsoupDoc.select("aside h2").text shouldBe Messages("calc.common.readMore")
          AnnualExemptAmountTestDataItem.jsoupDoc.select("aside a").text shouldBe Messages("calc.annualExemptAmount.link.one")
        }
      }
    }

    //############## Acquisition Value tests ######################
    "In CalculationController calling the .acquisitionValue action " should {

      object AcquisitonValueTestDataItem extends fakeRequestTo("acquisition-value", CalculationController.acquisitionValue)

      "return a 200" in {
        status(AcquisitonValueTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          contentType(AcquisitonValueTestDataItem.result) shouldBe Some("text/html")
          charset(AcquisitonValueTestDataItem.result) shouldBe Some("utf-8")
        }

        "have the title 'How much did you pay for the property?'" in {
          AcquisitonValueTestDataItem.jsoupDoc.title shouldEqual Messages("calc.acquisitionValue.question")
        }

        "have the heading Calculate your tax (non-residents) " in {
          AcquisitonValueTestDataItem.jsoupDoc.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a 'Back' link " in {
          AcquisitonValueTestDataItem.jsoupDoc.body.getElementById("link-back").text shouldEqual Messages("calc.base.back")
        }

        "have the question 'How much did you pay for the property?'" in {
          AcquisitonValueTestDataItem.jsoupDoc.body.getElementsByTag("label").text shouldEqual Messages("calc.acquisitionValue.question")
        }

        "display an input box for the Acquisition Value" in {
          AcquisitonValueTestDataItem.jsoupDoc.body.getElementById("acquisitionValue").tagName shouldEqual "input"
        }
        "display a 'Continue' button " in {
          AcquisitonValueTestDataItem.jsoupDoc.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
        }
      }
    }

    //################### Improvements tests #######################
    "In CalculationController calling the .improvements action " should {

      object ImprovementsTestDataItem extends fakeRequestTo("improvements", CalculationController.improvements)

      "return a 200" in {
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
          ImprovementsTestDataItem.jsoupDoc.body.getElementById("improvementsCheckYes").parent.text shouldEqual Messages("calc.base.yes")
        }

        "display the correct wording for radio option `no`" in{
          ImprovementsTestDataItem.jsoupDoc.body.getElementById("improvementsCheckNo").parent.text shouldEqual Messages("calc.base.no")
        }

        "contain a hidden component with an input box" in {
          ImprovementsTestDataItem.jsoupDoc.body.getElementById("improvements").parent.parent.id shouldBe "hidden"
        }
      }
    }


    //################### Disposal Date tests #######################
    "In CalculationController calling the .disposalDate action " should {

      object DisposalDateTestDataItem extends fakeRequestTo("disposal-date", CalculationController.disposalDate)

      "return a 200" in {
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

        "display a 'Continue' button " in {
          DisposalDateTestDataItem.jsoupDoc.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
        }
      }
    }

    //################### Disposal Value tests #######################
    "In CalculationController calling the .disposalValue action " should {

      object DisposalValueTestDataItem extends fakeRequestTo("disposal-value", CalculationController.disposalValue)

      "return a 200" in {
        status(DisposalValueTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          contentType(DisposalValueTestDataItem.result) shouldBe Some("text/html")
          charset(DisposalValueTestDataItem.result) shouldBe Some("utf-8")
        }

        "have the title 'How much did you sell or give away the property for?'" in {
          DisposalValueTestDataItem.jsoupDoc.title shouldEqual Messages("calc.disposalValue.question")
        }

        "have the heading Calculate your tax (non-residents) " in {
          DisposalValueTestDataItem.jsoupDoc.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a 'Back' link " in {
          DisposalValueTestDataItem.jsoupDoc.body.getElementById("link-back").text shouldEqual Messages("calc.base.back")
        }

        "have the question 'How much did you sell or give away the property for?' as the legend of the input" in {
          DisposalValueTestDataItem.jsoupDoc.body.getElementsByTag("label").text shouldEqual Messages("calc.disposalValue.question")
        }

        "display an input box for the Annual Exempt Amount" in {
          DisposalValueTestDataItem.jsoupDoc.body.getElementById("disposalValue").tagName() shouldEqual "input"
        }

        "display a 'Continue' button " in {
          DisposalValueTestDataItem.jsoupDoc.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
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
            AcquisitionCostsTestDataItem.jsoupDoc.select("label[for=acquisitionCosts]").text shouldEqual Messages("calc.acquisitionCosts.question")
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

    //################### Disposal Costs tests #######################
    "In CalculationController calling the .disposalCosts action " should {

      object DisposalCostsTestDataItem extends fakeRequestTo("disposal-costs", CalculationController.disposalCosts)

      "return a 200" in {
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

    //################### Entrepreneurs Relief tests #######################
    "In CalculationController calling the .entrepreneursRelief action " should {

      object EntrepreneursReliefTestDataItem extends fakeRequestTo("entrepreneurs-relief", CalculationController.entrepreneursRelief)

      "return a 200" in {
        status(EntrepreneursReliefTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          contentType(EntrepreneursReliefTestDataItem.result) shouldBe Some("text/html")
          charset(EntrepreneursReliefTestDataItem.result) shouldBe Some("utf-8")
        }

        "have the title 'Are you claiming Entrepreneurs Relief?'" in {
          EntrepreneursReliefTestDataItem.jsoupDoc.title shouldEqual Messages("calc.entrepreneursRelief.question")
        }

        "have the heading Calculate your tax (non-residents) " in {
          EntrepreneursReliefTestDataItem.jsoupDoc.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a 'Back' link " in {
          EntrepreneursReliefTestDataItem.jsoupDoc.body.getElementById("link-back").text shouldEqual Messages("calc.base.back")
        }

        "have the question 'Are you claiming Entrepreneurs Relief?' as the legend of the input" in {
          EntrepreneursReliefTestDataItem.jsoupDoc.body.getElementsByTag("legend").text shouldEqual Messages("calc.entrepreneursRelief.question")
        }

        "display a 'Continue' button " in {
          EntrepreneursReliefTestDataItem.jsoupDoc.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
        }

        "have a sidebar with additional links" in {
          EntrepreneursReliefTestDataItem.jsoupDoc.body.getElementsByClass("sidebar")
        }

      }
    }


    //################### Allowable Losses tests #######################
    "In CalculationController calling the .allowableLosses action " should {

      object AllowableLossesTestDataItem extends fakeRequestTo("allowable-losses", CalculationController.allowableLosses)

      "return a 200" in {
        status(AllowableLossesTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          contentType(AllowableLossesTestDataItem.result) shouldBe Some("text/html")
          charset(AllowableLossesTestDataItem.result) shouldBe Some("utf-8")
        }

        "have a back button" in {
          AllowableLossesTestDataItem.jsoupDoc.body.getElementById("link-back").text shouldEqual Messages("calc.base.back")
        }

        "have the title 'Are you claiming any allowable losses?'" in {
          AllowableLossesTestDataItem.jsoupDoc.title shouldEqual Messages("calc.allowableLosses.question.one")
        }

        "have the heading 'Calculate your tax (non-residents)'" in {
          AllowableLossesTestDataItem.jsoupDoc.body.getElementsByTag("H1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a yes no helper with hidden content and question 'Are you claiming any allowable losses?'" in {
          AllowableLossesTestDataItem.jsoupDoc.body.getElementById("allowableLossesYes").parent.text shouldBe Messages("calc.base.yes")
          AllowableLossesTestDataItem.jsoupDoc.body.getElementById("allowableLossesNo").parent.text shouldBe Messages("calc.base.no")
          AllowableLossesTestDataItem.jsoupDoc.body.getElementsByTag("legend").text shouldBe Messages("calc.allowableLosses.question.one")
        }

        "have a hidden monetary input with question 'Whats the total value of your allowable losses?'" in {
          AllowableLossesTestDataItem.jsoupDoc.body.getElementById("allowableLosses").tagName shouldEqual "input"
          AllowableLossesTestDataItem.jsoupDoc.select("label[for=allowableLosses]").text shouldEqual Messages("calc.allowableLosses.question.two")
        }

        "have a hidden help text section with summary 'What are allowable losses?' and correct content" in {
          AllowableLossesTestDataItem.jsoupDoc.select("div#allowableLossesHiddenHelp").text should
            include(Messages("calc.allowableLosses.helpText.title"))
            include(Messages("calc.allowableLosses.helpText.paragraph.one"))
            include(Messages("calc.allowableLosses.helpText.bullet.one"))
            include(Messages("calc.allowableLosses.helpText.bullet.two"))
            include(Messages("calc.allowableLosses.helpText.bullet.three"))
        }

        "has a Continue button" in {
          AllowableLossesTestDataItem.jsoupDoc.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
        }
      }
    }


    //################### Other Reliefs tests #######################
    "In CalculationController calling the .otherReliefs action " should {

      object OtherReliefsTestDataItem extends fakeRequestTo("other-reliefs", CalculationController.otherReliefs)

      "return a 200" in {
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
      }
    }
  }
}