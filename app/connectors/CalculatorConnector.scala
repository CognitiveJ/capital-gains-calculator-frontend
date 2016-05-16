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
import constructors.CalculateRequestConstructor
import models._
import play.api.libs.json.Format
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpResponse}
import scala.concurrent.ExecutionContext.Implicits.global

import common.KeystoreKeys

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

  def calculateFlat(input: SummaryModel)(implicit hc: HeaderCarrier): Future[Option[CalculationResultModel]] = {
    http.GET[Option[CalculationResultModel]](s"$serviceUrl/capital-gains-calculator/calculate-flat?${
      CalculateRequestConstructor.baseCalcUrl(input)}${
      CalculateRequestConstructor.flatCalcUrlExtra(input)
    }")
  }

  def calculateTA(input: SummaryModel)(implicit hc: HeaderCarrier): Future[Option[CalculationResultModel]] = {
    http.GET[Option[CalculationResultModel]](s"$serviceUrl/capital-gains-calculator/calculate-time-apportioned?${
      CalculateRequestConstructor.baseCalcUrl(input)}${
      CalculateRequestConstructor.taCalcUrlExtra(input)
    }")
  }

  def calculateRebased(input: SummaryModel)(implicit hc: HeaderCarrier): Future[Option[CalculationResultModel]] = {
    http.GET[Option[CalculationResultModel]](s"$serviceUrl/capital-gains-calculator/calculate-rebased?${
      CalculateRequestConstructor.baseCalcUrl(input)}${
      CalculateRequestConstructor.rebasedCalcUrlExtra(input)
    }")
  }


  def createSummary(implicit hc: HeaderCarrier): Future[SummaryModel] = {
    val customerType = fetchAndGetFormData[CustomerTypeModel](KeystoreKeys.customerType).map(formData => formData.get)
    val disabledTrustee = fetchAndGetFormData[DisabledTrusteeModel](KeystoreKeys.disabledTrustee)
    val currentIncome = fetchAndGetFormData[CurrentIncomeModel](KeystoreKeys.currentIncome)
    val personalAllowance = fetchAndGetFormData[PersonalAllowanceModel](KeystoreKeys.personalAllowance)
    val otherProperties = fetchAndGetFormData[OtherPropertiesModel](KeystoreKeys.otherProperties).map(formData => formData.get)
    val annualExemptAmount = fetchAndGetFormData[AnnualExemptAmountModel](KeystoreKeys.annualExemptAmount)
    val acquisitionDate = fetchAndGetFormData[AcquisitionDateModel](KeystoreKeys.acquisitionDate).map(formData => formData.get)
    val acquisitionValue = fetchAndGetFormData[AcquisitionValueModel](KeystoreKeys.acquisitionValue).map(formData => formData.get)
    val rebasedValue = fetchAndGetFormData[RebasedValueModel](KeystoreKeys.rebasedValue)
    val rebasedCosts = fetchAndGetFormData[RebasedCostsModel](KeystoreKeys.rebasedCosts)
    val improvements = fetchAndGetFormData[ImprovementsModel](KeystoreKeys.improvements).map(formData => formData.get)
    val disposalDate = fetchAndGetFormData[DisposalDateModel](KeystoreKeys.disposalDate).map(formData => formData.get)
    val disposalValue = fetchAndGetFormData[DisposalValueModel](KeystoreKeys.disposalValue).map(formData => formData.get)
    val acquisitionCosts = fetchAndGetFormData[AcquisitionCostsModel](KeystoreKeys.acquisitionCosts).map(formData => formData.get)
    val disposalCosts = fetchAndGetFormData[DisposalCostsModel](KeystoreKeys.disposalCosts).map(formData => formData.get)
    val entrepreneursRelief = fetchAndGetFormData[EntrepreneursReliefModel](KeystoreKeys.entrepreneursRelief).map(formData => formData.get)
    val allowableLosses = fetchAndGetFormData[AllowableLossesModel](KeystoreKeys.allowableLosses).map(formData => formData.get)
    val calculationElection = fetchAndGetFormData[CalculationElectionModel](KeystoreKeys.calculationElection).map(formData => formData.getOrElse(CalculationElectionModel("")))
    val otherReliefsFlat = fetchAndGetFormData[OtherReliefsModel](KeystoreKeys.otherReliefsFlat).map(formData => formData.getOrElse(OtherReliefsModel(None)))
    val otherReliefsTA = fetchAndGetFormData[OtherReliefsModel](KeystoreKeys.otherReliefsTA).map(formData => formData.getOrElse(OtherReliefsModel(None)))
    val otherReliefsRebased = fetchAndGetFormData[OtherReliefsModel](KeystoreKeys.otherReliefsRebased).map(formData => formData.getOrElse(OtherReliefsModel(None)))
    val privateResidenceRelief = fetchAndGetFormData[PrivateResidenceReliefModel](KeystoreKeys.privateResidenceRelief).map(formData => formData.get)

    for {
      customerTypeModel <- customerType
      disabledTrusteeModel <- disabledTrustee
      currentIncomeModel <- currentIncome
      personalAllowanceModel <- personalAllowance
      otherPropertiesModel <- otherProperties
      annualExemptAmountModel <- annualExemptAmount
      acquisitionDateModel <- acquisitionDate
      acquisitionValueModel <- acquisitionValue
      rebasedValueModel <- rebasedValue
      rebasedCostsModel <- rebasedCosts
      improvementsModel <- improvements
      disposalDateModel <- disposalDate
      disposalValueModel <- disposalValue
      acquisitionCostsModel <- acquisitionCosts
      disposalCostsModel <- disposalCosts
      entrepreneursReliefModel <- entrepreneursRelief
      allowableLossesModel <- allowableLosses
      calculationElectionModel <- calculationElection
      otherReliefsFlatModel <- otherReliefsFlat
      otherReliefsTAModel <- otherReliefsTA
      otherReliefsRebasedModel <- otherReliefsRebased
      privateResidenceReliefModel <- privateResidenceRelief
    } yield SummaryModel(customerTypeModel, disabledTrusteeModel, currentIncomeModel, personalAllowanceModel, otherPropertiesModel,
      annualExemptAmountModel, acquisitionDateModel, acquisitionValueModel, rebasedValueModel, rebasedCostsModel, improvementsModel,
      disposalDateModel, disposalValueModel, acquisitionCostsModel, disposalCostsModel, entrepreneursReliefModel, allowableLossesModel,
      calculationElectionModel, otherReliefsFlatModel, otherReliefsTAModel, otherReliefsRebasedModel, privateResidenceReliefModel)
  }

}