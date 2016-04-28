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

import akka.actor.Status.Success
import config.{CalculatorSessionCache, WSHttp}
import models._
import play.api.libs.json.Format
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpResponse}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object CalculatorConnector extends CalculatorConnector with ServicesConfig {
  override val sessionCache = CalculatorSessionCache
  override val http = WSHttp
  override val serviceUrl = baseUrl("capital-gains-calculator")
}

trait CalculatorConnector {

  val sessionCache: SessionCache
  val http: HttpGet
  val serviceUrl: String

  implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders("Accept" -> "application/vnd.hmrc.1.0+json")

  def saveFormData[T](key: String, data: T)(implicit hc: HeaderCarrier, formats: Format[T]): Future[CacheMap] = {
    sessionCache.cache(key, data)
  }

  def fetchAndGetFormData[T](key: String)(implicit hc: HeaderCarrier, formats: Format[T]): Future[Option[T]] = {
    sessionCache.fetchAndGetEntry(key)
  }

  def fetchAndGetValue[T](key: String)(implicit hc: HeaderCarrier, formats: Format[T]): Option[T] ={
    Await.result(fetchAndGetFormData(key).map {
      case Some(data) => Some(data)
      case None => None
      case _ => None
    }, Duration("5s"))
  }

  def calculate(input: SummaryModel)(implicit hc: HeaderCarrier): Future[Option[CalculationResultModel]] = {
    http.GET[Option[CalculationResultModel]](s"$serviceUrl/capital-gains-calculator/calculate?customerType=${
      input.customerTypeModel.customerType}&priorDisposal=${
      input.otherPropertiesModel.otherProperties}" +{
      input.annualExemptAmountModel match {
        case Some(data) => "&annualExemptAmount=" + Some(data.annualExemptAmount)
        case None => ""
      }
      } + {
      input.disabledTrusteeModel match {
        case Some(data) => "&isVulnerable=" + Some(data.isVulnerable)
        case None => ""
      }
    } + {
      input.currentIncomeModel match {
        case Some(data) => "&currentIncome=" + data.currentIncome
        case None => ""
      }
    } + {
      input.personalAllowanceModel match {
        case Some(data) => "&personalAllowanceAmt=" + data.personalAllowanceAmt
        case None => ""
      }
    } + "&disposalValue=" + {
      input.disposalValueModel.disposalValue
    } + "&disposalCosts=" + {
      input.disposalCostsModel.disposalCosts.getOrElse(0)
    } + "&acquisitionValueAmt=" + {
      input.acquisitionValueModel.acquisitionValueAmt
    } + "&acquisitionCostsAmt=" + {
      input.acquisitionCostsModel.acquisitionCostsAmt.getOrElse(0)
    } + "&improvementsAmt=" + {
      input.improvementsModel.improvementsAmt.getOrElse(0)
    } + "&reliefs=" +{
      input.otherReliefsModelFlat.otherReliefs.getOrElse(0)
    } + "&allowableLossesAmt=" +{
      input.allowableLossesModel.allowableLossesAmt.getOrElse(0)
    } + "&entReliefClaimed=" +{
      input.entrepreneursReliefModel.entReliefClaimed
    })
//    Future.successful(Some(new CalculationResultModel(8000, 40000, 32000, 18, Some(8000), Some(28))))
  }

  // $COVERAGE-OFF$
  def createSummary(implicit hc: HeaderCarrier): SummaryModel = {
    SummaryModel(
      fetchAndGetValue[CustomerTypeModel]("customerType").getOrElse(CustomerTypeModel("null")),
      fetchAndGetValue[DisabledTrusteeModel]("disabledTrustee"),
      fetchAndGetValue[CurrentIncomeModel]("currentIncome"),
      fetchAndGetValue[PersonalAllowanceModel]("personalAllowance"),
      fetchAndGetValue[OtherPropertiesModel]("otherProperties").getOrElse(OtherPropertiesModel("No")),
      fetchAndGetValue[AnnualExemptAmountModel]("annualExemptAmount"),
      fetchAndGetValue[AcquisitionDateModel]("acquisitionDate").getOrElse(AcquisitionDateModel("No", None, None, None)),
      fetchAndGetValue[AcquisitionValueModel]("acquisitionValue").getOrElse(AcquisitionValueModel(0)),
      fetchAndGetValue[ImprovementsModel]("improvements").getOrElse(ImprovementsModel("No", None)),
      fetchAndGetValue[DisposalDateModel]("disposalDate").getOrElse(DisposalDateModel(1, 1, 1900)),
      fetchAndGetValue[DisposalValueModel]("disposalValue").getOrElse(DisposalValueModel(0)),
      fetchAndGetValue[AcquisitionCostsModel]("acquisitionCosts").getOrElse(AcquisitionCostsModel(None)),
      fetchAndGetValue[DisposalCostsModel]("disposalCosts").getOrElse(DisposalCostsModel(None)),
      fetchAndGetValue[EntrepreneursReliefModel]("entrepreneursRelief").getOrElse(EntrepreneursReliefModel("No")),
      fetchAndGetValue[AllowableLossesModel]("allowableLosses").getOrElse(AllowableLossesModel("No", None)),
      fetchAndGetValue[CalculationElectionModel]("calculationElection").getOrElse(CalculationElectionModel("null")),
      fetchAndGetValue[OtherReliefsModel]("otherReliefsFlat").getOrElse(OtherReliefsModel(None)),
      fetchAndGetValue[OtherReliefsModel]("otherReliefsTA").getOrElse(OtherReliefsModel(None))
    )
  }
  // $COVERAGE-ON$

}