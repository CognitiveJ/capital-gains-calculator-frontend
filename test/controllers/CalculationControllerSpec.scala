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
  "In CalculationController calling the .disposalDate action " when {
    "not supplied with a pre-existing stored model" should {

      object DisposalDateTestDataItem extends fakeRequestTo("disposal-date", TestCalculationController.disposalDate)

      "return a 200" in {
        mockfetchAndGetFormData[DisposalDateModel](None)
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
        mockfetchAndGetFormData[DisposalDateModel](Some(testDisposalDateModel))
        status(DisposalDateTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {
        "contain some text and use the character set utf-8" in {
          contentType(DisposalDateTestDataItem.result) shouldBe Some("text/html")
          charset(DisposalDateTestDataItem.result) shouldBe Some("utf-8")
        }

        "be pre-populated with the date 10, 12, 2016" in {
          mockfetchAndGetFormData[DisposalDateModel](Some(testDisposalDateModel))
          DisposalDateTestDataItem.jsoupDoc.body.getElementById("disposalDate.day").attr("value") shouldEqual testDisposalDateModel.day.toString
          DisposalDateTestDataItem.jsoupDoc.body.getElementById("disposalDate.month").attr("value") shouldEqual testDisposalDateModel.month.toString
          DisposalDateTestDataItem.jsoupDoc.body.getElementById("disposalDate.year").attr("value") shouldEqual testDisposalDateModel.year.toString
        }
      }
    }
  }

  "In CalculationController calling the .submitDisposalDate action" when {
    def mockSaveFormData[T](data: DisposalDateModel): Unit = {
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
        mockSaveFormData[DisposalDateModel](testModel)
        status(DisposalDateTestDataItem.result) shouldBe 303
      }

      s"redirect to ${routes.CalculationController.disposalValue()}" in {
        mockSaveFormData[DisposalDateModel](testModel)
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
        mockSaveFormData[DisposalDateModel](testModel)
        status(DisposalDateTestDataItem.result) shouldBe 303
      }

      s"redirect to ${routes.CalculationController.disposalValue()}" in {
        mockSaveFormData[DisposalDateModel](testModel)
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
        mockSaveFormData[DisposalDateModel](testModel)
        status(DisposalDateTestDataItem.result) shouldBe 400
      }

      s"should error with message '${Messages("calc.common.date.error.invalidDate")}'" in {
        mockSaveFormData[DisposalDateModel](testModel)
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
        mockSaveFormData[DisposalDateModel](testModel)
        status(DisposalDateTestDataItem.result) shouldBe 400
      }

      s"should error with message '${Messages("calc.common.date.error.day.lessThan1")}'" in {
        mockSaveFormData[DisposalDateModel](testModel)
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
        mockSaveFormData[DisposalDateModel](testModel)
        status(DisposalDateTestDataItem.result) shouldBe 400
      }

      s"should error with message '${Messages("calc.common.date.error.day.greaterThan31")}'" in {
        mockSaveFormData[DisposalDateModel](testModel)
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
        mockSaveFormData[DisposalDateModel](testModel)
        status(DisposalDateTestDataItem.result) shouldBe 400
      }

      s"should error with message '${Messages("calc.common.date.error.month.greaterThan12")}'" in {
        mockSaveFormData[DisposalDateModel](testModel)
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
        mockSaveFormData[DisposalDateModel](testModel)
        status(DisposalDateTestDataItem.result) shouldBe 400
      }

      s"should error with message '${Messages("calc.common.date.error.month.lessThan1")}'" in {
        mockSaveFormData[DisposalDateModel](testModel)
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
        mockSaveFormData[DisposalDateModel](testModel)
        status(DisposalDateTestDataItem.result) shouldBe 400
      }

      "should error with message 'calc.common.date.error.invalidDate'" in {
        mockSaveFormData[DisposalDateModel](testModel)
        DisposalDateTestDataItem.jsoupDoc.select(".error-notification").text should include (Messages("calc.common.date.error.invalidDate"))
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
        mockSaveFormData[DisposalDateModel](testModel)
        status(DisposalDateTestDataItem.result) shouldBe 400
      }

      "should error with message 'calc.common.date.error.invalidDate'" in {
        mockSaveFormData[DisposalDateModel](testModel)
        DisposalDateTestDataItem.jsoupDoc.select(".error-notification").text should include (Messages("calc.common.date.error.invalidDate"))
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
        mockSaveFormData[DisposalDateModel](testModel)
        status(DisposalDateTestDataItem.result) shouldBe 400
      }

      "should error with message 'calc.common.date.error.invalidDate'" in {
        mockSaveFormData[DisposalDateModel](testModel)
        DisposalDateTestDataItem.jsoupDoc.select(".error-notification").text should include (Messages("calc.common.date.error.invalidDate"))
      }
    }
  }

  //################### Disposal Value tests #######################

  //################### Acquisition Costs tests #######################
 
  //################### Disposal Costs tests #######################

  //################### Entrepreneurs Relief tests #######################

  //################### Allowable Losses tests #######################
  "In CalculationController calling the .allowableLosses action " when {

    "not supplied with a pre-existing stored value" should {

      object AllowableLossesTestDataItem extends fakeRequestTo("allowable-losses", TestCalculationController.allowableLosses)

      "return a 200" in {
        mockfetchAndGetFormData[AllowableLossesModel](None)
        status(AllowableLossesTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          mockfetchAndGetFormData[AllowableLossesModel](None)
          contentType(AllowableLossesTestDataItem.result) shouldBe Some("text/html")
          charset(AllowableLossesTestDataItem.result) shouldBe Some("utf-8")
        }

        "have a back button" in {
          mockfetchAndGetFormData[AllowableLossesModel](None)
          AllowableLossesTestDataItem.jsoupDoc.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
        }

        "have the title 'Are you claiming any allowable losses?'" in {
          mockfetchAndGetFormData[AllowableLossesModel](None)
          AllowableLossesTestDataItem.jsoupDoc.title shouldEqual Messages("calc.allowableLosses.question.one")
        }

        "have the heading 'Calculate your tax (non-residents)'" in {
          mockfetchAndGetFormData[AllowableLossesModel](None)
          AllowableLossesTestDataItem.jsoupDoc.body.getElementsByTag("H1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a yes no helper with hidden content and question 'Are you claiming any allowable losses?'" in {
          mockfetchAndGetFormData[AllowableLossesModel](None)
          AllowableLossesTestDataItem.jsoupDoc.body.getElementById("isClaimingAllowableLosses-yes").parent.text shouldBe Messages("calc.base.yes")
          AllowableLossesTestDataItem.jsoupDoc.body.getElementById("isClaimingAllowableLosses-no").parent.text shouldBe Messages("calc.base.no")
          AllowableLossesTestDataItem.jsoupDoc.body.getElementsByTag("legend").text shouldBe Messages("calc.allowableLosses.question.one")
        }

        "have a hidden monetary input with question 'Whats the total value of your allowable losses?'" in {
          mockfetchAndGetFormData[AllowableLossesModel](None)
          AllowableLossesTestDataItem.jsoupDoc.body.getElementById("allowableLossesAmt").tagName shouldEqual "input"
          AllowableLossesTestDataItem.jsoupDoc.select("label[for=allowableLossesAmt]").text should include (Messages("calc.allowableLosses.question.two"))
        }

        "have no value auto-filled into the input box" in {
          mockfetchAndGetFormData[AcquisitionValueModel](None)
          AllowableLossesTestDataItem.jsoupDoc.getElementById("allowableLossesAmt").attr("value") shouldBe empty
        }

        "have a hidden help text section with summary 'What are allowable losses?' and correct content" in {
          mockfetchAndGetFormData[AllowableLossesModel](None)
          AllowableLossesTestDataItem.jsoupDoc.select("div#allowableLossesHiddenHelp").text should
            include(Messages("calc.allowableLosses.helpText.title"))
            include(Messages("calc.allowableLosses.helpText.paragraph.one"))
            include(Messages("calc.allowableLosses.helpText.bullet.one"))
            include(Messages("calc.allowableLosses.helpText.bullet.two"))
            include(Messages("calc.allowableLosses.helpText.bullet.three"))
        }

        "has a Continue button" in {
          mockfetchAndGetFormData[AllowableLossesModel](None)
          AllowableLossesTestDataItem.jsoupDoc.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
        }
      }
    }
    "supplied with a pre-existing stored model" should {

      object AllowableLossesTestDataItem extends fakeRequestTo("allowable-losses", TestCalculationController.allowableLosses)
      val testModel = new AllowableLossesModel("Yes", Some(9999.54))

      "return a 200" in {
        mockfetchAndGetFormData[AllowableLossesModel](Some(testModel))
        status(AllowableLossesTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          mockfetchAndGetFormData[AllowableLossesModel](Some(testModel))
          contentType(AllowableLossesTestDataItem.result) shouldBe Some("text/html")
          charset(AllowableLossesTestDataItem.result) shouldBe Some("utf-8")
        }

        "have the 'Yes' Radio option selected" in {
          mockfetchAndGetFormData[AllowableLossesModel](Some(testModel))
          AllowableLossesTestDataItem.jsoupDoc.getElementById("isClaimingAllowableLosses-yes").parent.classNames().contains("selected") shouldBe true
        }

        "have the value 9999.54 auto-filled into the input box" in {
          mockfetchAndGetFormData[AllowableLossesModel](Some(testModel))
          AllowableLossesTestDataItem.jsoupDoc.getElementById("allowableLossesAmt").attr("value") shouldEqual ("9999.54")
        }
      }
    }
  }

  "In CalculationController calling the .submitAllowableLosses action" when {
    def mockSaveFormData[T](data: AllowableLossesModel): Unit = {
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
        mockfetchAndGetFormData(Some(acqDateModel))
        mockSaveFormData[AllowableLossesModel](testModel)
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
        mockfetchAndGetFormData(Some(acqDateModel))
        mockSaveFormData[AllowableLossesModel](testModel)
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
        mockfetchAndGetFormData(Some(acqDateModel))
        mockSaveFormData[AllowableLossesModel](testModel)
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
        mockfetchAndGetFormData(None)
        mockSaveFormData[AllowableLossesModel](testModel)
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
        mockSaveFormData[AllowableLossesModel](testModel)
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
        mockSaveFormData[AllowableLossesModel](testModel)
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
        mockSaveFormData[AllowableLossesModel](testModel)
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
        mockSaveFormData[AllowableLossesModel](testModel)
        status(AllowableLossesTestDataItem.result) shouldBe 400
      }
    }
  }

  //################### Calculation Election tests #########################

  "In CalculationController calling the .calculationElection action" when {

    "supplied with no pre-existing data" should {
      mockCreateSummary(TestModels.summaryTrusteeTAWithoutAEA)
      mockGenerateElection
      object CalculationElectionTestDataItem extends fakeRequestTo("calculation-election", TestCalculationController.calculationElection)

      "return a 200" in {
        mockCreateSummary(TestModels.summaryTrusteeTAWithoutAEA)
        mockGenerateElection
        mockfetchAndGetFormData[CalculationElectionModel](None)
        status(CalculationElectionTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set UTF-8" in {
          mockCreateSummary(TestModels.summaryTrusteeTAWithoutAEA)
          mockGenerateElection
          mockfetchAndGetFormData[CalculationElectionModel](None)
          contentType(CalculationElectionTestDataItem.result) shouldBe Some("text/html")
          charset(CalculationElectionTestDataItem.result) shouldBe Some("utf-8")
        }

        "have the title Which method of calculation would you like?" in {
          mockCreateSummary(TestModels.summaryTrusteeTAWithoutAEA)
          mockGenerateElection
          mockfetchAndGetFormData[CalculationElectionModel](None)
          CalculationElectionTestDataItem.jsoupDoc.title shouldEqual Messages("calc.calculationElection.question")
        }

        "have the heading Calculate your tax (non-residents) " in {
          mockCreateSummary(TestModels.summaryTrusteeTAWithoutAEA)
          mockGenerateElection
          mockfetchAndGetFormData[CalculationElectionModel](None)
          CalculationElectionTestDataItem.jsoupDoc.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a 'Back' link " in {
          mockCreateSummary(TestModels.summaryTrusteeTAWithoutAEA)
          mockGenerateElection
          mockfetchAndGetFormData[CalculationElectionModel](None)
          CalculationElectionTestDataItem.jsoupDoc.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
        }

        "have the paragraph You can decide what to base your Capital Gains Tax on. It affects how much you'll pay." in {
          mockCreateSummary(TestModels.summaryTrusteeTAWithoutAEA)
          mockGenerateElection
          mockfetchAndGetFormData[CalculationElectionModel](None)
          CalculationElectionTestDataItem.jsoupDoc.body.getElementById("calculationElection").text shouldEqual Messages("calc.calculationElection.message")
        }

        "have a calculationElectionHelper for the option of a flat calculation rendered on the page" in {
          mockCreateSummary(TestModels.summaryTrusteeTAWithoutAEA)
          mockGenerateElection
          mockfetchAndGetFormData[CalculationElectionModel](None)
          CalculationElectionTestDataItem.jsoupDoc.body.getElementById("calculationElection-flat").attr("value") shouldEqual "flat"
          CalculationElectionTestDataItem.jsoupDoc.body.getElementById("flat-para").text shouldEqual "Based on " + Messages("calc.calculationElection.message.flat")
        }

        "display a 'Continue' button " in {
          mockCreateSummary(TestModels.summaryTrusteeTAWithoutAEA)
          mockGenerateElection
          mockfetchAndGetFormData[CalculationElectionModel](None)
          CalculationElectionTestDataItem.jsoupDoc.body.getElementById("continue-button").text shouldEqual Messages("calc.base.continue")
        }

        "display a concertina information box with 'They sometimes qualify for larger tax reliefs. This can lower the amount you owe or even reduce it to zero' as the content" in {
          mockCreateSummary(TestModels.summaryTrusteeTAWithoutAEA)
          mockGenerateElection
          mockfetchAndGetFormData[CalculationElectionModel](None)
          CalculationElectionTestDataItem.jsoupDoc.select("summary span.summary").text shouldEqual Messages("calc.calculationElection.message.whyMore")
          CalculationElectionTestDataItem.jsoupDoc.select("div#details-content-0 p").text shouldEqual Messages("calc.calculationElection.message.whyMoreDetails")
        }
        "have no pre-selected option" in {
          mockCreateSummary(TestModels.summaryTrusteeTAWithoutAEA)
          mockGenerateElection
          mockfetchAndGetFormData[CalculationElectionModel](None)
          CalculationElectionTestDataItem.jsoupDoc.body.getElementById("calculationElection-flat").parent.classNames().contains("selected") shouldBe false
        }
      }
    }

    "supplied with pre-existing data" should {

      object CalculationElectionTestDataItem extends fakeRequestTo("calculation-election", TestCalculationController.calculationElection)
      val calculationElectionTestModel = new CalculationElectionModel("flat")
      mockCreateSummary(TestModels.summaryTrusteeTAWithoutAEA)
      mockGenerateElection

      "return a 200" in {
        mockfetchAndGetFormData[CalculationElectionModel](Some(calculationElectionTestModel))
        status(CalculationElectionTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          mockfetchAndGetFormData[CalculationElectionModel](Some(calculationElectionTestModel))
          contentType(CalculationElectionTestDataItem.result) shouldBe Some("text/html")
          charset(CalculationElectionTestDataItem.result) shouldBe Some("utf-8")
        }

        "have the stored value of flat calculation selected" in {
          mockCreateSummary(TestModels.summaryTrusteeTAWithoutAEA)
          mockGenerateElection
          mockfetchAndGetFormData[CalculationElectionModel](Some(calculationElectionTestModel))
          CalculationElectionTestDataItem.jsoupDoc.body.getElementById("calculationElection-flat").parent.classNames().contains("selected") shouldBe true
        }
      }
    }
  }

  "In CalculationController calling the .submitCalculationElection action" when {

    def mockSaveFormData[T](data: CalculationElectionModel): Unit = {
      lazy val returnedCacheMap = CacheMap("form-id", Map("data" -> Json.toJson(data)))
      when(mockCalcConnector.saveFormData[T](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(returnedCacheMap))
    }

    "submitting a valid form with 'flat' selected" should {
      object CalculationElectionTestDataItem extends fakeRequestToPost("calculation-election", TestCalculationController.submitCalculationElection, ("calculationElection", "flat"))
      val calculationElectionTestModel = new CalculationElectionModel("flat")

      "return a 303" in {
        mockCreateSummary(TestModels.summaryTrusteeTAWithoutAEA)
        mockCalculateFlatValue(Some(calcModelOneRate))
        mockSaveFormData[CalculationElectionModel](calculationElectionTestModel)
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
        mockCreateSummary(TestModels.summaryTrusteeTAWithoutAEA)
        mockCalculateFlatValue(Some(calcModelOneRate))
        mockSaveFormData[CalculationElectionModel](calculationElectionTestModel)
        status(CalculationElectionTestDataItem.result) shouldBe 303
      }
    }

    "submitting a valid form with 'rebased' selected" should {
      object CalculationElectionTestDataItem extends fakeRequestToPost("calculation-election", TestCalculationController.submitCalculationElection, ("calculationElection", "rebased"))
      val calculationElectionTestModel = new CalculationElectionModel("rebased")

      "return a 303" in {
        mockCreateSummary(sumModelTA)
        mockCalculateFlatValue(Some(calcModelOneRate))
        mockSaveFormData[CalculationElectionModel](calculationElectionTestModel)
        status(CalculationElectionTestDataItem.result) shouldBe 303
      }
    }

    "submitting a form with no data" should  {
      object CalculationElectionTestDataItem extends fakeRequestToPost("calculation-election", TestCalculationController.submitCalculationElection)
      val calculationElectionTestModel = new CalculationElectionModel("")

      "return a 400" in {
        mockCreateSummary(sumModelFlat)
        mockCalculateFlatValue(Some(calcModelOneRate))
        mockCalculateTAValue(Some(TestModels.calcModelTwoRates))
        mockSaveFormData[CalculationElectionModel](calculationElectionTestModel)
        status(CalculationElectionTestDataItem.result) shouldBe 400
      }

      "display a visible Error Summary field" in {
        mockSaveFormData[CalculationElectionModel](calculationElectionTestModel)
        CalculationElectionTestDataItem.jsoupDoc.getElementById("error-summary-display").hasClass("error-summary--show")
      }

      "link to the invalid input box in Error Summary" in {
        mockSaveFormData[CalculationElectionModel](calculationElectionTestModel)
        CalculationElectionTestDataItem.jsoupDoc.getElementById("calculationElection-error-summary").attr("href") should include ("#calculationElection")
      }
    }

    "submitting a form with completely unrelated 'ew1234qwer'" should  {
      object CalculationElectionTestDataItem extends fakeRequestToPost("calculation-election", TestCalculationController.submitCalculationElection, ("calculationElection", "ew1234qwer"))
      val calculationElectionTestModel = new CalculationElectionModel("ew1234qwer")

      "return a 400" in {
        mockCreateSummary(sumModelFlat)
        mockCalculateFlatValue(Some(calcModelOneRate))
        mockSaveFormData[CalculationElectionModel](calculationElectionTestModel)
        status(CalculationElectionTestDataItem.result) shouldBe 400
      }
    }
  }

  //################### Other Reliefs tests #######################
  "In CalculationController calling the .otherReliefs action " when {
    "not supplied with a pre-existing stored model" should {
      object OtherReliefsTestDataItem extends fakeRequestTo("other-reliefs", TestCalculationController.otherReliefs)

      "return a 200 with a valid calculation result" in {
        mockCreateSummary(sumModelFlat)
        mockCalculateFlatValue(Some(calcModelTwoRates))
        mockfetchAndGetFormData[OtherReliefsModel](None)
        status(OtherReliefsTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          mockCreateSummary(sumModelFlat)
          mockCalculateFlatValue(Some(calcModelTwoRates))
          contentType(OtherReliefsTestDataItem.result) shouldBe Some("text/html")
          charset(OtherReliefsTestDataItem.result) shouldBe Some("utf-8")
        }
        "have the title 'How much extra tax relief are you claiming?'" in {
          mockCreateSummary(sumModelFlat)
          mockCalculateFlatValue(Some(calcModelTwoRates))
          OtherReliefsTestDataItem.jsoupDoc.title shouldEqual Messages("calc.otherReliefs.question")
        }

        "have the heading Calculate your tax (non-residents) " in {
          mockCreateSummary(sumModelFlat)
          mockCalculateFlatValue(Some(calcModelTwoRates))
          OtherReliefsTestDataItem.jsoupDoc.body.getElementsByTag("h1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a 'Back' link " in {
          mockCreateSummary(sumModelFlat)
          mockCalculateFlatValue(Some(calcModelTwoRates))
          OtherReliefsTestDataItem.jsoupDoc.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
        }

        "have the question 'How much extra tax relief are you claiming?' as the legend of the input" in {
          mockCreateSummary(sumModelFlat)
          mockCalculateFlatValue(Some(calcModelTwoRates))
          OtherReliefsTestDataItem.jsoupDoc.body.getElementsByTag("label").text should include (Messages("calc.otherReliefs.question"))
        }

        "have a value for your gain" in {
          mockCreateSummary(sumModelFlat)
          mockCalculateFlatValue(Some(calcModelTwoRates))
          mockfetchAndGetFormData[OtherReliefsModel](None)
          OtherReliefsTestDataItem.jsoupDoc.getElementById("totalGain").text() shouldBe ("£40000.00 Total gain")
        }

        "display an input box for the Other Tax Reliefs" in {
          mockCreateSummary(sumModelFlat)
          mockCalculateFlatValue(Some(calcModelTwoRates))
          OtherReliefsTestDataItem.jsoupDoc.body.getElementById("otherReliefs").tagName() shouldEqual "input"
        }

        "display an 'Add relief' button " in {
          mockCreateSummary(sumModelFlat)
          mockCalculateFlatValue(Some(calcModelTwoRates))
          OtherReliefsTestDataItem.jsoupDoc.body.getElementById("add-relief-button").text shouldEqual Messages("calc.otherReliefs.button.addRelief")
        }

        "include helptext for 'Total gain'" in {
          mockCreateSummary(sumModelFlat)
          mockCalculateFlatValue(Some(calcModelTwoRates))
          OtherReliefsTestDataItem.jsoupDoc.body.getElementById("totalGain").text should include (Messages("calc.otherReliefs.totalGain"))
        }

        "include helptext for 'Taxable gain'" in {
          mockCreateSummary(sumModelFlat)
          mockCalculateFlatValue(Some(calcModelTwoRates))
          OtherReliefsTestDataItem.jsoupDoc.body.getElementById("taxableGain").text should include (Messages("calc.otherReliefs.taxableGain"))
        }
      }
    }
    "supplied with a pre-existing stored model and a loss" should {
      object OtherReliefsTestDataItem extends fakeRequestTo("other-reliefs", TestCalculationController.otherReliefs)
      val testOtherReliefsModel = new OtherReliefsModel(Some(5000))

      "return a 200 with a valid calculation call" in {
        mockCreateSummary(sumModelFlat)
        mockCalculateFlatValue(Some(TestModels.calcModelLoss))
        mockfetchAndGetFormData[OtherReliefsModel](Some(testOtherReliefsModel))
        status(OtherReliefsTestDataItem.result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          mockCreateSummary(sumModelFlat)
          mockCalculateFlatValue(Some(TestModels.calcModelLoss))
          contentType(OtherReliefsTestDataItem.result) shouldBe Some("text/html")
          charset(OtherReliefsTestDataItem.result) shouldBe Some("utf-8")
        }

        "have the value 5000 auto-filled into the input box" in {
          mockCreateSummary(sumModelFlat)
          mockCalculateFlatValue(Some(TestModels.calcModelLoss))
          mockfetchAndGetFormData[OtherReliefsModel](Some(testOtherReliefsModel))
          OtherReliefsTestDataItem.jsoupDoc.getElementById("otherReliefs").attr("value") shouldEqual "5000"
        }

        "have a value for your loss" in {
          mockCreateSummary(sumModelFlat)
          mockCalculateFlatValue(Some(TestModels.calcModelLoss))
          mockfetchAndGetFormData[OtherReliefsModel](Some(testOtherReliefsModel))
          OtherReliefsTestDataItem.jsoupDoc.getElementById("totalGain").text() shouldBe ("£10000.00 Total loss")
        }

      }
    }
  }

  "In CalculationController calling the .submitOtherReliefs action" when {
    def mockSaveFormData[T](data: OtherReliefsModel): Unit = {
      lazy val returnedCacheMap = CacheMap("form-id", Map("data" -> Json.toJson(data)))
      when(mockCalcConnector.saveFormData[T](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(returnedCacheMap))
    }

    "submitting a valid form with and an amount of 1000" should {


      "return a 303 with no Acquisition date" in {
        object OtherReliefsTestDataItem extends fakeRequestToPost("other-reliefs", TestCalculationController.submitOtherReliefs, ("otherReliefs", "1000"))
        val otherReliefsTestModel = new OtherReliefsModel(Some(1000))
        mockCreateSummary(sumModelFlat)
        mockCalculateFlatValue(Some(calcModelTwoRates))
        mockSaveFormData[OtherReliefsModel](otherReliefsTestModel)
        status(OtherReliefsTestDataItem.result) shouldBe 303
      }

      "return a 303 with an Acquisition date before the start date" in {
        object OtherReliefsTestDataItem extends fakeRequestToPost("other-reliefs", TestCalculationController.submitOtherReliefs, ("otherReliefs", "1000"))
        val otherReliefsTestModel = new OtherReliefsModel(Some(1000))
        mockCreateSummary(TestModels.summaryTrusteeTAWithoutAEA)
        mockCalculateFlatValue(Some(calcModelTwoRates))
        mockSaveFormData[OtherReliefsModel](otherReliefsTestModel)
        status(OtherReliefsTestDataItem.result) shouldBe 303
      }

      "return a 303 with an Acquisition date after the start date" in {
        object OtherReliefsTestDataItem extends fakeRequestToPost("other-reliefs", TestCalculationController.submitOtherReliefs, ("otherReliefs", "1000"))
        val otherReliefsTestModel = new OtherReliefsModel(Some(1000))
        mockCreateSummary(TestModels.summaryIndividualAcqDateAfter)
        mockCalculateFlatValue(Some(calcModelTwoRates))
        mockSaveFormData[OtherReliefsModel](otherReliefsTestModel)
        status(OtherReliefsTestDataItem.result) shouldBe 303
      }
    }

    "submitting a valid form with and an amount with two decimal places" should {
      object OtherReliefsTestDataItem extends fakeRequestToPost("other-reliefs", TestCalculationController.submitOtherReliefs, ("otherReliefs", "1000.11"))
      val otherReliefsTestModel = new OtherReliefsModel(Some(1000.11))

      "return a 303" in {
        mockCreateSummary(sumModelFlat)
        mockCalculateFlatValue(Some(calcModelTwoRates))
        mockSaveFormData[OtherReliefsModel](otherReliefsTestModel)
        status(OtherReliefsTestDataItem.result) shouldBe 303
      }
    }

    "submitting an valid form with no value" should {
      object OtherReliefsTestDataItem extends fakeRequestToPost("other-reliefs", TestCalculationController.submitOtherReliefs, ("otherReliefs", ""))
      val otherReliefsTestModel = new OtherReliefsModel(Some(0))

      "return a 303" in {
        mockCreateSummary(sumModelFlat)
        mockCalculateFlatValue(Some(calcModelTwoRates))
        mockSaveFormData[OtherReliefsModel](otherReliefsTestModel)
        status(OtherReliefsTestDataItem.result) shouldBe 303
      }
    }

    "submitting an invalid form with an amount with three decimal places" should {
      object OtherReliefsTestDataItem extends fakeRequestToPost("other-reliefs", TestCalculationController.submitOtherReliefs, ("otherReliefs", "1000.111"))
      val otherReliefsTestModel = new OtherReliefsModel(Some(1000.111))

      "return a 400" in {
        mockCreateSummary(sumModelFlat)
        mockCalculateFlatValue(Some(calcModelTwoRates))
        mockSaveFormData[OtherReliefsModel](otherReliefsTestModel)
        status(OtherReliefsTestDataItem.result) shouldBe 400
      }
    }

    "submitting an invalid form with a negative value" should {
      object OtherReliefsTestDataItem extends fakeRequestToPost("other-reliefs", TestCalculationController.submitOtherReliefs, ("otherReliefs", "-1000"))
      val otherReliefsTestModel = new OtherReliefsModel(Some(-1000))

      "return a 400" in {
        mockCreateSummary(sumModelFlat)
        mockCalculateFlatValue(Some(calcModelTwoRates))
        mockSaveFormData[OtherReliefsModel](otherReliefsTestModel)
        status(OtherReliefsTestDataItem.result) shouldBe 400
      }
    }

    "submitting an invalid form with an value of shdgsaf" should {
      object OtherReliefsTestDataItem extends fakeRequestToPost("other-reliefs", TestCalculationController.submitOtherReliefs, ("otherReliefs", "shdgsaf"))
      val otherReliefsTestModel = new OtherReliefsModel(Some(1000))

      "return a 400" in {
        mockCreateSummary(sumModelFlat)
        mockCalculateFlatValue(Some(calcModelTwoRates))
        mockSaveFormData[OtherReliefsModel](otherReliefsTestModel)
        status(OtherReliefsTestDataItem.result) shouldBe 400
      }
    }
  }

  //################### Time Apportioned Other Relief tests ###################
  "In CalculationController calling the .otherReliefsTA action " should  {
    mockfetchAndGetFormData[OtherReliefsModel](None)
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
        mockfetchAndGetFormData[OtherReliefsModel](None)
        OtherReliefsTATestDataItem.jsoupDoc.body.getElementById("otherReliefs").attr("value") shouldBe ""
      }
    }

    "when supplied with a previous value" should {
      val testModel = OtherReliefsModel(Some(1000))
      object OtherReliefsTATestDataItem extends fakeRequestTo("other-reliefs-time-apportioned", TestCalculationController.otherReliefsTA)

      "contain the pre-supplied data" in {
        mockfetchAndGetFormData[OtherReliefsModel](Some(testModel))
        OtherReliefsTATestDataItem.jsoupDoc.body.getElementById("otherReliefs").attr("value") shouldBe "1000"
      }
    }
  }

  "In CalculationController calling the .submitOtherReliefsTA action" when {
    def mockSaveFormData[T](data: OtherReliefsModel): Unit = {
      lazy val returnedCacheMap = CacheMap("form-id", Map("data" -> Json.toJson(data)))
      when(mockCalcConnector.saveFormData[T](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(returnedCacheMap))
    }

    "submitting a valid form with and an amount of 1000" should {
      object OtherReliefsTATestDataItem extends fakeRequestToPost("other-reliefs-time-apportioned", TestCalculationController.submitOtherReliefsTA, ("otherReliefs", "1000"))
      val otherReliefsTATestModel = new OtherReliefsModel(Some(1000))

      "return a 303" in {
        mockSaveFormData[OtherReliefsModel](otherReliefsTATestModel)
        status(OtherReliefsTATestDataItem.result) shouldBe 303
      }
    }

    "submitting a valid form with and an amount with two decimal places" should {
      object OtherReliefsTATestDataItem extends fakeRequestToPost("other-reliefs-time-apportioned", TestCalculationController.submitOtherReliefsTA, ("otherReliefs", "1000.11"))
      val otherReliefsTATestModel = new OtherReliefsModel(Some(1000.11))

      "return a 303" in {
        mockSaveFormData[OtherReliefsModel](otherReliefsTATestModel)
        status(OtherReliefsTATestDataItem.result) shouldBe 303
      }
    }

    "submitting an valid form with no value" should {
      object OtherReliefsTATestDataItem extends fakeRequestToPost("other-reliefs-time-apportioned", TestCalculationController.submitOtherReliefsTA, ("otherReliefs", ""))
      val otherReliefsTATestModel = new OtherReliefsModel(Some(0))

      "return a 303" in {
        mockSaveFormData[OtherReliefsModel](otherReliefsTATestModel)
        status(OtherReliefsTATestDataItem.result) shouldBe 303
      }
    }

    "submitting an invalid form with an amount with three decimal places" should {
      object OtherReliefsTATestDataItem extends fakeRequestToPost("other-reliefs-time-apportioned", TestCalculationController.submitOtherReliefsTA, ("otherReliefs", "1000.111"))
      val otherReliefsTATestModel = new OtherReliefsModel(Some(1000.111))

      "return a 400" in {
        mockSaveFormData[OtherReliefsModel](otherReliefsTATestModel)
        status(OtherReliefsTATestDataItem.result) shouldBe 400
      }
    }

    "submitting an invalid form with a negative value" should {
      object OtherReliefsTATestDataItem extends fakeRequestToPost("other-reliefs-time-apportioned", TestCalculationController.submitOtherReliefsTA, ("otherReliefs", "-1000"))
      val otherReliefsTATestModel = new OtherReliefsModel(Some(-1000))

      "return a 400" in {
        mockSaveFormData[OtherReliefsModel](otherReliefsTATestModel)
        status(OtherReliefsTATestDataItem.result) shouldBe 400
      }
    }

    "submitting an invalid form with an value of shdgsaf" should {
      object OtherReliefsTATestDataItem extends fakeRequestToPost("other-reliefs-time-apportioned", TestCalculationController.submitOtherReliefsTA, ("otherReliefs", "shdgsaf"))
      val otherReliefsTATestModel = new OtherReliefsModel(Some(1000))

      "return a 400" in {
        mockSaveFormData[OtherReliefsModel](otherReliefsTATestModel)
        status(OtherReliefsTATestDataItem.result) shouldBe 400
      }
    }
  }

  //################### Rebased Other Relief tests ###################
  "In CalculationController calling the .otherReliefsRebased action " should  {
    mockfetchAndGetFormData[OtherReliefsModel](None)
    mockCreateSummary(sumModelRebased)
    mockCalculateRebasedValue(Some(calcModelTwoRates))
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

    "when not supplied with any previous value" should {
      object OtherReliefsRebasedTestDataItem extends fakeRequestTo("other-reliefs-rebased", TestCalculationController.otherReliefsRebased)

      "contain no pre-filled data" in {
        mockfetchAndGetFormData[OtherReliefsModel](None)
        OtherReliefsRebasedTestDataItem.jsoupDoc.body.getElementById("otherReliefs").attr("value") shouldBe ""
      }
    }

    "when supplied with a previous value" should {
      val testotherReliefsRebasedModel = OtherReliefsModel(Some(1000))
      object OtherReliefsRebasedTestDataItem extends fakeRequestTo("other-reliefs-rebased", TestCalculationController.otherReliefsRebased)

      "contain the pre-supplied data" in {
        mockfetchAndGetFormData[OtherReliefsModel](Some(testotherReliefsRebasedModel))
        OtherReliefsRebasedTestDataItem.jsoupDoc.body.getElementById("otherReliefs").attr("value") shouldBe "1000"
      }
    }
  }

  "In CalculationController calling the .submitOtherReliefsRebased action" when {
    def mockSaveFormData[T](data: OtherReliefsModel): Unit = {
      lazy val returnedCacheMap = CacheMap("form-id", Map("data" -> Json.toJson(data)))
      when(mockCalcConnector.saveFormData[T](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(returnedCacheMap))
    }

    "submitting a valid form and an amount of 1000" should {
      object OtherReliefsRebasedTestDataItem extends fakeRequestToPost("other-reliefs-rebased", TestCalculationController.submitOtherReliefsRebased, ("otherReliefs", "1000"))
      val otherReliefsRebasedTestModel = new OtherReliefsModel(Some(1000))
      mockCreateSummary(sumModelRebased)
      mockCalculateRebasedValue(Some(calcModelTwoRates))

      "return a 303" in {
        mockSaveFormData[OtherReliefsModel](otherReliefsRebasedTestModel)
        status(OtherReliefsRebasedTestDataItem.result) shouldBe 303
      }

      s"redirect to ${routes.CalculationController.calculationElection()}" in {
        mockSaveFormData[OtherReliefsModel](otherReliefsRebasedTestModel)
        redirectLocation(OtherReliefsRebasedTestDataItem.result) shouldBe Some(s"${routes.CalculationController.calculationElection()}")
      }
    }
  }

  //################### Summary tests #######################
  "In CalculationController calling the .summary action" when {

    "individual is chosen with a flat calculation" when {

      "the user has provided a value for the AEA" should {
        mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
        mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
        object SummaryTestDataItem extends fakeRequestTo("summary", TestCalculationController.summary)

        "return a 200" in {
          mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
          mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
          status(SummaryTestDataItem.result) shouldBe 200
        }

        "return some HTML that" should {

          "should have the title 'Summary'" in {
            mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
            mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
            SummaryTestDataItem.jsoupDoc.getElementsByTag("title").text shouldEqual Messages("calc.summary.title")
          }

          "have a back button" in {
            mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
            mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
            SummaryTestDataItem.jsoupDoc.getElementById("back-link").text shouldEqual Messages("calc.base.back")
          }

          "have the correct sub-heading 'You owe'" in {
            mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
            mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
            SummaryTestDataItem.jsoupDoc.select("h1 span").text shouldEqual Messages("calc.summary.secondaryHeading")
          }

          "have a result amount currently set to £8000.00" in {
            mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
            mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
            SummaryTestDataItem.jsoupDoc.select("h1 b").text shouldEqual "£8000.00"
          }

          "have a 'Calculation details' section that" should {

            "include the section heading 'Calculation details" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#calcDetails").text should include(Messages("calc.summary.calculation.details.title"))
            }

            "include 'How would you like to work out your tax?'" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#calcDetails").text should include(Messages("calc.summary.calculation.details.calculationElection"))
            }

            "have an election description of 'How much of your total gain you've made since 5 April 2015'" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body().getElementById("calcDetails(0)").text() shouldBe Messages("calc.summary.calculation.details.flatCalculation")
            }

            "include 'Your total gain'" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#calcDetails").text should include(Messages("calc.summary.calculation.details.totalGain"))
            }

            "have a total gain equal to £40000.00" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body().getElementById("calcDetails(1)").text() shouldBe "£40000.00"
            }

            "include 'Your taxable gain'" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#calcDetails").text should include(Messages("calc.summary.calculation.details.taxableGain"))
            }

            "have a taxable gain equal to £40000.00" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body().getElementById("calcDetails(2)").text() shouldBe "£40000.00"
            }

            "include 'Your tax rate'" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#calcDetails").text should include(Messages("calc.summary.calculation.details.taxRate"))
            }

            "have a base tax rate of £32000" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body().getElementById("calcDetails(3)").text() shouldBe "£32000.00 at 18%"
            }

            "have an upper tax rate of £8000" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body().getElementById("calcDetails(4)").text() shouldBe "£8000.00 at 28%"
            }

          }

          "have a 'Personal details' section that" should {

            "include the section heading 'Personal details" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#personalDetails").text should include(Messages("calc.summary.personal.details.title"))
            }

            "include the question 'Who owned the property?'" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#personalDetails").text should include(Messages("calc.customerType.question"))
            }

            "have an 'individual' owner" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body().getElementById("personalDetails(0)").text() shouldBe "Individual"
            }

            "include the question 'What’s your total income for this tax year?'" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#personalDetails").text should include(Messages("calc.currentIncome.question"))
            }

            "have an total income of £1000" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body().getElementById("personalDetails(1)").text() shouldBe "£1000.00"
            }

            "include the question 'What's your Personal Allowance for this tax year?'" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#personalDetails").text should include(Messages("calc.personalAllowance.question"))
            }

            "have a personal allowance of £9000" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body().getElementById("personalDetails(2)").text() shouldBe "£9000.00"
            }

            "include the question 'What was the total taxable gain of your previous Capital Gains in the tax year you stopped owning the property?'" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#personalDetails").text should include(Messages("calc.otherProperties.questionTwo"))
            }

            "have a total taxable gain of prior disposals of £9600" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body.getElementById("personalDetails(3)").text() shouldBe "£9600.00"
            }

            "include the question 'How much of your Capital Gains Tax allowance have you got left'" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#personalDetails").text should include(Messages("calc.annualExemptAmount.question"))
            }

            "have a remaining CGT Allowance of £1500" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body().getElementById("personalDetails(4)").text() shouldBe "£1500.00"
            }
          }

          "have a 'Purchase details' section that" should {

            "include the section heading 'Purchase details" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#purchaseDetails").text should include(Messages("calc.summary.purchase.details.title"))
            }

            "include the question 'How much did you pay for the property?'" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#purchaseDetails").text should include(Messages("calc.acquisitionValue.question"))
            }

            "have an acquisition value of £100000" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body().getElementById("purchaseDetails(0)").text() shouldBe "£100000.00"
            }

            "include the question 'How much did you pay in costs when you became the property owner?'" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#purchaseDetails").text should include(Messages("calc.acquisitionCosts.question"))
            }

            "have a acquisition costs of £0" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body().getElementById("purchaseDetails(1)").text() shouldBe "£0.00"
            }
          }

          "have a 'Property details' section that" should {

            "include the section heading 'Property details" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#propertyDetails").text should include(Messages("calc.summary.property.details.title"))
            }

            "include the question 'Did you make any improvements to the property?'" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#propertyDetails").text should include(Messages("calc.improvements.question"))
            }

            "the answer to the improvements question should be No" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body.getElementById("propertyDetails(0)").text shouldBe "No"
            }
          }

          "have a 'Sale details' section that" should {

            "include the section heading 'Sale details" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#saleDetails").text should include(Messages("calc.summary.sale.details.title"))
            }

            "include the question 'When did you sign the contract that made someone else the owner?'" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#saleDetails").text should include(Messages("calc.disposalDate.question"))
            }

            "the date of disposal should be '10 October 2010" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body().getElementById("saleDetails(0)").text shouldBe "10 October 2010"
            }

            "include the question 'How much did you sell or give away the property for?'" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#saleDetails").text should include(Messages("calc.disposalValue.question"))
            }

            "the value of the sale should be £150000" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body().getElementById("saleDetails(1)").text shouldBe "£150000.00"
            }

            "include the question 'How much did you pay in costs when you stopped being the property owner?'" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#saleDetails").text should include(Messages("calc.disposalCosts.question"))
            }

            "the value of the costs should be £0" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body().getElementById("saleDetails(2)").text shouldBe "£0.00"
            }
          }

          "have a 'Deductions details' section that" should {

            "include the section heading 'Deductions" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#deductions").text should include(Messages("calc.summary.deductions.title"))
            }

            "include the question 'Are you claiming Entrepreneurs' Relief?'" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#deductions").text should include(Messages("calc.entrepreneursRelief.question"))
            }

            "have the answer to entrepreneurs relief question be 'No'" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body().getElementById("deductions(0)").text shouldBe "No"
            }

            "include the question 'Whats the total value of your allowable losses?'" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#deductions").text should include(Messages("calc.allowableLosses.question.two"))
            }

            "the value of allowable losses should be £0" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body().getElementById("deductions(1)").text shouldBe "£0.00"
            }

            "include the question 'What other reliefs are you claiming?'" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#deductions").text should include(Messages("calc.otherReliefs.question"))
            }

            "the value of other reliefs should be £0" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.body().getElementById("deductions(2)").text shouldBe "£0.00"
            }

          }

          "have a 'What to do next' section that" should {

            "have the heading 'What to do next'" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#whatToDoNext H2").text shouldEqual (Messages("calc.common.next.actions.heading"))
            }

            "include the text 'You need to tell HMRC about the property'" in {
              mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
              mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
              SummaryTestDataItem.jsoupDoc.select("#whatToDoNext").text should
                include(Messages("calc.summary.next.actions.text"))
              include(Messages("calc.summary.next.actions.link"))
            }
          }

          "have a link to 'Start again'" in {
            mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
            mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
            SummaryTestDataItem.jsoupDoc.select("#startAgain").text shouldEqual Messages("calc.summary.startAgain")
          }
        }
      }

      "the user has provided no value for the AEA" should {
        mockCreateSummary(TestModels.summaryIndividualFlatWithoutAEA)
        mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
        object SummaryTestDataItem extends fakeRequestTo("summary", TestCalculationController.summary)

        "have a remaining CGT Allowance of £11100" in {
          mockCreateSummary(TestModels.summaryIndividualFlatWithoutAEA)
          mockCalculateFlatValue(Some(TestModels.calcModelOneRate))
          SummaryTestDataItem.jsoupDoc.body().getElementById("personalDetails(3)").text() shouldBe "£11100.00"
        }

        "the answer to the improvements question should be Yes" in {
          mockCreateSummary(TestModels.summaryIndividualFlatWithAEA)
          mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
          SummaryTestDataItem.jsoupDoc.body.getElementById("propertyDetails(0)").text shouldBe "Yes"
        }

        "the value of the improvements should be £8000" in {
          mockCreateSummary(TestModels.summaryIndividualFlatWithoutAEA)
          mockCalculateFlatValue(Some(TestModels.calcModelOneRate))
          SummaryTestDataItem.jsoupDoc.body.getElementById("propertyDetails(1)").text shouldBe "£8000.00"
        }

        "the value of the disposal costs should be £600" in {
          mockCreateSummary(TestModels.summaryIndividualFlatWithoutAEA)
          mockCalculateFlatValue(Some(TestModels.calcModelOneRate))
          SummaryTestDataItem.jsoupDoc.body().getElementById("saleDetails(2)").text shouldBe "£600.00"
        }

        "have a acquisition costs of £300" in {
          mockCreateSummary(TestModels.summaryIndividualFlatWithoutAEA)
          mockCalculateFlatValue(Some(TestModels.calcModelOneRate))
          SummaryTestDataItem.jsoupDoc.body().getElementById("purchaseDetails(1)").text() shouldBe "£300.00"
        }

        "the value of allowable losses should be £50000" in {
          mockCreateSummary(TestModels.summaryIndividualFlatWithoutAEA)
          mockCalculateFlatValue(Some(TestModels.calcModelOneRate))
          SummaryTestDataItem.jsoupDoc.body().getElementById("deductions(1)").text shouldBe "£50000.00"
        }

        "the value of other reliefs should be £999" in {
          mockCreateSummary(TestModels.summaryIndividualFlatWithoutAEA)
          mockCalculateFlatValue(Some(TestModels.calcModelOneRate))
          SummaryTestDataItem.jsoupDoc.body().getElementById("deductions(2)").text shouldBe "£999.00"
        }

        "have a base tax rate of 20%" in {
          mockCreateSummary(TestModels.summaryIndividualFlatWithoutAEA)
          mockCalculateFlatValue(Some(TestModels.calcModelOneRate))
          SummaryTestDataItem.jsoupDoc.body().getElementById("calcDetails(3)").text() shouldBe "20%"
        }
      }

      "users calculation results in a loss" should {
        mockCreateSummary(TestModels.summaryIndividualFlatLoss)
        mockCalculateFlatValue(Some(TestModels.calcModelLoss))
        object SummaryTestDataItem extends fakeRequestTo("summary", TestCalculationController.summary)

        s"have ${(Messages("calc.summary.calculation.details.totalLoss"))} output" in {
          mockCreateSummary(TestModels.summaryIndividualFlatLoss)
          mockCalculateFlatValue(Some(TestModels.calcModelLoss))
          SummaryTestDataItem.jsoupDoc.body.getElementById("calcDetails").text() should include (Messages("calc.summary.calculation.details.totalLoss"))
        }

        s"have £10000.00 loss" in {
          mockCreateSummary(TestModels.summaryIndividualFlatLoss)
          mockCalculateFlatValue(Some(TestModels.calcModelLoss))
          SummaryTestDataItem.jsoupDoc.body.getElementById("calcDetails(1)").text() shouldBe "£10000.00"
        }
      }
    }

    "regular trustee is chosen with a time apportioned calculation" when {

      "the user has provided a value for the AEA" should {
        mockCreateSummary(TestModels.summaryTrusteeTAWithAEA)
        mockCalculateTAValue(Some(TestModels.calcModelOneRate))
        object SummaryTestDataItem extends fakeRequestTo("summary", TestCalculationController.summary)

        "have an election description of time apportionment method" in {
          mockCreateSummary(TestModels.summaryTrusteeTAWithAEA)
          mockCalculateTAValue(Some(TestModels.calcModelOneRate))
          SummaryTestDataItem.jsoupDoc.body().getElementById("calcDetails(0)").text() shouldBe Messages("calc.summary.calculation.details.timeCalculation")
        }

        "have an acquisition date of '9 September 1990'" in{
          mockCreateSummary(TestModels.summaryTrusteeTAWithAEA)
          mockCalculateTAValue(Some(TestModels.calcModelOneRate))
          SummaryTestDataItem.jsoupDoc.body().getElementById("purchaseDetails(0)").text() shouldBe ("09 September 1999")
        }

        "have a 'trustee' owner" in {
          mockCreateSummary(TestModels.summaryTrusteeTAWithAEA)
          mockCalculateTAValue(Some(TestModels.calcModelOneRate))
          SummaryTestDataItem.jsoupDoc.body().getElementById("personalDetails(0)").text() shouldBe "Trustee"
        }

        "have an answer of 'No to the disabled trustee question" in {
          mockCreateSummary(TestModels.summaryTrusteeTAWithAEA)
          mockCalculateTAValue(Some(TestModels.calcModelOneRate))
          SummaryTestDataItem.jsoupDoc.body().getElementById("personalDetails(1)").text() shouldBe "No"
        }

        "have a total taxable gain of prior disposals of £9600" in {
          mockCreateSummary(TestModels.summaryTrusteeTAWithAEA)
          mockCalculateTAValue(Some(TestModels.calcModelOneRate))
          SummaryTestDataItem.jsoupDoc.body.getElementById("personalDetails(2)").text() shouldBe "£9600.00"
        }

        "have a remaining CGT Allowance of £1500" in {
          mockCreateSummary(TestModels.summaryTrusteeTAWithAEA)
          mockCalculateTAValue(Some(TestModels.calcModelOneRate))
          SummaryTestDataItem.jsoupDoc.body().getElementById("personalDetails(3)").text() shouldBe "£1500.00"
        }

        "have a base tax rate of 20%" in {
          mockCreateSummary(TestModels.summaryTrusteeTAWithAEA)
          mockCalculateTAValue(Some(TestModels.calcModelOneRate))
          SummaryTestDataItem.jsoupDoc.body().getElementById("calcDetails(3)").text() shouldBe "20%"
        }
      }

      "the user has provided no value for the AEA" should {
        mockCreateSummary(TestModels.summaryTrusteeTAWithoutAEA)
        mockCalculateTAValue(Some(TestModels.calcModelTwoRates))
        object SummaryTestDataItem extends fakeRequestTo("summary", TestCalculationController.summary)

        "have an answer of 'No to the disabled trustee question" in {
          mockCreateSummary(TestModels.summaryTrusteeTAWithoutAEA)
          mockCalculateTAValue(Some(TestModels.calcModelTwoRates))
          SummaryTestDataItem.jsoupDoc.body().getElementById("personalDetails(1)").text() shouldBe "No"
        }

        "have a remaining CGT Allowance of £5050" in {
          mockCreateSummary(TestModels.summaryTrusteeTAWithoutAEA)
          mockCalculateTAValue(Some(TestModels.calcModelTwoRates))
          SummaryTestDataItem.jsoupDoc.body().getElementById("personalDetails(2)").text() shouldBe "£5050.00"
        }
      }
    }

    "disabled trustee is chosen with a time apportioned calculation" when {

      "the user has provided a value for the AEA" should {
        mockCreateSummary(TestModels.summaryDisabledTrusteeTAWithAEA)
        mockCalculateTAValue(Some(TestModels.calcModelTwoRates))
        object SummaryTestDataItem extends fakeRequestTo("summary", TestCalculationController.summary)

        "have an answer of 'Yes' to the disabled trustee question" in {
          mockCreateSummary(TestModels.summaryDisabledTrusteeTAWithAEA)
          mockCalculateTAValue(Some(TestModels.calcModelTwoRates))
          SummaryTestDataItem.jsoupDoc.body().getElementById("personalDetails(1)").text() shouldBe "Yes"
        }

        "have a remaining CGT Allowance of £1500" in {
          mockCreateSummary(TestModels.summaryDisabledTrusteeTAWithAEA)
          mockCalculateTAValue(Some(TestModels.calcModelTwoRates))
          SummaryTestDataItem.jsoupDoc.body().getElementById("personalDetails(3)").text() shouldBe "£1500.00"
        }
      }

      "the user has provided no value for the AEA" should {
        mockCreateSummary(TestModels.summaryDisabledTrusteeTAWithoutAEA)
        mockCalculateTAValue(Some(TestModels.calcModelTwoRates))
        object SummaryTestDataItem extends fakeRequestTo("summary", TestCalculationController.summary)

        "have an answer of 'Yes' to the disabled trustee question" in {
          mockCreateSummary(TestModels.summaryDisabledTrusteeTAWithoutAEA)
          mockCalculateTAValue(Some(TestModels.calcModelTwoRates))
          SummaryTestDataItem.jsoupDoc.body().getElementById("personalDetails(1)").text() shouldBe "Yes"
        }

        "have a remaining CGT Allowance of £11100" in {
          mockCreateSummary(TestModels.summaryDisabledTrusteeTAWithoutAEA)
          mockCalculateTAValue(Some(TestModels.calcModelTwoRates))
          SummaryTestDataItem.jsoupDoc.body().getElementById("personalDetails(2)").text() shouldBe "£11100.00"
        }
      }
    }

    "personal representative is chosen with a flat calculation" when {

      "the user has provided a value for the AEA" should {
        mockCreateSummary(TestModels.summaryRepresentativeFlatWithAEA)
        mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
        object SummaryTestDataItem extends fakeRequestTo("summary", TestCalculationController.summary)

        "have a 'Personal Representative' owner" in {
          mockCreateSummary(TestModels.summaryRepresentativeFlatWithAEA)
          mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
          SummaryTestDataItem.jsoupDoc.body().getElementById("personalDetails(0)").text() shouldBe "Personal Representative"
        }

        "have a total taxable gain of prior disposals of £9600" in {
          mockCreateSummary(TestModels.summaryRepresentativeFlatWithAEA)
          mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
          SummaryTestDataItem.jsoupDoc.body.getElementById("personalDetails(1)").text() shouldBe "£9600.00"
        }

        "have a remaining CGT Allowance of £1500" in {
          mockCreateSummary(TestModels.summaryRepresentativeFlatWithAEA)
          mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
          SummaryTestDataItem.jsoupDoc.body().getElementById("personalDetails(2)").text() shouldBe "£1500.00"
        }
      }

      "the user has provided no value for the AEA" should {
        mockCreateSummary(TestModels.summaryRepresentativeFlatWithoutAEA)
        mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
        object SummaryTestDataItem extends fakeRequestTo("summary", TestCalculationController.summary)

        "have a 'Personal Representative' owner" in {
          mockCreateSummary(TestModels.summaryRepresentativeFlatWithoutAEA)
          mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
          SummaryTestDataItem.jsoupDoc.body().getElementById("personalDetails(0)").text() shouldBe "Personal Representative"
        }

        "have a remaining CGT Allowance of £11100" in {
          mockCreateSummary(TestModels.summaryRepresentativeFlatWithoutAEA)
          mockCalculateFlatValue(Some(TestModels.calcModelTwoRates))
          SummaryTestDataItem.jsoupDoc.body().getElementById("personalDetails(1)").text() shouldBe "£11100.00"
        }
      }

    }
    "individual is chosen with a rebased calculation" when {

      "user provides no acquisition date and has two tax rates" should {
        mockCreateSummary(TestModels.summaryIndividualRebased)
        mockCalculateRebasedValue(Some(TestModels.calcModelTwoRates))
        object SummaryTestDataItem extends fakeRequestTo("summary", TestCalculationController.summary)

        "have an election description of 'How much of your total gain you've made since 5 April 2015'" in {
          mockCreateSummary(TestModels.summaryIndividualRebased)
          mockCalculateRebasedValue(Some(TestModels.calcModelTwoRates))
          SummaryTestDataItem.jsoupDoc.body().getElementById("calcDetails(0)").text() shouldBe Messages("calc.summary.calculation.details.rebasedCalculation")
        }

        "include the question for the rebased value" in {
          mockCreateSummary(TestModels.summaryIndividualRebased)
          mockCalculateRebasedValue(Some(TestModels.calcModelTwoRates))
          SummaryTestDataItem.jsoupDoc.select("#purchaseDetails").text should include(Messages("calc.rebasedValue.questionTwo"))
        }

        "have a value for the rebased value" in {
          mockCreateSummary(TestModels.summaryIndividualRebased)
          mockCalculateRebasedValue(Some(TestModels.calcModelTwoRates))
          SummaryTestDataItem.jsoupDoc.body.getElementById("purchaseDetails(1)").text() shouldBe "£150000.00"
        }

        "include the question for the rebased costs" in {
          mockCreateSummary(TestModels.summaryIndividualRebased)
          mockCalculateRebasedValue(Some(TestModels.calcModelTwoRates))
          SummaryTestDataItem.jsoupDoc.select("#purchaseDetails").text should include(Messages("calc.rebasedCosts.questionTwo"))
        }

        "have a value for the rebased costs" in {
          mockCreateSummary(TestModels.summaryIndividualRebased)
          mockCalculateRebasedValue(Some(TestModels.calcModelTwoRates))
          SummaryTestDataItem.jsoupDoc.body.getElementById("purchaseDetails(2)").text() shouldBe "£1000.00"
        }

        "include the question for the improvements before" in {
          mockCreateSummary(TestModels.summaryIndividualRebased)
          mockCalculateRebasedValue(Some(TestModels.calcModelTwoRates))
          SummaryTestDataItem.jsoupDoc.select("#propertyDetails").text should include(Messages("calc.improvements.questionThree"))
        }

        "have a value for the improvements before" in {
          mockCreateSummary(TestModels.summaryIndividualRebased)
          mockCalculateRebasedValue(Some(TestModels.calcModelTwoRates))
          SummaryTestDataItem.jsoupDoc.body.getElementById("propertyDetails(1)").text() shouldBe "£2000.00"
        }

        "include the question for the improvements after" in {
          mockCreateSummary(TestModels.summaryIndividualRebased)
          mockCalculateRebasedValue(Some(TestModels.calcModelTwoRates))
          SummaryTestDataItem.jsoupDoc.select("#propertyDetails").text should include(Messages("calc.improvements.questionFour"))
        }

        "have a value for the improvements after" in {
          mockCreateSummary(TestModels.summaryIndividualRebased)
          mockCalculateRebasedValue(Some(TestModels.calcModelTwoRates))
          SummaryTestDataItem.jsoupDoc.body.getElementById("propertyDetails(2)").text() shouldBe "£3000.00"
        }

        "have a value for the other reliefs rebased" in {
          mockCreateSummary(TestModels.summaryIndividualRebased)
          mockCalculateRebasedValue(Some(TestModels.calcModelTwoRates))
          SummaryTestDataItem.jsoupDoc.body.getElementById("deductions(2)").text() shouldBe "£777.00"
        }

      }

      "user provides no acquisition date and has one tax rate" should {
        mockCreateSummary(TestModels.summaryIndividualRebasedNoAcqDate)
        mockCalculateRebasedValue(Some(TestModels.calcModelOneRate))
        object SummaryTestDataItem extends fakeRequestTo("summary", TestCalculationController.summary)

        "have an election description of 'How much of your total gain you've made since 5 April 2015'" in {
          mockCreateSummary(TestModels.summaryIndividualRebasedNoAcqDate)
          mockCalculateRebasedValue(Some(TestModels.calcModelOneRate))
          SummaryTestDataItem.jsoupDoc.body().getElementById("calcDetails(0)").text() shouldBe Messages("calc.summary.calculation.details.rebasedCalculation")
        }

        "the value of allowable losses should be £0" in {
          mockCreateSummary(TestModels.summaryIndividualRebasedNoAcqDate)
          mockCalculateRebasedValue(Some(TestModels.calcModelOneRate))
          SummaryTestDataItem.jsoupDoc.body().getElementById("deductions(1)").text shouldBe "£0.00"
        }

        "the value of other reliefs should be £0" in {
          mockCreateSummary(TestModels.summaryIndividualRebasedNoAcqDate)
          mockCalculateRebasedValue(Some(TestModels.calcModelOneRate))
          SummaryTestDataItem.jsoupDoc.body().getElementById("deductions(2)").text shouldBe "£0.00"
        }
      }

      "user provides acquisition date and no rebased costs" should {
        mockCreateSummary(TestModels.summaryIndividualRebasedNoRebasedCosts)
        mockCalculateRebasedValue(Some(TestModels.calcModelOneRate))
        object SummaryTestDataItem extends fakeRequestTo("summary", TestCalculationController.summary)

        "have no value for the rebased costs" in {
          mockCreateSummary(TestModels.summaryIndividualRebasedNoRebasedCosts)
          mockCalculateRebasedValue(Some(TestModels.calcModelOneRate))
          SummaryTestDataItem.jsoupDoc.body.getElementById("purchaseDetails(2)").text() shouldBe "£0.00"
        }
      }

      "user provides no acquisition date and no rebased costs" should {
        mockCreateSummary(TestModels.summaryIndividualRebasedNoAcqDateOrRebasedCosts)
        mockCalculateRebasedValue(Some(TestModels.calcModelOneRate))
        object SummaryTestDataItem extends fakeRequestTo("summary", TestCalculationController.summary)

        "have no value for the rebased costs" in {
          mockCreateSummary(TestModels.summaryIndividualRebasedNoAcqDateOrRebasedCosts)
          mockCalculateRebasedValue(Some(TestModels.calcModelOneRate))
          SummaryTestDataItem.jsoupDoc.body.getElementById("purchaseDetails(1)").text() shouldBe "£0.00"
        }
      }
    }
  }

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
