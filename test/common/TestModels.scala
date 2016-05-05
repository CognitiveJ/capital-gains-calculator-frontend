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

package common

import models._

object TestModels {

  val calcModelTwoRates = CalculationResultModel(8000, 40000, 32000, 18, Some(8000), Some(28))
  val calcModelOneRate = CalculationResultModel(8000, 40000, 32000, 20, None, None)

  val summaryIndividualFlatWithoutAEA = SummaryModel (
    CustomerTypeModel("individual"),
    None,
    Some(CurrentIncomeModel(1000)),
    Some(PersonalAllowanceModel(9000)),
    OtherPropertiesModel("No"),
    None,
    AcquisitionDateModel("No", None, None, None),
    AcquisitionValueModel(100000),
    ImprovementsModel("Yes", Some(8000)),
    DisposalDateModel(10, 10, 2010),
    DisposalValueModel(150000),
    AcquisitionCostsModel(Some(300)),
    DisposalCostsModel(Some(600)),
    EntrepreneursReliefModel("No"),
    AllowableLossesModel("Yes", Some(50000)),
    CalculationElectionModel("flat"),
    OtherReliefsModel(Some(999)),
    OtherReliefsModel(Some(888))
  )

  val summaryIndividualFlatWithAEA = SummaryModel (
    CustomerTypeModel("individual"),
    None,
    Some(CurrentIncomeModel(1000)),
    Some(PersonalAllowanceModel(9000)),
    OtherPropertiesModel("Yes"),
    Some(AnnualExemptAmountModel(1500)),
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

  val summaryTrusteeTAWithAEA = SummaryModel (
    CustomerTypeModel("trustee"),
    Some(DisabledTrusteeModel("No")),
    None,
    None,
    OtherPropertiesModel("Yes"),
    Some(AnnualExemptAmountModel(1500)),
    AcquisitionDateModel("Yes", Some(9), Some(9), Some(1999)),
    AcquisitionValueModel(100000),
    ImprovementsModel("No", None),
    DisposalDateModel(10, 10, 2010),
    DisposalValueModel(150000),
    AcquisitionCostsModel(None),
    DisposalCostsModel(None),
    EntrepreneursReliefModel("No"),
    AllowableLossesModel("No", None),
    CalculationElectionModel("time"),
    OtherReliefsModel(None),
    OtherReliefsModel(None)
  )

  val summaryTrusteeTAWithoutAEA = SummaryModel (
    CustomerTypeModel("trustee"),
    Some(DisabledTrusteeModel("No")),
    None,
    None,
    OtherPropertiesModel("No"),
    None,
    AcquisitionDateModel("Yes", Some(9), Some(9), Some(1999)),
    AcquisitionValueModel(100000),
    ImprovementsModel("Yes", Some(8000)),
    DisposalDateModel(10, 10, 2010),
    DisposalValueModel(150000),
    AcquisitionCostsModel(Some(300)),
    DisposalCostsModel(None),
    EntrepreneursReliefModel("No"),
    AllowableLossesModel("Yes", Some(50000)),
    CalculationElectionModel("time"),
    OtherReliefsModel(Some(999)),
    OtherReliefsModel(Some(888))
  )

  val summaryDisabledTrusteeTAWithAEA = SummaryModel (
    CustomerTypeModel("trustee"),
    Some(DisabledTrusteeModel("Yes")),
    None,
    None,
    OtherPropertiesModel("Yes"),
    Some(AnnualExemptAmountModel(1500)),
    AcquisitionDateModel("Yes", Some(9), Some(9), Some(1999)),
    AcquisitionValueModel(100000),
    ImprovementsModel("No", None),
    DisposalDateModel(10, 10, 2010),
    DisposalValueModel(150000),
    AcquisitionCostsModel(None),
    DisposalCostsModel(None),
    EntrepreneursReliefModel("No"),
    AllowableLossesModel("No", None),
    CalculationElectionModel("time"),
    OtherReliefsModel(None),
    OtherReliefsModel(None)
  )

  val summaryDisabledTrusteeTAWithoutAEA = SummaryModel (
    CustomerTypeModel("trustee"),
    Some(DisabledTrusteeModel("Yes")),
    None,
    None,
    OtherPropertiesModel("No"),
    None,
    AcquisitionDateModel("Yes", Some(9), Some(9), Some(1999)),
    AcquisitionValueModel(100000),
    ImprovementsModel("Yes", Some(8000)),
    DisposalDateModel(10, 10, 2010),
    DisposalValueModel(150000),
    AcquisitionCostsModel(Some(300)),
    DisposalCostsModel(None),
    EntrepreneursReliefModel("No"),
    AllowableLossesModel("Yes", Some(50000)),
    CalculationElectionModel("time"),
    OtherReliefsModel(Some(999)),
    OtherReliefsModel(Some(888))
  )

  val summaryRepresentativeFlatWithoutAEA = SummaryModel (
    CustomerTypeModel("personalRep"),
    None,
    None,
    None,
    OtherPropertiesModel("No"),
    None,
    AcquisitionDateModel("No", None, None, None),
    AcquisitionValueModel(100000),
    ImprovementsModel("Yes", Some(8000)),
    DisposalDateModel(10, 10, 2010),
    DisposalValueModel(150000),
    AcquisitionCostsModel(Some(300)),
    DisposalCostsModel(None),
    EntrepreneursReliefModel("No"),
    AllowableLossesModel("Yes", Some(50000)),
    CalculationElectionModel("flat"),
    OtherReliefsModel(Some(999)),
    OtherReliefsModel(Some(888))
  )

  val summaryRepresentativeFlatWithAEA = SummaryModel (
    CustomerTypeModel("personalRep"),
    None,
    None,
    None,
    OtherPropertiesModel("Yes"),
    Some(AnnualExemptAmountModel(1500)),
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

  val summaryIndividualAcqDateAfter = SummaryModel (
    CustomerTypeModel("individual"),
    None,
    Some(CurrentIncomeModel(1000)),
    Some(PersonalAllowanceModel(9000)),
    OtherPropertiesModel("No"),
    None,
    AcquisitionDateModel("Yes", Some(6), Some(6), Some(2016)),
    AcquisitionValueModel(100000),
    ImprovementsModel("Yes", Some(8000)),
    DisposalDateModel(10, 10, 2010),
    DisposalValueModel(150000),
    AcquisitionCostsModel(Some(300)),
    DisposalCostsModel(Some(600)),
    EntrepreneursReliefModel("No"),
    AllowableLossesModel("Yes", Some(50000)),
    CalculationElectionModel("flat"),
    OtherReliefsModel(Some(999)),
    OtherReliefsModel(Some(888))
  )
}
