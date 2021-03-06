/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.epayeapi.config

import play.api.ApplicationLoader
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationLoader, GuiceableModule}
import play.api.routing.Router
import uk.gov.hmrc.epayeapi.router.RoutesProvider

class EpayeApiAppLoader extends GuiceApplicationLoader {
  protected override def overrides(context: ApplicationLoader.Context): Seq[GuiceableModule] = {
    super.overrides(context) :+ (bind[Router].toProvider[RoutesProvider]: GuiceableModule)
  }

}
