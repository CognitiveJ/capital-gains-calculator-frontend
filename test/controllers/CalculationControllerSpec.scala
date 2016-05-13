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
import constructors.CalculationElectionConstructor
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
  val mockCalcElectionConstructor = mock[CalculationElectionConstructor]
  val TestCalculationController = new CalculationController {
    override val calcConnector: CalculatorConnector = mockCalcConnector
    override val calcElectionConstructor: CalculationElectionConstructor = mockCalcElectionConstructor

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

  def mockfetchAndGetFormData[T](data: Option[T]): Unit = {
    when(mockCalcConnector.fetchAndGetFormData[T](Matchers.anyString())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(data))
  }

  def mockCreateSummary(data: SummaryModel): Unit = {
    when(mockCalcConnector.createSummary(Matchers.any()))
      .thenReturn(data)
  }

  def mockCalculateFlatValue(data: Option[CalculationResultModel]): Unit = {
    when(mockCalcConnector.calculateFlat(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(data))
  }

  def mockCalculateTAValue(data: Option[CalculationResultModel]): Unit = {
    when(mockCalcConnector.calculateTA(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(data))
  }


  def mockCalculateRebasedValue(data: Option[CalculationResultModel]): Unit = {
    when(mockCalcConnector.calculateRebased(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(data))
  }

  def mockGenerateElection = {
    when(mockCalcElectionConstructor.generateElection(Matchers.any(), Matchers.any()))
      .thenReturn(Seq(
        ("flat", "8000.00", Messages("calc.calculationElection.message.flat"),
          None, routes.CalculationController.otherReliefs().toString()),
        ("time", "8000.00", Messages("calc.calculationElection.message.time"),
          Some(Messages("calc.calculationElection.message.timeDate")), routes.CalculationController.otherReliefsTA().toString())))
  }

  def mockfetchAndGetValue[T](data: Option[T]): Unit = {
    when(mockCalcConnector.fetchAndGetValue[T](Matchers.anyString())(Matchers.any(), Matchers.any()))
      .thenReturn(data)
  }

    val sumModelFlat = SummaryModel(
      CustomerTypeModel("individual"),
      None,
      Some(CurrentIncomeModel(1000)),
      Some(PersonalAllowanceModel(11100)),
      OtherPropertiesModel("No", None),
      None,
      AcquisitionDateModel("No", None, None, None),
      AcquisitionValueModel(100000),
      Some(RebasedValueModel("No", None)),
      None,
      ImprovementsModel("No", None),
      DisposalDateModel(10, 10, 2010),
      DisposalValueModel(150000),
      AcquisitionCostsModel(None),
      DisposalCostsModel(None),
      EntrepreneursReliefModel("No"),
      AllowableLossesModel("No", None),
      CalculationElectionModel("flat"),
      OtherReliefsModel(None),
      OtherReliefsModel(None),
      OtherReliefsModel(None)
    )

  val sumModelTA = SummaryModel(
    CustomerTypeModel("individual"),
    None,
    Some(CurrentIncomeModel(1000)),
    Some(PersonalAllowanceModel(11100)),
    OtherPropertiesModel("Yes", Some(2100)),
    Some(AnnualExemptAmountModel(9000)),
    AcquisitionDateModel("Yes", Some(9), Some(9), Some(9)),
    AcquisitionValueModel(100000),
    Some(RebasedValueModel("No", None)),
    None,
    ImprovementsModel("Yes", Some(500)),
    DisposalDateModel(10, 10, 2010),
    DisposalValueModel(150000),
    AcquisitionCostsModel(Some(650)),
    DisposalCostsModel(Some(850)),
    EntrepreneursReliefModel("No"),
    AllowableLossesModel("No", None),
    CalculationElectionModel("time"),
    OtherReliefsModel(Some(2000)),
    OtherReliefsModel(Some(1000)),
    OtherReliefsModel(Some(500))
  )

  val sumModelRebased = SummaryModel(
    CustomerTypeModel("individual"),
    None,
    Some(CurrentIncomeModel(1000)),
    Some(PersonalAllowanceModel(11100)),
    OtherPropertiesModel("Yes", Some(2100)),
    Some(AnnualExemptAmountModel(9000)),
    AcquisitionDateModel("Yes", Some(9), Some(9), Some(9)),
    AcquisitionValueModel(100000),
    Some(RebasedValueModel("No", None)),
    None,
    ImprovementsModel("Yes", Some(500)),
    DisposalDateModel(10, 10, 2010),
    DisposalValueModel(150000),
    AcquisitionCostsModel(Some(650)),
    DisposalCostsModel(Some(850)),
    EntrepreneursReliefModel("No"),
    AllowableLossesModel("No", None),
    CalculationElectionModel("rebased"),
    OtherReliefsModel(Some(2000)),
    OtherReliefsModel(Some(1000)),
    OtherReliefsModel(Some(500))
  )

  val calcModelTwoRates = CalculationResultModel(8000, 40000, 32000, 18, Some(8000), Some(28))
  val calcModelOneRate = CalculationResultModel(8000, 40000, 32000, 18, None, None)

  //############## Acquisition Date tests ######################

  //############## Acquisition Value tests ######################

  //################### Rebased Value Tests #######################

  //################### Rebased Costs Tests #######################

  //################### Improvements tests #######################

  //################### Disposal Date tests #######################

  //################### Disposal Value tests #######################

  //################### Acquisition Costs tests #######################
 
  //################### Disposal Costs tests #######################

  //################### Entrepreneurs Relief tests #######################

  //################### Allowable Losses tests #######################

  //################### Calculation Election tests #########################

  //################### Other Reliefs tests #######################

  //################### Time Apportioned Other Relief tests ###################

  //################### Rebased Other Relief tests ###################

  //################### Summary tests #######################

  //############## Private Residence Relief tests ######################
  "In CalculationController calling the .privateResidenceRelief action " should {
    object PrivateResidenceReliefTestDataItem extends fakeRequestTo("private-residence-relief", TestCalculationController.privateResidenceRelief)

    "return a 200" in {
      status(PrivateResidenceReliefTestDataItem.result) shouldBe 200
    }

    "return some HTML that" should {

      "contain some text and use the character set utf-8" in {
        contentType(PrivateResidenceReliefTestDataItem.result) shouldBe Some("text/html")
        charset(PrivateResidenceReliefTestDataItem.result) shouldBe Some("utf-8")
      }

      "have a back button" in {
        PrivateResidenceReliefTestDataItem.jsoupDoc.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
      }

      "have the title 'calc.privateResidenceRelief.question'" in {
        PrivateResidenceReliefTestDataItem.jsoupDoc.title shouldEqual Messages("calc.privateResidenceRelief.question")
      }

      "have the heading 'Calculate your tax (non-residents)'" in {
        PrivateResidenceReliefTestDataItem.jsoupDoc.body.getElementsByTag("H1").text shouldEqual Messages("calc.base.pageHeading")
      }

      "have a yes no helper with hidden content and question 'calc.privateResidenceRelief.question'" in {
        PrivateResidenceReliefTestDataItem.jsoupDoc.body.getElementById("isClaimingPRR-yes").parent.text shouldBe Messages("calc.base.yes")
        PrivateResidenceReliefTestDataItem.jsoupDoc.body.getElementById("isClaimingPRR-no").parent.text shouldBe Messages("calc.base.no")
        PrivateResidenceReliefTestDataItem.jsoupDoc.body.getElementsByTag("legend").text shouldBe Messages("calc.privateResidenceRelief.question")
      }

      "have a hidden input with question 'calc.privateResidenceRelief.questionTwo'" in {
        PrivateResidenceReliefTestDataItem.jsoupDoc.body.getElementById("daysClaimed").tagName shouldEqual "input"
        PrivateResidenceReliefTestDataItem.jsoupDoc.select("label[for=daysClaimed]").text should include (Messages("calc.privateResidenceRelief.questionTwoStart"))
        PrivateResidenceReliefTestDataItem.jsoupDoc.select("label[for=daysClaimed]").text should include (Messages("calc.privateResidenceRelief.questionTwoEnd"))

      }
    }
  }

  //############## Current Income tests ######################
  "In CalculationController calling the .currentIncome action " when {
    "not supplied with a pre-existing stored model" should {
      object CurrentIncomeTestDataItem extends fakeRequestTo("currentIncome", TestCalculationController.currentIncome)

      "return a 200" in {
        mockfetchAndGetFormData[CurrentIncomeModel](None)
        status(CurrentIncomeTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          mockfetchAndGetFormData[CurrentIncomeModel](None)
          contentType(CurrentIncomeTestDataItem.result) shouldBe Some("text/html")
          charset(CurrentIncomeTestDataItem.result) shouldBe Some("utf-8")
        }

        "have the title 'In the tax year when you stopped owning the property, what was your total UK income?'" in {
          mockfetchAndGetFormData[CurrentIncomeModel](None)
          CurrentIncomeTestDataItem.jsoupDoc.title shouldEqual Messages("calc.currentIncome.question")
        }

        "have the heading Calculate your tax (non-residents) " in {
          mockfetchAndGetFormData[CurrentIncomeModel](None)
          CurrentIncomeTestDataItem.jsoupDoc.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a 'Back' link " in {
          mockfetchAndGetFormData[CurrentIncomeModel](None)
          CurrentIncomeTestDataItem.jsoupDoc.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
        }

        "have the question 'In the tax year when you stopped owning the property, what was your total UK income?' as the label of the input" in {
          mockfetchAndGetFormData[CurrentIncomeModel](None)
          CurrentIncomeTestDataItem.jsoupDoc.body.getElementsByTag("label").text.contains(Messages("calc.currentIncome.question")) shouldBe true
        }

        "have the help text 'Tax years start on 6 April' as the form-hint of the input" in {
          mockfetchAndGetFormData[CurrentIncomeModel](None)
          CurrentIncomeTestDataItem.jsoupDoc.body.getElementsByClass("form-hint").text shouldEqual Messages("calc.currentIncome.helpText")
        }

        "display an input box for the Current Income Amount" in {
          mockfetchAndGetFormData[CurrentIncomeModel](None)
          CurrentIncomeTestDataItem.jsoupDoc.body.getElementById("currentIncome").tagName() shouldEqual "input"
        }

        "have no value auto-filled into the input box" in {
          mockfetchAndGetFormData[CurrentIncomeModel](None)
          CurrentIncomeTestDataItem.jsoupDoc.getElementById("currentIncome").attr("value") shouldBe empty
        }

        "display a 'Continue' button " in {
          mockfetchAndGetFormData[CurrentIncomeModel](None)
          CurrentIncomeTestDataItem.jsoupDoc.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
        }

        "should contain a Read more sidebar with a link to CGT allowances" in {
          mockfetchAndGetFormData[CurrentIncomeModel](None)
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
        mockfetchAndGetFormData[CurrentIncomeModel](Some(testModel))
        status(CurrentIncomeTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {
        "have some value auto-filled into the input box" in {
          mockfetchAndGetFormData[CurrentIncomeModel](Some(testModel))
          CurrentIncomeTestDataItem.jsoupDoc.getElementById("currentIncome").attr("value") shouldBe "1000"
        }
      }
    }
  }

  "In CalculationController calling the .submitCurrentIncome action " when {

    def mockSaveFormData[T](data: CurrentIncomeModel): Unit = {
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
        mockSaveFormData(testModel)
        status(CurrentIncomeTestDataItem.result) shouldBe 303
      }

      s"redirect to ${routes.CalculationController.personalAllowance()}" in {
        mockSaveFormData[CurrentIncomeModel](testModel)
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
        mockSaveFormData(testModel)
        CurrentIncomeTestDataItem.jsoupDoc.getElementsByClass("error-notification").text should include (Messages("calc.currentIncome.errorDecimalPlaces"))
      }
    }
  }
}
