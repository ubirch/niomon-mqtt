package com.ubirch.controllers

import com.typesafe.config.Config
import com.ubirch.ConfPaths.GenericConfPaths
import com.ubirch.controllers.concerns.{ ControllerBase, SwaggerElements }
import com.ubirch.models.{ Good, NOK }
import io.prometheus.client.Counter
import monix.eval.Task
import monix.execution.Scheduler
import org.json4s.Formats
import org.scalatra._
import org.scalatra.swagger.{ Swagger, SwaggerSupportSyntax }

import javax.inject._
import scala.concurrent.ExecutionContext

/**
  * Represents a simple controller for the base path "/"
  * @param swagger Represents the Swagger Engine.
  * @param jFormats Represents the json formats for the system.
  */

@Singleton
class InfoController @Inject() (config: Config, val swagger: Swagger, jFormats: Formats)(implicit val executor: ExecutionContext, scheduler: Scheduler)
  extends ControllerBase {

  override protected val applicationDescription = "Info Controller"
  override protected implicit def jsonFormats: Formats = jFormats

  val service: String = config.getString(GenericConfPaths.NAME)

  val successCounter: Counter = Counter.build()
    .name("info_management_success")
    .help("Represents the number of info management successes")
    .labelNames("service", "method")
    .register()

  val errorCounter: Counter = Counter.build()
    .name("info_management_failures")
    .help("Represents the number of info management failures")
    .labelNames("service", "method")
    .register()

  val getSimpleCheck: SwaggerSupportSyntax.OperationBuilder =
    (apiOperation[String]("simpleCheck")
      summary "Welcome"
      description "Getting a hello from the system"
      tags SwaggerElements.TAG_WELCOME)

  get("/hola", operation(getSimpleCheck)) {
    asyncResult("hola") { _ =>
      Task(hello)
    }
  }

  get("/hello", operation(getSimpleCheck)) {
    asyncResult("hello") { _ =>
      Task(hello)
    }
  }

  get("/ping", operation(getSimpleCheck)) {
    asyncResult("ping") { _ =>
      Task {
        Ok("pong")
      }
    }
  }

  get("/", operation(getSimpleCheck)) {
    asyncResult("root") { _ =>
      Task {
        Ok(Good("Hallo, Hola, Hello, Salut, Hej, this is the Ubirch Niomon Mqtt."))
      }
    }
  }

  before() {
    contentType = formats("json")
  }

  notFound {
    asyncResult("not_found") { _ =>
      Task {
        logger.info("controller=InfoController route_not_found={} query_string={}", requestPath, request.getQueryString)
        NotFound(NOK.noRouteFound(requestPath + " might exist in another universe"))
      }
    }
  }

  private def hello: ActionResult = {
    contentType = formats("txt")
    val data =
      """
        |                                    ___,,___
        |                                ,d8888888888b,_
        |                            _,d889'        8888b,
        |                        _,d8888'          8888888b,
        |                    _,d8889'           888888888888b,_
        |                _,d8889'             888888889'688888, /b
        |            _,d8889'               88888889'     `6888d 6,_
        |         ,d88886'              _d888889'           ,8d  b888b,  d\
        |       ,d889'888,             d8889'               8d   9888888Y  )
        |     ,d889'   `88,          ,d88'                 d8    `,88aa88 9
        |    d889'      `88,        ,88'                   `8b     )88a88'
        |   d88'         `88       ,88                   88 `8b,_ d888888
        |  d89            88,      88                  d888b  `88`_  8888
        |  88             88b      88                 d888888 8: (6`) 88')
        |  88             8888b,   88                d888aaa8888, `   'Y'
        |  88b          ,888888888888                 `d88aa `88888b ,d8
        |  `88b       ,88886 `88888888                 d88a  d8a88` `8/
        |   `q8b    ,88'`888  `888'"`88          d8b  d8888,` 88/ 9)_6
        |     88  ,88"   `88  88p    `88        d88888888888bd8( Z~/
        |     88b 8p      88 68'      `88      88888888' `688889`
        |     `88 8        `8 8,       `88    888 `8888,   `qp'
        |       8 8,        `q 8b       `88  88"    `888b
        |       q8 8b        "888        `8888'
        |        "888                     `q88b
        |                                  "888'
        |mic
        |
        |------------------------------------------------
        |Thank you for visiting https://asciiart.website/
        |This ASCII pic can be found at
        |https://asciiart.website/index.php?art=animals/bison
        |""".stripMargin
    Ok(data)
  }

}
