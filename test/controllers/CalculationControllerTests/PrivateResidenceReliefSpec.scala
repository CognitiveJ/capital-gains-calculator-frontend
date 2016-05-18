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

package controllers.CalculationControllerTests

import connectors.CalculatorConnector
import constructors.CalculationElectionConstructor
import models._
import controllers.{routes, CalculationController}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future

class PrivateResidenceReliefSpec extends UnitSpec with WithFakeApplication with MockitoSugar {

  implicit val hc = new HeaderCarrier()

  def setupTarget
  (
    getData: Option[PrivateResidenceReliefModel],
    postData: Option[PrivateResidenceReliefModel],
    disposalDateData: Option[DisposalDateModel] = None,
    acquisitionDateData: Option[AcquisitionDateModel] = None,
    rebasedValueData: Option[RebasedValueModel] = None
  ): CalculationController = {

    val mockCalcConnector = mock[CalculatorConnector]
    val mockCalcElectionConstructor = mock[CalculationElectionConstructor]

    when(mockCalcConnector.fetchAndGetFormData[PrivateResidenceReliefModel](Matchers.eq("privateResidenceRelief"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(getData))

    when(mockCalcConnector.fetchAndGetFormData[DisposalDateModel](Matchers.eq("disposalDate"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(disposalDateData))

    when(mockCalcConnector.fetchAndGetFormData[AcquisitionDateModel](Matchers.eq("acquisitionDate"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(acquisitionDateData))

    when(mockCalcConnector.fetchAndGetFormData[RebasedValueModel](Matchers.eq("rebasedValue"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(rebasedValueData))

    lazy val data = CacheMap("form-id", Map("data" -> Json.toJson(postData.getOrElse(PrivateResidenceReliefModel("", None)))))
    when(mockCalcConnector.saveFormData[PrivateResidenceReliefModel](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(data))

    new CalculationController {
      override val calcConnector: CalculatorConnector = mockCalcConnector
      override val calcElectionConstructor: CalculationElectionConstructor = mockCalcElectionConstructor
    }
  }

  //GET Tests
  "In CalculationController calling the .privateResidenceRelief action " should {

    "when not supplied wth a pre-existing stored model" should {

      lazy val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/private-residence-relief").withSession(SessionKeys.sessionId -> "12345")
      val target = setupTarget(None, None)
      lazy val result = target.privateResidenceRelief(fakeRequest)
      lazy val document = Jsoup.parse(bodyOf(result))

      "return a 200" in {
        status(result) shouldBe 200
      }

      "return some HTML that" should {

        "contain some text and use the character set utf-8" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "have a back button" in {
          document.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
        }


        "have the title 'calc.privateResidenceRelief.question'" in {
          document.title shouldEqual Messages("calc.privateResidenceRelief.question")
        }

        "have the heading 'Calculate your tax (non-residents)'" in {
          document.body.getElementsByTag("H1").text shouldEqual Messages("calc.base.pageHeading")
        }

        "have a yes no helper with hidden content and question 'calc.privateResidenceRelief.question'" in {
          document.body.getElementById("isClaimingPRR-yes").parent.text shouldBe Messages("calc.base.yes")
          document.body.getElementById("isClaimingPRR-no").parent.text shouldBe Messages("calc.base.no")
          document.body.getElementsByTag("legend").text shouldBe Messages("calc.privateResidenceRelief.question")
        }

        "Not show the Days Between question" in {
          document.body.getElementById("daysClaimedAfter") shouldEqual null
        }

        "Not show the Days Before question" in {
          document.body.getElementById("daysClaimed") shouldEqual null
        }
      }
    }

    "when supplied wth a pre-existing stored model" should {

      "when disposal date is >= 6 October 2016 with rebased value and daysClaimedAfter of 45" should {

        lazy val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/private-residence-relief").withSession(SessionKeys.sessionId -> "12345")
        val target = setupTarget(
          Some(PrivateResidenceReliefModel("Yes", None, Some(45))),
          None,
          Some(DisposalDateModel(6, 10, 2016)),
          None,
          Some(RebasedValueModel("Yes", Some(200)))
        )
        lazy val result = target.privateResidenceRelief(fakeRequest)
        lazy val document = Jsoup.parse(bodyOf(result))

        "return a 200" in {
          status(result) shouldBe 200
        }

        "return some HTML that" should {

          "have the `Yes` option of the radio button checked" in {
            document.body.getElementById("isClaimingPRR-yes").attr("checked") shouldEqual "checked"
          }

          "have an input with question 'How many days between 5 April 2015 and {x} are you claiming relief for'" in {
            document.body.getElementById("daysClaimedAfter").tagName shouldEqual "input"
            document.select("label[for=daysClaimedAfter]").text should include(Messages("calc.privateResidenceRelief.questionBetween.partOne"))
            document.select("label[for=daysClaimedAfter]").text should include(Messages("calc.privateResidenceRelief.questionBetween.partTwo"))
          }

          "have 45 days in the input field for days claimed after" in {
            document.body.getElementById("daysClaimedAfter").attr("value") shouldEqual ("45")
          }

          "Not show the Days Before question" in {
            document.body.getElementById("daysClaimed") shouldEqual null
          }
        }
      }

      "when disposal date is >= 6 October 2016, no acquisition date supplied with rebased value and daysClaimedAfter of 45" should {

        lazy val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/private-residence-relief").withSession(SessionKeys.sessionId -> "12345")
        val target = setupTarget(
          Some(PrivateResidenceReliefModel("Yes", None, Some(45))),
          None,
          Some(DisposalDateModel(6, 10, 2016)),
          Some(AcquisitionDateModel("No", None, None, None)),
          Some(RebasedValueModel("Yes", Some(200)))
        )
        lazy val result = target.privateResidenceRelief(fakeRequest)
        lazy val document = Jsoup.parse(bodyOf(result))

        "return a 200" in {
          status(result) shouldBe 200
        }

        "return some HTML that" should {

          "have the `Yes` option of the radio button checked" in {
            document.body.getElementById("isClaimingPRR-yes").attr("checked") shouldEqual "checked"
          }

          "have an input with question 'How many days between 5 April 2015 and {x} are you claiming relief for'" in {
            document.body.getElementById("daysClaimedAfter").tagName shouldEqual "input"
            document.select("label[for=daysClaimedAfter]").text should include(Messages("calc.privateResidenceRelief.questionBetween.partOne"))
            document.select("label[for=daysClaimedAfter]").text should include(Messages("calc.privateResidenceRelief.questionBetween.partTwo"))
          }

          "have 45 days in the input field for days claimed after" in {
            document.body.getElementById("daysClaimedAfter").attr("value") shouldEqual ("45")
          }

          "Not show the Days Before question" in {
            document.body.getElementById("daysClaimed") shouldEqual null
          }
        }
      }

      "when disposal date is >= 6 October 2016 and no rebased value" should {
        //This is an impossible scenario, as the Private Residence Relief screen would not be routed to, included for completenetss

        lazy val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/private-residence-relief").withSession(SessionKeys.sessionId -> "12345")
        val target = setupTarget(
          Some(PrivateResidenceReliefModel("Yes", None, None)),
          None,
          Some(DisposalDateModel(6, 4, 2016)),
          None,
          Some(RebasedValueModel("No", None))
        )
        lazy val result = target.privateResidenceRelief(fakeRequest)
        lazy val document = Jsoup.parse(bodyOf(result))

        "return a 200" in {
          status(result) shouldBe 200
        }

        "return some HTML that" should {

          "have the `Yes` option of the radio button checked" in {
            document.body.getElementById("isClaimingPRR-yes").attr("checked") shouldEqual "checked"
          }

          "Not show the Days Between question" in {
            document.body.getElementById("daysClaimedAfter") shouldEqual null
          }

          "Not show the Days Before question" in {
            document.body.getElementById("daysClaimed") shouldEqual null
          }
        }
      }

      "when disposal date is >= 6 October 2016, acquisition date < 6 April 2015 " +
        "with rebased value, daysClaimed of 23 daysClaimedAfter of 45" should {

        lazy val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/private-residence-relief").withSession(SessionKeys.sessionId -> "12345")
        val target = setupTarget(
          Some(PrivateResidenceReliefModel("Yes", Some(23), Some(45))),
          None,
          Some(DisposalDateModel(6, 10, 2016)),
          Some(AcquisitionDateModel("Yes", Some(5), Some(6), Some(2015))),
          Some(RebasedValueModel("Yes", Some(200)))
        )
        lazy val result = target.privateResidenceRelief(fakeRequest)
        lazy val document = Jsoup.parse(bodyOf(result))

        "return a 200" in {
          status(result) shouldBe 200
        }

        "return some HTML that" should {

          "have the `Yes` option of the radio button checked" in {
            document.body.getElementById("isClaimingPRR-yes").attr("checked") shouldEqual "checked"
          }

          "have an input with question 'How many days between 5 April 2015 and {x} are you claiming relief for'" in {
            document.body.getElementById("daysClaimedAfter").tagName shouldEqual "input"
            document.select("label[for=daysClaimedAfter]").text should include(Messages("calc.privateResidenceRelief.questionBetween.partOne"))
            document.select("label[for=daysClaimedAfter]").text should include(Messages("calc.privateResidenceRelief.questionBetween.partTwo"))
          }

          "have 45 days in the input field for days claimed after" in {
            document.body.getElementById("daysClaimedAfter").attr("value") shouldEqual "45"
          }

          "have an input with question 'How many days before 5 April 2015 are you claiming relief for'" in {
            document.body.getElementById("daysClaimed").tagName shouldEqual "input"
            document.select("label[for=daysClaimed]").text should include(Messages("calc.privateResidenceRelief.questionBefore.partOne"))
            document.select("label[for=daysClaimed]").text should include(Messages("calc.privateResidenceRelief.questionBefore.partTwo"))
          }

          "have 23 days in the input field for days claimed" in {
            document.body.getElementById("daysClaimed").attr("value") shouldEqual "23"
          }
        }
      }

      "when disposal date is >= 6 October 2016, acquisition < 6 April 2015 " +
        "with no rebased value, daysClaimed of 23 daysClaimedAfter of 45" should {

        lazy val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/private-residence-relief").withSession(SessionKeys.sessionId -> "12345")
        val target = setupTarget(
          Some(PrivateResidenceReliefModel("Yes", Some(23), Some(45))),
          None,
          Some(DisposalDateModel(6, 10, 2016)),
          Some(AcquisitionDateModel("Yes", Some(5), Some(4), Some(2015))),
          None
        )
        lazy val result = target.privateResidenceRelief(fakeRequest)
        lazy val document = Jsoup.parse(bodyOf(result))

        "return a 200" in {
          status(result) shouldBe 200
        }

        "return some HTML that" should {

          "have the `Yes` option of the radio button checked" in {
            document.body.getElementById("isClaimingPRR-yes").attr("checked") shouldEqual "checked"
          }

          "have an input with question 'How many days between 5 April 2015 and {x} are you claiming relief for'" in {
            document.body.getElementById("daysClaimedAfter").tagName shouldEqual "input"
            document.select("label[for=daysClaimedAfter]").text should include(Messages("calc.privateResidenceRelief.questionBetween.partOne"))
            document.select("label[for=daysClaimedAfter]").text should include(Messages("calc.privateResidenceRelief.questionBetween.partTwo"))
          }

          "have 45 days in the input field for days claimed after" in {
            document.body.getElementById("daysClaimedAfter").attr("value") shouldEqual "45"
          }

          "have an input with question 'How many days before 5 April 2015 are you claiming relief for'" in {
            document.body.getElementById("daysClaimed").tagName shouldEqual "input"
            document.select("label[for=daysClaimed]").text should include(Messages("calc.privateResidenceRelief.questionBefore.partOne"))
            document.select("label[for=daysClaimed]").text should include(Messages("calc.privateResidenceRelief.questionBefore.partTwo"))
          }

          "have 23 days in the input field for days claimed" in {
            document.body.getElementById("daysClaimed").attr("value") shouldEqual "23"
          }
        }
      }

      "when disposal date is >= 6 October 2016, acquisition date >= 6 April 2015 " +
        "with no rebased value, daysClaimed of 23" should {

        lazy val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/private-residence-relief").withSession(SessionKeys.sessionId -> "12345")
        val target = setupTarget(
          Some(PrivateResidenceReliefModel("Yes", Some(23), Some(45))),
          None,
          Some(DisposalDateModel(6, 10, 2016)),
          Some(AcquisitionDateModel("Yes", Some(6), Some(4), Some(2015))),
          Some(RebasedValueModel("No", None))
        )
        lazy val result = target.privateResidenceRelief(fakeRequest)
        lazy val document = Jsoup.parse(bodyOf(result))

        "return a 200" in {
          status(result) shouldBe 200
        }

        "return some HTML that" should {

          "have the `Yes` option of the radio button checked" in {
            document.body.getElementById("isClaimingPRR-yes").attr("checked") shouldEqual "checked"
          }

          "have an input with question 'How many days before 5 April 2015 are you claiming relief for'" in {
            document.body.getElementById("daysClaimed").tagName shouldEqual "input"
            document.select("label[for=daysClaimed]").text should include(Messages("calc.privateResidenceRelief.questionBefore.partOne"))
            document.select("label[for=daysClaimed]").text should include(Messages("calc.privateResidenceRelief.questionBefore.partTwo"))
          }

          "have 23 days in the input field for days claimed" in {
            document.body.getElementById("daysClaimed").attr("value") shouldEqual "23"
          }

          "Not show the Days Between question" in {
            document.body.getElementById("daysClaimedAfter") shouldEqual null
          }
        }

        s"have a 'Back' link to ${routes.CalculationController.disposalCosts}" in {
          document.body.getElementById("back-link").text shouldEqual Messages("calc.base.back")
          document.body.getElementById("back-link").attr("href") shouldEqual routes.CalculationController.disposalCosts.toString()
        }

        "when disposal date is < 6 October 2016, no acquisition date with rebased value" should {

          lazy val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/private-residence-relief").withSession(SessionKeys.sessionId -> "12345")
          val target = setupTarget(
            Some(PrivateResidenceReliefModel("Yes", None, None)),
            None,
            Some(DisposalDateModel(5, 10, 2016)),
            Some(AcquisitionDateModel("No", None, None, None)),
            Some(RebasedValueModel("Yes", Some(455)))
          )
          lazy val result = target.privateResidenceRelief(fakeRequest)
          lazy val document = Jsoup.parse(bodyOf(result))

          "return a 200" in {
            status(result) shouldBe 200
          }

          "return some HTML that" should {

            "have the `Yes` option of the radio button checked" in {
              document.body.getElementById("isClaimingPRR-yes").attr("checked") shouldEqual "checked"
            }

            "Not show the Days Between question" in {
              document.body.getElementById("daysClaimedAfter") shouldEqual null
            }

            "Not show the Days Before question" in {
              document.body.getElementById("daysClaimed") shouldEqual null
            }
          }
        }

        "when disposal date is < 6 October 2016 with rebased value" should {

          lazy val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/private-residence-relief").withSession(SessionKeys.sessionId -> "12345")
          val target = setupTarget(
            Some(PrivateResidenceReliefModel("Yes", None, None)),
            None,
            Some(DisposalDateModel(5, 10, 2016)),
            None,
            Some(RebasedValueModel("Yes", Some(455)))
          )
          lazy val result = target.privateResidenceRelief(fakeRequest)
          lazy val document = Jsoup.parse(bodyOf(result))

          "return a 200" in {
            status(result) shouldBe 200
          }

          "return some HTML that" should {

            "have the `Yes` option of the radio button checked" in {
              document.body.getElementById("isClaimingPRR-yes").attr("checked") shouldEqual "checked"
            }

            "Not show the Days Between question" in {
              document.body.getElementById("daysClaimedAfter") shouldEqual null
            }

            "Not show the Days Before question" in {
              document.body.getElementById("daysClaimed") shouldEqual null
            }
          }
        }

        "when disposal date is < 6 October 2016, no acquisition date with no rebased value" should {

          lazy val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/private-residence-relief").withSession(SessionKeys.sessionId -> "12345")
          val target = setupTarget(
            Some(PrivateResidenceReliefModel("Yes", None, None)),
            None,
            Some(DisposalDateModel(5, 10, 2016)),
            None,
            Some(RebasedValueModel("No", None))
          )
          lazy val result = target.privateResidenceRelief(fakeRequest)
          lazy val document = Jsoup.parse(bodyOf(result))

          "return a 200" in {
            status(result) shouldBe 200
          }

          "return some HTML that" should {

            "have the `Yes` option of the radio button checked" in {
              document.body.getElementById("isClaimingPRR-yes").attr("checked") shouldEqual "checked"
            }

            "Not show the Days Between question" in {
              document.body.getElementById("daysClaimedAfter") shouldEqual null
            }

            "Not show the Days Before question" in {
              document.body.getElementById("daysClaimed") shouldEqual null
            }
          }
        }

        "when disposal date is < 6 October 2016, acquisition date < 6 April 15 with rebased value" should {

          lazy val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/private-residence-relief").withSession(SessionKeys.sessionId -> "12345")
          val target = setupTarget(
            Some(PrivateResidenceReliefModel("Yes", Some(23), None)),
            None,
            Some(DisposalDateModel(5, 10, 2016)),
            Some(AcquisitionDateModel("Yes", Some(5), Some(4), Some(2015))),
            Some(RebasedValueModel("Yes", Some(455)))
          )
          lazy val result = target.privateResidenceRelief(fakeRequest)
          lazy val document = Jsoup.parse(bodyOf(result))

          "return a 200" in {
            status(result) shouldBe 200
          }

          "return some HTML that" should {

            "have the `Yes` option of the radio button checked" in {
              document.body.getElementById("isClaimingPRR-yes").attr("checked") shouldEqual "checked"
            }

            "Not show the Days Between question" in {
              document.body.getElementById("daysClaimedAfter") shouldEqual null
            }

            "have an input with question 'How many days before 5 April 2015 are you claiming relief for'" in {
              document.body.getElementById("daysClaimed").tagName shouldEqual "input"
              document.select("label[for=daysClaimed]").text should include(Messages("calc.privateResidenceRelief.questionBefore.partOne"))
              document.select("label[for=daysClaimed]").text should include(Messages("calc.privateResidenceRelief.questionBefore.partTwo"))
            }

            "have 23 days in the input field for days claimed" in {
              document.body.getElementById("daysClaimed").attr("value") shouldEqual "23"
            }
          }
        }

        "when disposal date is < 6 October 2016, acquisition date < 6 April 15 with no rebased value" should {

          lazy val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/private-residence-relief").withSession(SessionKeys.sessionId -> "12345")
          val target = setupTarget(
            Some(PrivateResidenceReliefModel("Yes", Some(23), None)),
            None,
            Some(DisposalDateModel(5, 10, 2016)),
            Some(AcquisitionDateModel("Yes", Some(5), Some(4), Some(2015))),
            Some(RebasedValueModel("No", None))
          )
          lazy val result = target.privateResidenceRelief(fakeRequest)
          lazy val document = Jsoup.parse(bodyOf(result))

          "return a 200" in {
            status(result) shouldBe 200
          }

          "return some HTML that" should {

            "have the `Yes` option of the radio button checked" in {
              document.body.getElementById("isClaimingPRR-yes").attr("checked") shouldEqual "checked"
            }

            "Not show the Days Between question" in {
              document.body.getElementById("daysClaimedAfter") shouldEqual null
            }

            "have an input with question 'How many days before 5 April 2015 are you claiming relief for'" in {
              document.body.getElementById("daysClaimed").tagName shouldEqual "input"
              document.select("label[for=daysClaimed]").text should include(Messages("calc.privateResidenceRelief.questionBefore.partOne"))
              document.select("label[for=daysClaimed]").text should include(Messages("calc.privateResidenceRelief.questionBefore.partTwo"))
            }

            "have 23 days in the input field for days claimed" in {
              document.body.getElementById("daysClaimed").attr("value") shouldEqual "23"
            }
          }
        }

        "when disposal date is < 6 October 2016, acquisition date >= 6 April 15 with rebased value" should {

          lazy val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/private-residence-relief").withSession(SessionKeys.sessionId -> "12345")
          val target = setupTarget(
            Some(PrivateResidenceReliefModel("Yes", None, None)),
            None,
            Some(DisposalDateModel(5, 10, 2016)),
            Some(AcquisitionDateModel("Yes", Some(6), Some(4), Some(2015))),
            Some(RebasedValueModel("Yes", Some(400)))
          )
          lazy val result = target.privateResidenceRelief(fakeRequest)
          lazy val document = Jsoup.parse(bodyOf(result))

          "return a 200" in {
            status(result) shouldBe 200
          }

          "return some HTML that" should {

            "have the `Yes` option of the radio button checked" in {
              document.body.getElementById("isClaimingPRR-yes").attr("checked") shouldEqual "checked"
            }

            "Not show the Days Between question" in {
              document.body.getElementById("daysClaimedAfter") shouldEqual null
            }

            "Not show the Days Before question" in {
              document.body.getElementById("daysClaimed") shouldEqual null
            }
          }
        }

        "when disposal date is < 6 October 2016, acquisition date >= 6 April 15 with no rebased value" should {

          lazy val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/private-residence-relief").withSession(SessionKeys.sessionId -> "12345")
          val target = setupTarget(
            Some(PrivateResidenceReliefModel("Yes", None, None)),
            None,
            Some(DisposalDateModel(5, 10, 2016)),
            Some(AcquisitionDateModel("Yes", Some(6), Some(4), Some(2015))),
            Some(RebasedValueModel("No", None))
          )
          lazy val result = target.privateResidenceRelief(fakeRequest)
          lazy val document = Jsoup.parse(bodyOf(result))

          "return a 200" in {
            status(result) shouldBe 200
          }

          "return some HTML that" should {

            "have the `Yes` option of the radio button checked" in {
              document.body.getElementById("isClaimingPRR-yes").attr("checked") shouldEqual "checked"
            }

            "Not show the Days Between question" in {
              document.body.getElementById("daysClaimedAfter") shouldEqual null
            }

            "Not show the Days Before question" in {
              document.body.getElementById("daysClaimed") shouldEqual null
            }
          }
        }
      }
    }
  }

  //POST Tests
  "In CalculationController calling the .submitPrivateResidenceRelief action " should {

    lazy val fakeRequest = FakeRequest("POST", "/calculate-your-capital-gains/private-residence-relief").withSession(SessionKeys.sessionId -> "12345")
    val target = setupTarget(None, None)
    lazy val result = target.submitPrivateResidenceRelief(fakeRequest)

    "return a 303" in {
      status(result) shouldBe 303
    }

    s"redirect to ${routes.CalculationController.entrepreneursRelief()}" in {
      redirectLocation(result) shouldBe Some(s"${routes.CalculationController.entrepreneursRelief()}")
    }
  }
}
