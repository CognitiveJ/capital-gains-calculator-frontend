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

import config.{CalculatorSessionCache, WSHttp}
import CalculatorSessionCache._
import models.CustomerTypeModel
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.{SessionCache, CacheMap}
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class CalculatorConnectorSpec extends UnitSpec with MockitoSugar {

  lazy val mockSessionCache  = mock[SessionCache]
  lazy val sessionId = UUID.randomUUID.toString

  object TestCalculatorConnector extends CalculatorConnector{
    lazy val sessionCache = mockSessionCache
  }

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(sessionId.toString)))

  "Keystore Connector" should {

    "fetch and get" in {
      val testModel = CustomerTypeModel("trustee")
      when(mockSessionCache.fetchAndGetEntry[CustomerTypeModel](Matchers.anyString())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Option(testModel)))
      lazy val result = TestCalculatorConnector.fetchAndGetFormData[CustomerTypeModel]("customerType")
      await(result) shouldBe Some(testModel)
    }

    "save data to keystore" in {
      val testModel = CustomerTypeModel("trustee")
      val returnedCacheMap = CacheMap("customerType", Map("data" -> Json.toJson(testModel)))
      when(mockSessionCache.cache[CustomerTypeModel](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(returnedCacheMap))
      lazy val result = TestCalculatorConnector.saveFormData("customerType", testModel)
      await(result) shouldBe returnedCacheMap
    }
  }
}
