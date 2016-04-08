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
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.SessionKeys
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class IntroductionControllerSpec extends UnitSpec with WithFakeApplication {

  class fakeRequestTo(url : String) {
    val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/" + url)
  }

  class fakeRequestToWithSessionId(url : String) {
    val sessionId = UUID.randomUUID.toString
    val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/" + url).withSession(SessionKeys.sessionId -> s"session-$sessionId")
  }

  "IntroductionController.introduction" should {
    "return 200 with no session" in new fakeRequestTo("introduction") {
      val result = IntroductionController.introduction(fakeRequest)
      status(result) shouldBe 200
    }

    "return HTML with no session" in new fakeRequestTo("introduction"){
      val result = IntroductionController.introduction(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }

    "display the title from the messages file" in new fakeRequestTo("introduction") {
      val result = IntroductionController.introduction(fakeRequest)
      contentAsString(result) should include (Messages("calc.introduction.title"))
    }

    "contain a start button" in new fakeRequestTo("introduction") {
      val result = IntroductionController.introduction(fakeRequest)
      contentAsString(result) should include (Messages("calc.introduction.start") + "</button>")
    }

    "return 200 with session Id" in new fakeRequestToWithSessionId("introduction") {
      val result = IntroductionController.introduction(fakeRequest)
      status(result) shouldBe 200
    }

    "return HTML with session Id" in new fakeRequestToWithSessionId("introduction"){
      val result = IntroductionController.introduction(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }


  }
}
