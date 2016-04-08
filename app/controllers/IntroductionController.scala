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
import play.api.mvc.Action
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.{SessionKeys, HeaderCarrier}
import uk.gov.hmrc.play.http.logging.SessionId
import views.html.cgts

import scala.concurrent.Future

object IntroductionController extends IntroductionController

trait IntroductionController extends FrontendController {

  implicit val sessionId = UUID.randomUUID.toString
  implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(sessionId)))

  val introduction = Action.async { implicit request =>
    if (request.session.get(SessionKeys.sessionId).isEmpty) {
      Future.successful(Ok(cgts.introduction()).withSession(request.session + (SessionKeys.sessionId -> s"session-$sessionId")))
    }
    else {
      Future.successful(Ok(cgts.introduction()))
    }
  }
}
