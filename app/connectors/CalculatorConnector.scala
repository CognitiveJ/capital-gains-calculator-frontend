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

  def calculate(data: SummaryModel)(implicit hc: HeaderCarrier): Future[Option[CalculationResultModel]] = {
//    http.GET[Option[CalculationResultModel]](s"$serviceUrl/capital-gains-calculator/calculate?data=$data")
    Future.successful(Some(new CalculationResultModel(8000, 40000, 32000, 18, Some(8000), Some(28))))
  }

  def createSummary(implicit hc: HeaderCarrier): SummaryModel = {
    SummaryModel(
      fetchAndGetValue[CustomerTypeModel]("customerType").getOrElse(CustomerTypeModel("null")),
      fetchAndGetValue[DisabledTrusteeModel]("disabledTrustee"),
      fetchAndGetValue[CurrentIncomeModel]("currentIncome"),
      fetchAndGetValue[PersonalAllowanceModel]("personalAllowance"),
      fetchAndGetValue[OtherPropertiesModel]("otherProperties").getOrElse(OtherPropertiesModel("No")),
      fetchAndGetValue[AnnualExemptAmountModel]("annualExemptAmount"),
      fetchAndGetValue[AcquisitionValueModel]("acquisitionValue").getOrElse(AcquisitionValueModel(0)),
      fetchAndGetValue[ImprovementsModel]("improvements").getOrElse(ImprovementsModel("No", None)),
      fetchAndGetValue[DisposalDateModel]("disposalDate").getOrElse(DisposalDateModel(1, 1, 1900)),
      fetchAndGetValue[DisposalValueModel]("disposalValue").getOrElse(DisposalValueModel(0)),
      fetchAndGetValue[AcquisitionCostsModel]("acquisitionCosts").getOrElse(AcquisitionCostsModel(None)),
      fetchAndGetValue[DisposalCostsModel]("disposalCosts").getOrElse(DisposalCostsModel(None)),
      fetchAndGetValue[EntrepreneursReliefModel]("entrepreneursRelief").getOrElse(EntrepreneursReliefModel("No")),
      fetchAndGetValue[AllowableLossesModel]("allowableLosses").getOrElse(AllowableLossesModel("No", None)),
      fetchAndGetValue[OtherReliefsModel]("otherReliefs").getOrElse(OtherReliefsModel(None))
    )
  }

}