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

import common.DefaultRoutes._
import common.{KeystoreKeys, TestModels}
import connectors.CalculatorConnector
import constructors.CalculationElectionConstructor
import controllers.{routes, CalculationController}
import models.{AcquisitionDateModel, RebasedValueModel, CalculationResultModel, SummaryModel}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.FakeRequest
import uk.gov.hmrc.play.http.{SessionKeys, HeaderCarrier}
import uk.gov.hmrc.play.test.{WithFakeApplication, UnitSpec}

import scala.concurrent.Future

class SummarySpec extends UnitSpec with WithFakeApplication with MockitoSugar {

  implicit val hc = new HeaderCarrier()
  def setupTarget(
                   summary: SummaryModel,
                   result: CalculationResultModel,
                   acquisitionDateData: Option[AcquisitionDateModel],
                   rebasedValueData: Option[RebasedValueModel]
                 ): CalculationController = {

    val mockCalcConnector = mock[CalculatorConnector]
    val mockCalcElectionConstructor = mock[CalculationElectionConstructor]

    when(mockCalcConnector.fetchAndGetFormData[RebasedValueModel](Matchers.eq(KeystoreKeys.rebasedValue))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(rebasedValueData))

    when(mockCalcConnector.fetchAndGetFormData[AcquisitionDateModel](Matchers.eq(KeystoreKeys.acquisitionDate))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(acquisitionDateData))

    when(mockCalcConnector.createSummary(Matchers.any()))
      .thenReturn(Future.successful(summary))

    when(mockCalcConnector.calculateFlat(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(Some(result)))

    when(mockCalcConnector.calculateTA(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(Some(result)))

    when(mockCalcConnector.calculateRebased(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(Some(result)))

    new CalculationController {
      override val calcConnector: CalculatorConnector = mockCalcConnector
      override val calcElectionConstructor: CalculationElectionConstructor = mockCalcElectionConstructor
    }
  }

  "In CalculationController calling the .summary action" when {
    lazy val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/summary").withSession(SessionKeys.sessionId -> "12345")

    "Testing the back links for all user types" when {

      "Acquisition Date is > 5 April 2015" should {
        val target = setupTarget(
          TestModels.summaryIndividualFlatWithAEA,
          TestModels.calcModelTwoRates,
          Some(AcquisitionDateModel("Yes", Some(1), Some(1), Some(2017))),
          None
        )
        lazy val result = target.summary()(fakeRequest)
        lazy val document = Jsoup.parse(bodyOf(result))

        s"have a 'Back' link to ${routes.CalculationController.otherReliefs().url}" in {
          document.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
          document.body.getElementById("back-link").attr("href") shouldEqual routes.CalculationController.otherReliefs().url
        }
      }

      "Acquisition Date is not supplied and no rebased value has been supplied" should {
        val target = setupTarget(
          TestModels.summaryIndividualFlatWithAEA,
          TestModels.calcModelTwoRates,
          Some(AcquisitionDateModel("No", None,None,None)),
          Some(RebasedValueModel("No", None))
        )
        lazy val result = target.summary()(fakeRequest)
        lazy val document = Jsoup.parse(bodyOf(result))

        s"have a 'Back' link to ${routes.CalculationController.otherReliefs().url}" in {
          document.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
          document.body.getElementById("back-link").attr("href") shouldEqual routes.CalculationController.otherReliefs().url
        }
      }

      "Acquisition Date is not supplied and rebased value is supplied" should {
        val target = setupTarget(
          TestModels.summaryIndividualFlatWithAEA,
          TestModels.calcModelTwoRates,
          Some(AcquisitionDateModel("No", None,None,None)),
          Some(RebasedValueModel("Yes", Some(500)))
        )
        lazy val result = target.summary()(fakeRequest)
        lazy val document = Jsoup.parse(bodyOf(result))

        s"have a 'Back' link to ${routes.CalculationController.calculationElection().url}" in {
          document.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
          document.body.getElementById("back-link").attr("href") shouldEqual routes.CalculationController.calculationElection().url
        }
      }

      "Acquisition Date <= 5 April 2015" should {
        val target = setupTarget(
          TestModels.summaryIndividualFlatWithAEA,
          TestModels.calcModelTwoRates,
          Some(AcquisitionDateModel("Yes", Some(1),Some(1),Some(2014))),
          Some(RebasedValueModel("Yes", Some(500)))
        )
        lazy val result = target.summary()(fakeRequest)
        lazy val document = Jsoup.parse(bodyOf(result))

        s"have a 'Back' link to ${routes.CalculationController.calculationElection().url}" in {
          document.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
          document.body.getElementById("back-link").attr("href") shouldEqual routes.CalculationController.calculationElection().url
        }
      }

      "Acquisition Date Model is not supplied" should {
        val target = setupTarget(
          TestModels.summaryIndividualFlatWithAEA,
          TestModels.calcModelTwoRates,
          None,
          Some(RebasedValueModel("Yes", Some(500)))
        )
        lazy val result = target.summary()(fakeRequest)
        lazy val document = Jsoup.parse(bodyOf(result))

        s"have a 'Back' link to ${missingDataRoute} " in {
          document.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
          document.body.getElementById("back-link").attr("href") shouldEqual missingDataRoute
        }
      }

      "Acquisition Date Model is supplied with no date but Rebased Value Model is not" should {
        val target = setupTarget(
          TestModels.summaryIndividualFlatWithAEA,
          TestModels.calcModelTwoRates,
          Some(AcquisitionDateModel("No", None,None,None)),
          None
        )
        lazy val result = target.summary()(fakeRequest)
        lazy val document = Jsoup.parse(bodyOf(result))

        s"have a 'Back' link to ${missingDataRoute} " in {
          document.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
          document.body.getElementById("back-link").attr("href") shouldEqual missingDataRoute
        }
      }
    }

    "individual is chosen with a flat calculation" when {

      "the user has provided a value for the AEA" should {
        val target = setupTarget(
          TestModels.summaryIndividualFlatWithAEA,
          TestModels.calcModelTwoRates,
          Some(AcquisitionDateModel("Yes", Some(1), Some(1), Some(2017))),
          None
        )
        lazy val result = target.summary()(fakeRequest)
        lazy val document = Jsoup.parse(bodyOf(result))

        "return a 200" in {
          status(result) shouldBe 200
        }

        "return some HTML that" should {

          "should have the title 'Summary'" in {
            document.getElementsByTag("title").text shouldEqual Messages("calc.summary.title")
          }

          "have a back button" in {
            document.getElementById("back-link").text shouldEqual Messages("calc.base.back")
          }

          "have the correct sub-heading 'You owe'" in {
            document.select("h1 span").text shouldEqual Messages("calc.summary.secondaryHeading")
          }

          "have a result amount currently set to £8000.00" in {
            document.select("h1 b").text shouldEqual "£8000.00"
          }

          "have a 'Calculation details' section that" should {

            "include the section heading 'Calculation details" in {
              document.select("#calcDetails").text should include(Messages("calc.summary.calculation.details.title"))
            }

            "include 'How would you like to work out your tax?'" in {
              document.select("#calcDetails").text should include(Messages("calc.summary.calculation.details.calculationElection"))
            }

            "have an election description of 'How much of your total gain you've made since 5 April 2015'" in {
              document.body().getElementById("calcDetails(0)").text() shouldBe Messages("calc.summary.calculation.details.flatCalculation")
            }

            "include 'Your total gain'" in {
              document.select("#calcDetails").text should include(Messages("calc.summary.calculation.details.totalGain"))
            }

            "have a total gain equal to £40000.00" in {
              document.body().getElementById("calcDetails(1)").text() shouldBe "£40000.00"
            }

            "include 'Your taxable gain'" in {
              document.select("#calcDetails").text should include(Messages("calc.summary.calculation.details.taxableGain"))
            }

            "have a taxable gain equal to £40000.00" in {
              document.body().getElementById("calcDetails(2)").text() shouldBe "£40000.00"
            }

            "include 'Your tax rate'" in {
              document.select("#calcDetails").text should include(Messages("calc.summary.calculation.details.taxRate"))
            }

            "have a base tax rate of £32000" in {
              document.body().getElementById("calcDetails(3)").text() shouldBe "£32000.00 at 18%"
            }

            "have an upper tax rate of £8000" in {
              document.body().getElementById("calcDetails(4)").text() shouldBe "£8000.00 at 28%"
            }

          }

          "have a 'Personal details' section that" should {

            "include the section heading 'Personal details" in {
              document.select("#personalDetails").text should include(Messages("calc.summary.personal.details.title"))
            }

            "include the question 'Who owned the property?'" in {
              document.select("#personalDetails").text should include(Messages("calc.customerType.question"))
            }

            "have an 'individual' owner and link to the customer-type page" in {
              document.body().getElementById("personalDetails(0)").text() shouldBe "Individual"
              document.body().getElementById("personalDetails(0)").attr("href") shouldEqual routes.CalculationController.customerType().toString()
            }

            "include the question 'What’s your total income for this tax year?'" in {
              document.select("#personalDetails").text should include(Messages("calc.currentIncome.question"))
            }

            "have an total income of £1000 and link to the current-income screen" in {
              document.body().getElementById("personalDetails(1)").text() shouldBe "£1000.00"
              document.body().getElementById("personalDetails(1)").attr("href") shouldEqual routes.CalculationController.currentIncome().toString()
            }

            "include the question 'What's your Personal Allowance for this tax year?'" in {
              document.select("#personalDetails").text should include(Messages("calc.personalAllowance.question"))
            }

            "have a personal allowance of £9000 that has a link to the personal allowance page." in {
              document.body().getElementById("personalDetails(2)").text() shouldBe "£9000.00"
              document.body().getElementById("personalDetails(2)").attr("href") shouldEqual routes.CalculationController.personalAllowance().toString()
            }

            "include the question 'What was the total taxable gain of your previous Capital Gains in the tax year you stopped owning the property?'" in {
              document.select("#personalDetails").text should include(Messages("calc.otherProperties.questionTwo"))
            }

            "have a total taxable gain of prior disposals of £9600 and link to the other-properties page" in {
              document.body().getElementById("personalDetails(3)").text() shouldBe "£9600.00"
              document.body().getElementById("personalDetails(3)").attr("href") shouldEqual routes.CalculationController.otherProperties().toString()
            }

            "include the question 'How much of your Capital Gains Tax allowance have you got left'" in {
              document.select("#personalDetails").text should include(Messages("calc.annualExemptAmount.question"))
            }

            "have a remaining CGT Allowance of £1500 and link to the allowance page" in {
              document.body().getElementById("personalDetails(4)").text() shouldBe "£1500.00"
              document.body().getElementById("personalDetails(4)").attr("href") shouldEqual routes.CalculationController.annualExemptAmount().toString()
            }
          }

          "have a 'Purchase details' section that" should {

            "include the section heading 'Purchase details" in {
              document.select("#purchaseDetails").text should include(Messages("calc.summary.purchase.details.title"))
            }

            "include the question 'How much did you pay for the property?'" in {
              document.select("#purchaseDetails").text should include(Messages("calc.acquisitionValue.question"))
            }

            "have an acquisition value of £100000 and link to the acquisition value page" in {
              document.body().getElementById("purchaseDetails(0)").text() shouldBe "£100000.00"
              document.body().getElementById("purchaseDetails(0)").attr("href") shouldEqual routes.CalculationController.acquisitionValue().toString()
            }

            "include the question 'How much did you pay in costs when you became the property owner?'" in {
              document.select("#purchaseDetails").text should include(Messages("calc.acquisitionCosts.question"))
            }

            "have a acquisition costs of £0 and link to the acquisition-costs page" in {
              document.body().getElementById("purchaseDetails(1)").text() shouldBe "£0.00"
              document.body().getElementById("purchaseDetails(1)").attr("href") shouldEqual routes.CalculationController.acquisitionCosts().toString()
            }
          }

          "have a 'Property details' section that" should {

            "include the section heading 'Property details" in {
              document.select("#propertyDetails").text should include(Messages("calc.summary.property.details.title"))
            }

            "include the question 'Did you make any improvements to the property?'" in {
              document.select("#propertyDetails").text should include(Messages("calc.improvements.question"))
            }

            "the answer to the improvements question should be No and should link to the improvements page" in {
              document.body.getElementById("propertyDetails(0)").text shouldBe "No"
              document.body().getElementById("propertyDetails(0)").attr("href") shouldEqual routes.CalculationController.improvements().toString()
            }
          }

          "have a 'Sale details' section that" should {

            "include the section heading 'Sale details" in {
              document.select("#saleDetails").text should include(Messages("calc.summary.sale.details.title"))
            }

            "include the question 'When did you sign the contract that made someone else the owner?'" in {
              document.select("#saleDetails").text should include(Messages("calc.disposalDate.question"))
            }

            "the date of disposal should be '10 October 2010 and link to the disposal-date page" in {
              document.body().getElementById("saleDetails(0)").text shouldBe "10 October 2010"
              document.body().getElementById("saleDetails(0)").attr("href") shouldEqual routes.CalculationController.disposalDate().toString()
            }

            "include the question 'How much did you sell or give away the property for?'" in {
              document.select("#saleDetails").text should include(Messages("calc.disposalValue.question"))
            }

            "the value of the sale should be £150000 and link to the disposal-value page" in {
              document.body().getElementById("saleDetails(1)").text shouldBe "£150000.00"
              document.body().getElementById("saleDetails(1)").attr("href") shouldEqual routes.CalculationController.disposalValue().toString()
            }

            "include the question 'How much did you pay in costs when you stopped being the property owner?'" in {
              document.select("#saleDetails").text should include(Messages("calc.disposalCosts.question"))
            }

            "the value of the costs should be £0 and link to the disposal costs page" in {
              document.body().getElementById("saleDetails(2)").text shouldBe "£0.00"
              document.body().getElementById("saleDetails(2)").attr("href") shouldEqual routes.CalculationController.disposalCosts().toString()
            }
          }

          "have a 'Deductions details' section that" should {

            "include the section heading 'Deductions" in {
              document.select("#deductions").text should include(Messages("calc.summary.deductions.title"))
            }

            "include the question 'Are you claiming Entrepreneurs' Relief?'" in {
              document.select("#deductions").text should include(Messages("calc.entrepreneursRelief.question"))
            }

            "have the answer to entrepreneurs relief question be 'No' and link to the entrepreurs-relief page" in {
              document.body().getElementById("deductions(0)").text shouldBe "No"
              document.body().getElementById("deductions(0)").attr("href") shouldEqual routes.CalculationController.entrepreneursRelief().toString()
            }

            "include the question 'Whats the total value of your allowable losses?'" in {
              document.select("#deductions").text should include(Messages("calc.allowableLosses.question.two"))
            }

            "the value of allowable losses should be £0 and link to the allowable-losses page" in {
              document.body().getElementById("deductions(1)").text shouldBe "£0.00"
              document.body().getElementById("deductions(1)").attr("href") shouldEqual routes.CalculationController.allowableLosses().toString()
            }

            "include the question 'What other reliefs are you claiming?'" in {
              document.select("#deductions").text should include(Messages("calc.otherReliefs.question"))
            }

            "the value of other reliefs should be £0 and link to the other-reliefs page" in {
              document.body().getElementById("deductions(2)").text shouldBe "£0.00"
              document.body().getElementById("deductions(2)").attr("href") shouldEqual routes.CalculationController.otherReliefs().toString()
            }

          }

          "have a 'What to do next' section that" should {

            "have the heading 'What to do next'" in {
              document.select("#whatToDoNext H2").text shouldEqual Messages("calc.common.next.actions.heading")
            }

            "include the text 'You need to tell HMRC about the property'" in {
              document.select("#whatToDoNext").text should
                include(Messages("calc.summary.next.actions.text"))
              include(Messages("calc.summary.next.actions.link"))
            }
          }

          "have a link to 'Start again'" in {
            document.select("#startAgain").text shouldEqual Messages("calc.summary.startAgain")
          }
        }
      }

      "the user has provided no value for the AEA" should {
        val target = setupTarget(
          TestModels.summaryIndividualFlatWithoutAEA,
          TestModels.calcModelOneRate,
          Some(AcquisitionDateModel("Yes", Some(1), Some(1), Some(2017))),
          None
        )
        lazy val result = target.summary()(fakeRequest)
        lazy val document = Jsoup.parse(bodyOf(result))

        "have a remaining CGT Allowance of £11100" in {
          document.body().getElementById("personalDetails(3)").text() shouldBe "£11100.00"
        }

        "the answer to the improvements question should be Yes" in {
          document.body.getElementById("propertyDetails(0)").text shouldBe "Yes"
        }

        "the value of the improvements should be £8000" in {
          document.body.getElementById("propertyDetails(1)").text shouldBe "£8000.00"
        }

        "the value of the disposal costs should be £600" in {
          document.body().getElementById("saleDetails(2)").text shouldBe "£600.00"
        }

        "have a acquisition costs of £300" in {
          document.body().getElementById("purchaseDetails(1)").text() shouldBe "£300.00"
        }

        "the value of allowable losses should be £50000" in {
          document.body().getElementById("deductions(1)").text shouldBe "£50000.00"
        }

        "the value of other reliefs should be £999" in {
          document.body().getElementById("deductions(2)").text shouldBe "£999.00"
        }

        "have a base tax rate of 20%" in {
          document.body().getElementById("calcDetails(3)").text() shouldBe "20%"
        }
      }

      "users calculation results in a loss" should {
        val target = setupTarget(
          TestModels.summaryIndividualFlatLoss,
          TestModels.calcModelLoss,
          Some(AcquisitionDateModel("Yes", Some(1), Some(1), Some(2017))),
          None
        )
        lazy val result = target.summary()(fakeRequest)
        lazy val document = Jsoup.parse(bodyOf(result))

        s"have ${Messages("calc.summary.calculation.details.totalLoss")} output" in {
          document.body.getElementById("calcDetails").text() should include (Messages("calc.summary.calculation.details.totalLoss"))
        }

        s"have £10000.00 loss" in {
          document.body.getElementById("calcDetails(1)").text() shouldBe "£10000.00"
        }
      }
    }

    "regular trustee is chosen with a time apportioned calculation" when {

      "the user has provided a value for the AEA" should {
        val target = setupTarget(
          TestModels.summaryTrusteeTAWithAEA,
          TestModels.calcModelOneRate,
          Some(AcquisitionDateModel("Yes", Some(1), Some(1), Some(2017))),
          None
        )
        lazy val result = target.summary()(fakeRequest)
        lazy val document = Jsoup.parse(bodyOf(result))

        "have an election description of time apportionment method" in {
          document.body().getElementById("calcDetails(0)").text() shouldBe Messages("calc.summary.calculation.details.timeCalculation")
        }

        "have an acquisition date of '9 September 1990'" in{
          document.body().getElementById("purchaseDetails(0)").text() shouldBe "09 September 1999"
        }

        "have a 'trustee' owner" in {
          document.body().getElementById("personalDetails(0)").text() shouldBe "Trustee"
        }

        "have an answer of 'No to the disabled trustee question" in {
          document.body().getElementById("personalDetails(1)").text() shouldBe "No"
        }

        "have a total taxable gain of prior disposals of £9600" in {
          document.body.getElementById("personalDetails(2)").text() shouldBe "£9600.00"
        }

        "have a remaining CGT Allowance of £1500" in {
          document.body().getElementById("personalDetails(3)").text() shouldBe "£1500.00"
        }

        "have a base tax rate of 20%" in {
          document.body().getElementById("calcDetails(3)").text() shouldBe "20%"
        }
      }

      "the user has provided no value for the AEA" should {
        val target = setupTarget(
          TestModels.summaryTrusteeTAWithoutAEA,
          TestModels.calcModelTwoRates,
          Some(AcquisitionDateModel("Yes", Some(1), Some(1), Some(2017))),
          None
        )
        lazy val result = target.summary()(fakeRequest)
        lazy val document = Jsoup.parse(bodyOf(result))

        "have an answer of 'No to the disabled trustee question" in {
         document.getElementById("personalDetails(1)").text() shouldBe "No"
        }

        "have a remaining CGT Allowance of £5050" in {
         document.getElementById("personalDetails(2)").text() shouldBe "£5050.00"
        }
      }
    }

    "disabled trustee is chosen with a time apportioned calculation" when {

      "the user has provided a value for the AEA" should {
        val target = setupTarget(
          TestModels.summaryDisabledTrusteeTAWithAEA,
          TestModels.calcModelTwoRates,
          Some(AcquisitionDateModel("Yes", Some(1), Some(1), Some(2017))),
          None
        )
        lazy val result = target.summary()(fakeRequest)
        lazy val document = Jsoup.parse(bodyOf(result))

        "have an answer of 'Yes' to the disabled trustee question" in {
          document.body().getElementById("personalDetails(1)").text() shouldBe "Yes"
        }

        "have a remaining CGT Allowance of £1500" in {
          document.body().getElementById("personalDetails(3)").text() shouldBe "£1500.00"
        }
      }

      "the user has provided no value for the AEA" should {
        val target = setupTarget(
          TestModels.summaryDisabledTrusteeTAWithoutAEA,
          TestModels.calcModelTwoRates,
          Some(AcquisitionDateModel("Yes", Some(1), Some(1), Some(2017))),
          None
        )
        lazy val result = target.summary()(fakeRequest)
        lazy val document = Jsoup.parse(bodyOf(result))

        "have an answer of 'Yes' to the disabled trustee question" in {
          document.body().getElementById("personalDetails(1)").text() shouldBe "Yes"
        }

        "have a remaining CGT Allowance of £11100" in {
          document.body().getElementById("personalDetails(2)").text() shouldBe "£11100.00"
        }
      }
    }

    "personal representative is chosen with a flat calculation" when {

      "the user has provided a value for the AEA" should {
        val target = setupTarget(
          TestModels.summaryRepresentativeFlatWithAEA,
          TestModels.calcModelTwoRates,
          Some(AcquisitionDateModel("Yes", Some(1), Some(1), Some(2017))),
          None
        )
        lazy val result = target.summary()(fakeRequest)
        lazy val document = Jsoup.parse(bodyOf(result))

        "have a 'Personal Representative' owner" in {
          document.body().getElementById("personalDetails(0)").text() shouldBe "Personal Representative"
        }

        "have a total taxable gain of prior disposals of £9600" in {
          document.body.getElementById("personalDetails(1)").text() shouldBe "£9600.00"
        }

        "have a remaining CGT Allowance of £1500" in {
          document.body().getElementById("personalDetails(2)").text() shouldBe "£1500.00"
        }
      }

      "the user has provided no value for the AEA" should {
        val target = setupTarget(
          TestModels.summaryRepresentativeFlatWithoutAEA,
          TestModels.calcModelTwoRates,
          Some(AcquisitionDateModel("Yes", Some(1), Some(1), Some(2017))),
          None
        )
        lazy val result = target.summary()(fakeRequest)
        lazy val document = Jsoup.parse(bodyOf(result))

        "have a 'Personal Representative' owner" in {
          document.body().getElementById("personalDetails(0)").text() shouldBe "Personal Representative"
        }

        "have a remaining CGT Allowance of £11100" in {
          document.body().getElementById("personalDetails(1)").text() shouldBe "£11100.00"
        }
      }

    }
    "individual is chosen with a rebased calculation" when {

      "user provides no acquisition date and has two tax rates" should {
        val target = setupTarget(
          TestModels.summaryIndividualRebased,
          TestModels.calcModelTwoRates,
          Some(AcquisitionDateModel("Yes", Some(1), Some(1), Some(2017))),
          None
        )
        lazy val result = target.summary()(fakeRequest)
        lazy val document = Jsoup.parse(bodyOf(result))

        "have an election description of 'How much of your total gain you've made since 5 April 2015'" in {
          document.body().getElementById("calcDetails(0)").text() shouldBe Messages("calc.summary.calculation.details.rebasedCalculation")
        }

        "include the question for the rebased value" in {
          document.select("#purchaseDetails").text should include(Messages("calc.rebasedValue.questionTwo"))
        }

        "have a value for the rebased value" in {
          document.body.getElementById("purchaseDetails(1)").text() shouldBe "£150000.00"
        }

        "include the question for the rebased costs" in {
          document.select("#purchaseDetails").text should include(Messages("calc.rebasedCosts.questionTwo"))
        }

        "have a value for the rebased costs" in {
          document.body.getElementById("purchaseDetails(2)").text() shouldBe "£1000.00"
        }

        "include the question for the improvements before" in {
          document.select("#propertyDetails").text should include(Messages("calc.improvements.questionThree"))
        }

        "have a value for the improvements before" in {
          document.body.getElementById("propertyDetails(1)").text() shouldBe "£2000.00"
        }

        "include the question for the improvements after" in {
          document.select("#propertyDetails").text should include(Messages("calc.improvements.questionFour"))
        }

        "have a value for the improvements after" in {
          document.body.getElementById("propertyDetails(2)").text() shouldBe "£3000.00"
        }

        "have a value for the other reliefs rebased" in {
          document.body.getElementById("deductions(2)").text() shouldBe "£777.00"
          document.body().getElementById("deductions(2)").attr("href") shouldEqual routes.CalculationController.otherReliefsRebased().toString()
        }

      }

      "user provides no acquisition date and has one tax rate" should {
        val target = setupTarget(
          TestModels.summaryIndividualRebasedNoAcqDate,
          TestModels.calcModelOneRate,
          Some(AcquisitionDateModel("Yes", Some(1), Some(1), Some(2017))),
          None
        )
        lazy val result = target.summary()(fakeRequest)
        lazy val document = Jsoup.parse(bodyOf(result))

        "have an election description of 'How much of your total gain you've made since 5 April 2015'" in {
          document.body().getElementById("calcDetails(0)").text() shouldBe Messages("calc.summary.calculation.details.rebasedCalculation")
        }

        "the value of allowable losses should be £0" in {
          document.body().getElementById("deductions(1)").text shouldBe "£0.00"
        }

        "the value of other reliefs should be £0" in {
          document.body().getElementById("deductions(2)").text shouldBe "£0.00"
        }
      }

      "user provides acquisition date and no rebased costs" should {
        val target = setupTarget(
          TestModels.summaryIndividualRebasedNoRebasedCosts,
          TestModels.calcModelOneRate,
          Some(AcquisitionDateModel("Yes", Some(1), Some(1), Some(2017))),
          None
        )
        lazy val result = target.summary()(fakeRequest)
        lazy val document = Jsoup.parse(bodyOf(result))

        "have no value for the rebased costs" in {
          document.body.getElementById("purchaseDetails(2)").text() shouldBe "£0.00"
        }
      }

      "user provides no acquisition date and no rebased costs" should {
        val target = setupTarget(
          TestModels.summaryIndividualRebasedNoAcqDateOrRebasedCosts,
          TestModels.calcModelOneRate,
          Some(AcquisitionDateModel("Yes", Some(1), Some(1), Some(2017))),
          None
        )
        lazy val result = target.summary()(fakeRequest)
        lazy val document = Jsoup.parse(bodyOf(result))

        "have no value for the rebased costs" in {
          document.body.getElementById("purchaseDetails(1)").text() shouldBe "£0.00"
        }
      }
    }
  }

}
