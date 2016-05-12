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

package controllers

import java.util.UUID
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Action}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.SessionKeys
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class IntroductionControllerSpec extends UnitSpec with WithFakeApplication {

  class fakeRequestTo(url : String, controllerAction : Action[AnyContent]) {
    val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/" + url)
    val result = controllerAction(fakeRequest)
    val jsoupDoc = Jsoup.parse(bodyOf(result))
  }

  class fakeRequestToWithSessionId(url : String, controllerAction : Action[AnyContent]) {
    val sessionId = UUID.randomUUID.toString
    val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/" + url).withSession(SessionKeys.sessionId -> s"session-$sessionId")
    val result = controllerAction(fakeRequest)
    val jsoupDoc = Jsoup.parse(bodyOf(result))
  }

  "IntroductionController.introduction" should {

    "when called with no session" should {

      object IntroductionTestDataItem extends fakeRequestTo("", IntroductionController.introduction)

      "return a 200" in {
        status(IntroductionTestDataItem.result) shouldBe 200
      }

      "return HTML that" should {

        "contain some text and use the character set utf-8" in {
          contentType(IntroductionTestDataItem.result) shouldBe Some("text/html")
          charset(IntroductionTestDataItem.result) shouldBe Some("utf-8")
        }

        "display the beta banner" in {
          IntroductionTestDataItem.jsoupDoc.body.getElementById("phase").text shouldEqual ("BETA")
        }
        "have the title 'Introduction'" in {
          IntroductionTestDataItem.jsoupDoc.title shouldEqual Messages("calc.introduction.title")
        }
        "contain a start button" in {
          IntroductionTestDataItem.jsoupDoc.body.getElementById("start").text shouldEqual Messages("calc.introduction.start")
        }
      }
    }

    "when called with a session" should {

      object IntroductionWithSessionTestDataItem extends fakeRequestToWithSessionId("introduction", IntroductionController.introduction)

      "return a 200" in {
        status(IntroductionWithSessionTestDataItem.result) shouldBe 200
      }

      "return HTML that" should {

        "contain some text and use character set utf-8" in {
          contentType(IntroductionWithSessionTestDataItem.result) shouldBe Some("text/html")
          charset(IntroductionWithSessionTestDataItem.result) shouldBe Some("utf-8")
        }
      }
    }
  }
}
