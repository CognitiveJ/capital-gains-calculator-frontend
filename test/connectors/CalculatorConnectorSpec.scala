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

package connectors

import java.util.UUID

import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet}
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent
import scala.concurrent.Future

class CalculatorConnectorSpec extends UnitSpec with MockitoSugar {

  val mockHttp = mock[HttpGet]
  val mockSessionCache = mock[SessionCache]
  val sessionId = UUID.randomUUID.toString

  object TargetCalculatorConnector extends CalculatorConnector {
    override val sessionCache = mockSessionCache
    override val http = mockHttp
    override val serviceUrl = "dummy"
  }

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(sessionId.toString)))

  def mockFetchAndGetFormData (summary: SummaryModel, calculationElectionModel: Option[CalculationElectionModel], otherReliefsModel: Option[OtherReliefsModel]) = {
    when(mockSessionCache.fetchAndGetEntry[CustomerTypeModel](Matchers.eq("customerType"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(Some(summary.customerTypeModel)))

    when(mockSessionCache.fetchAndGetEntry[DisabledTrusteeModel](Matchers.eq("disabledTrustee"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(summary.disabledTrusteeModel))

    when(mockSessionCache.fetchAndGetEntry[CurrentIncomeModel](Matchers.eq("currentIncome"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(summary.currentIncomeModel))

    when(mockSessionCache.fetchAndGetEntry[PersonalAllowanceModel](Matchers.eq("personalAllowance"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(summary.personalAllowanceModel))

    when(mockSessionCache.fetchAndGetEntry[OtherPropertiesModel](Matchers.eq("otherProperties"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(Some(summary.otherPropertiesModel)))

    when(mockSessionCache.fetchAndGetEntry[AnnualExemptAmountModel](Matchers.eq("annualExemptAmount"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(summary.annualExemptAmountModel))

    when(mockSessionCache.fetchAndGetEntry[AcquisitionDateModel](Matchers.eq("acquisitionDate"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(Some(summary.acquisitionDateModel)))

    when(mockSessionCache.fetchAndGetEntry[AcquisitionValueModel](Matchers.eq("acquisitionValue"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(Some(summary.acquisitionValueModel)))

    when(mockSessionCache.fetchAndGetEntry[RebasedValueModel](Matchers.eq("rebasedValue"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(summary.rebasedValueModel))

    when(mockSessionCache.fetchAndGetEntry[RebasedCostsModel](Matchers.eq("rebasedCosts"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(summary.rebasedCostsModel))

    when(mockSessionCache.fetchAndGetEntry[ImprovementsModel](Matchers.eq("improvements"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(Some(summary.improvementsModel)))

    when(mockSessionCache.fetchAndGetEntry[DisposalDateModel](Matchers.eq("disposalDate"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(Some(summary.disposalDateModel)))

    when(mockSessionCache.fetchAndGetEntry[DisposalValueModel](Matchers.eq("disposalValue"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(Some(summary.disposalValueModel)))

    when(mockSessionCache.fetchAndGetEntry[AcquisitionCostsModel](Matchers.eq("acquisitionCosts"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(Some(summary.acquisitionCostsModel)))

    when(mockSessionCache.fetchAndGetEntry[DisposalCostsModel](Matchers.eq("disposalCosts"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(Some(summary.disposalCostsModel)))

    when(mockSessionCache.fetchAndGetEntry[EntrepreneursReliefModel](Matchers.eq("entrepreneursRelief"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(Some(summary.entrepreneursReliefModel)))

    when(mockSessionCache.fetchAndGetEntry[AllowableLossesModel](Matchers.eq("allowableLosses"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(Some(summary.allowableLossesModel)))

    when(mockSessionCache.fetchAndGetEntry[CalculationElectionModel](Matchers.eq("calculationElection"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(calculationElectionModel))

    when(mockSessionCache.fetchAndGetEntry[OtherReliefsModel](Matchers.eq("otherReliefsFlat"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(otherReliefsModel))

    when(mockSessionCache.fetchAndGetEntry[OtherReliefsModel](Matchers.eq("otherReliefsTA"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(otherReliefsModel))

    when(mockSessionCache.fetchAndGetEntry[OtherReliefsModel](Matchers.eq("otherReliefsRebased"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(otherReliefsModel))

    when(mockSessionCache.fetchAndGetEntry[PrivateResidenceReliefModel](Matchers.eq("privateResidenceRelief"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(Some(summary.privateResidenceReliefModel)))
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
    OtherReliefsModel(None),
    PrivateResidenceReliefModel("No", None, None)
  )

  val sumModelTA = SummaryModel(
    CustomerTypeModel("individual"),
    None,
    Some(CurrentIncomeModel(1000)),
    Some(PersonalAllowanceModel(11100)),
    OtherPropertiesModel("No", None),
    None,
    AcquisitionDateModel("Yes", Some(9), Some(9), Some(9)),
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
    CalculationElectionModel("time-apportioned-calculation"),
    OtherReliefsModel(None),
    OtherReliefsModel(None),
    OtherReliefsModel(None),
    PrivateResidenceReliefModel("No", None, None)
  )

  val sumModelRebased = SummaryModel(
    CustomerTypeModel("individual"),
    None,
    Some(CurrentIncomeModel(1000)),
    Some(PersonalAllowanceModel(11100)),
    OtherPropertiesModel("No", None),
    None,
    AcquisitionDateModel("Yes", Some(9), Some(9), Some(9)),
    AcquisitionValueModel(100000),
    Some(RebasedValueModel("Yes", Some(1000))),
    Some(RebasedCostsModel("No", None)),
    ImprovementsModel("No", None),
    DisposalDateModel(10, 10, 2010),
    DisposalValueModel(150000),
    AcquisitionCostsModel(None),
    DisposalCostsModel(None),
    EntrepreneursReliefModel("No"),
    AllowableLossesModel("No", None),
    CalculationElectionModel("rebased"),
    OtherReliefsModel(None),
    OtherReliefsModel(None),
    OtherReliefsModel(None),
    PrivateResidenceReliefModel("No", None, None)
  )

  val sumModelFlatDefaulted = SummaryModel(
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
    CalculationElectionModel(""),
    OtherReliefsModel(None),
    OtherReliefsModel(None),
    OtherReliefsModel(None),
    PrivateResidenceReliefModel("No", None, None)
  )

  "Calculator Connector" should {

    "fetch and get from keystore" in {
      val testModel = CustomerTypeModel("trustee")
      when(mockSessionCache.fetchAndGetEntry[CustomerTypeModel](Matchers.anyString())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Option(testModel)))

      lazy val result = TargetCalculatorConnector.fetchAndGetFormData[CustomerTypeModel]("customerType")
      await(result) shouldBe Some(testModel)
    }

    "save data to keystore" in {
      val testModel = CustomerTypeModel("trustee")
      val returnedCacheMap = CacheMap("customerType", Map("data" -> Json.toJson(testModel)))
      when(mockSessionCache.cache[CustomerTypeModel](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(returnedCacheMap))

      lazy val result = TargetCalculatorConnector.saveFormData("customerType", testModel)
      await(result) shouldBe returnedCacheMap
    }
  }

  "Calling calculateFlat" should {

    val validResponse = CalculationResultModel(8000, 40000, 32000, 18, Some(8000), Some(28))
    when(mockHttp.GET[Option[CalculationResultModel]](Matchers.anyString())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(Some(validResponse)))

    "return a valid response" in {
      val testModel: SummaryModel = sumModelFlat
      val result = TargetCalculatorConnector.calculateFlat(testModel)
      await(result) shouldBe Some(validResponse)
    }
  }

  "Calling calculateTA" should {
    val validResponse = CalculationResultModel(8000, 40000, 32000, 18, Some(8000), Some(28))
    when(mockHttp.GET[Option[CalculationResultModel]](Matchers.anyString())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(Some(validResponse)))

    "return a valid response" in {
      val testModel: SummaryModel = sumModelTA
      val result = TargetCalculatorConnector.calculateTA(testModel)
      await(result) shouldBe Some(validResponse)
    }
  }

  "Calling calculateRebased" should {
    val validResponse = CalculationResultModel(8000, 40000, 32000, 18, Some(8000), Some(28))
    when(mockHttp.GET[Option[CalculationResultModel]](Matchers.anyString())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(Some(validResponse)))

    "return a valid response" in {
      val testModel: SummaryModel = sumModelRebased
      val result = TargetCalculatorConnector.calculateRebased(testModel)
      await(result) shouldBe Some(validResponse)
    }
  }

  "Calling create summary" should {

    "produce a non-empty summary with calculation selection or reliefs provided" in {
      mockFetchAndGetFormData(sumModelFlat, Some(CalculationElectionModel("flat")), Some(OtherReliefsModel(None)))
      lazy val result = TargetCalculatorConnector.createSummary
      await(result) shouldBe sumModelFlat
    }

    "produce a non-empty summary without calculation selection or reliefs provided" in {
      mockFetchAndGetFormData(sumModelFlat, None, None)
      lazy val result = TargetCalculatorConnector.createSummary
      await(result) shouldBe sumModelFlatDefaulted
    }
  }
}