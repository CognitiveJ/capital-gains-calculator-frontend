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

import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class SummaryControllerSpec extends UnitSpec with WithFakeApplication {

  class fakeRequestTo(url : String) {
    val fakeRequest = FakeRequest("GET", "/calculate-your-capital-gains/" + url)
  }

  "SummaryController.summary" should {
    "return 200 from summary" in new fakeRequestTo("summary") {
      val result = SummaryController.summary(fakeRequest)
      status(result) shouldBe 200
    }

    "return HTML from summary" in new fakeRequestTo("summary") {
      val result = SummaryController.summary(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }
  }
}
