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

package constructors

import common.TestModels
import connectors.CalculatorConnector
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class CalculationElectionConstructorSpec extends UnitSpec with MockitoSugar {

  object TestCalculationElectionConstructor extends CalculationElectionConstructor {
    val calcConnector = mockCalcConnector
  }

  implicit val hc = new HeaderCarrier()
  val mockCalcConnector = mock[CalculatorConnector]
  def mockFlatCalc = when(mockCalcConnector.calculateFlat(Matchers.any())(Matchers.any()))
    .thenReturn(Future.successful(Some(CalculationResultModel(8000, 10000, 6000, 20, None, None))))

  def mockTimeCalc = when(mockCalcConnector.calculateTA(Matchers.any())(Matchers.any()))
    .thenReturn(Future.successful(Some(CalculationResultModel(8000, 10000, 6000, 20, None, None))))

  def mockRebasedCalc = when(mockCalcConnector.calculateRebased(Matchers.any())(Matchers.any()))
    .thenReturn(Future.successful(Some(CalculationResultModel(8000, 10000, 6000, 20, None, None))))

  "Calling generateElection" should {

    "when summary has no acquisition date" should {

      "produce a single entry sequence" in {
        mockFlatCalc
        TestCalculationElectionConstructor.generateElection(TestModels.summaryIndividualFlatWithoutAEA, hc).size shouldBe 1
      }
    }

    "when summary has an acquisition date after the tax start date" should {

      "produce a single entry sequence" in {
        mockFlatCalc
        TestCalculationElectionConstructor.generateElection(TestModels.summaryIndividualAcqDateAfter, hc).size shouldBe 1
      }
    }

    "when summary has an acquisition date before the tax start date" should {

      "produce a two entry sequence if there is no value for rebased supplied." in {
        mockFlatCalc
        mockTimeCalc
        TestCalculationElectionConstructor.generateElection(TestModels.summaryTrusteeTAWithoutAEA, hc).size shouldBe 2
      }

      "produce a three entry sequence if there is a value for the rebased calculation supplied." in {
        mockFlatCalc
        mockTimeCalc
        mockRebasedCalc
        TestCalculationElectionConstructor.generateElection(TestModels.summaryIndividualRebased, hc).size shouldBe 3
      }

      "produce a two entry sequence if there is a value for the rebased calculation supplied but the acquisition date is not supplied." in {
        mockFlatCalc
        mockRebasedCalc
        TestCalculationElectionConstructor.generateElection(TestModels.summaryIndividualRebasedNoAcqDate, hc).size shouldBe 2
      }
    }
  }
}
