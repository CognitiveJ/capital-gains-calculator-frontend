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

package models

import play.api.libs.json.Json

case class SummaryModel (
                          customerTypeModel: CustomerTypeModel,
                          disabledTrusteeModel: Option[DisabledTrusteeModel],
                          currentIncomeModel : Option[CurrentIncomeModel],
                          personalAllowanceModel: Option[PersonalAllowanceModel],
                          otherPropertiesModel: OtherPropertiesModel,
                          annualExemptAmountModel: Option[AnnualExemptAmountModel],
                          acquisitionDateModel: AcquisitionDateModel,
                          acquisitionValueModel: AcquisitionValueModel,
                          rebasedValueModel: Option[RebasedValueModel],
                          rebasedCostsModel: Option[RebasedCostsModel],
                          improvementsModel: ImprovementsModel,
                          disposalDateModel: DisposalDateModel,
                          disposalValueModel: DisposalValueModel,
                          acquisitionCostsModel: AcquisitionCostsModel,
                          disposalCostsModel : DisposalCostsModel,
                          entrepreneursReliefModel : EntrepreneursReliefModel,
                          allowableLossesModel : AllowableLossesModel,
                          calculationElectionModel: CalculationElectionModel,
                          otherReliefsModelFlat : OtherReliefsModel,
                          otherReliefsModelTA: OtherReliefsModel,
                          otherReliefsModelRebased: OtherReliefsModel,
                          privateResidenceReliefModel: PrivateResidenceReliefModel
                        )

object SummaryModel {
  implicit val format = Json.format[SummaryModel]
}

