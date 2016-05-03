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
    OtherPropertiesModel("No"),
    None,
    AcquisitionDateModel("Yes", Some(9), Some(9), Some(9)),
    AcquisitionValueModel(100000),
    ImprovementsModel("No", None),
    DisposalDateModel(10, 10, 2010),
    DisposalValueModel(150000),
    AcquisitionCostsModel(None),
    DisposalCostsModel(None),
    EntrepreneursReliefModel("No"),
    AllowableLossesModel("No", None),
    CalculationElectionModel("time-apportioned-calculation"),
    OtherReliefsModel(None),
    OtherReliefsModel(None)
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

    "fetch an option from keystore if it exists" in {
      val testModel = CustomerTypeModel("trustee")
      when(mockSessionCache.fetchAndGetEntry[CustomerTypeModel](Matchers.anyString())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Option(testModel)))

      lazy val result = TargetCalculatorConnector.fetchAndGetValue[CustomerTypeModel]("CustomerType")
      await(result) shouldBe Some(testModel)
    }

    "fetch a None from keystore if it does not exist" in {
      when(mockSessionCache.fetchAndGetEntry[CustomerTypeModel](Matchers.anyString())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(None))

      lazy val result = TargetCalculatorConnector.fetchAndGetValue[CustomerTypeModel]("CustomerType")
      await(result) shouldBe None
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
}