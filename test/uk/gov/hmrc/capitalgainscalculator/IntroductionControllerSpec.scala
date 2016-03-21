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

package uk.gov.hmrc.capitalgainscalculator

import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.capitalgainscalculator.controllers.cgts.IntroductionController
import uk.gov.hmrc.play.test.{WithFakeApplication, UnitSpec}

/**
  * Created by james on 18/03/16.
  */
class IntroductionControllerSpec extends UnitSpec with WithFakeApplication{

  val fakeRequest = FakeRequest("GET", "/capital-gains-calculator-frontend/introduction")
  val fakePostRequest = FakeRequest("POST", "/capital-gains-calculator-frontend/introduction")

  "GET /" should {
    "return 200" in {
      val result = IntroductionController.introduction(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return HTML" in {
      val result = IntroductionController.introduction(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }

  }

}
