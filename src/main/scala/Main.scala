package io.enigma.downloadily

import io.javalin.Javalin
import io.javalin.http.HandlerType
import org.slf4j.{Logger, LoggerFactory}

object Main {

  val logger: Logger = LoggerFactory.getLogger(classOf[Main.type])

  def main(args: Array[String]): Unit = {
    val app = Javalin.create()
    controllers.registerRoutes()
    controllers.getControllers.map(controller => {
      controller.handlerType match {
        case HandlerType.GET => {
          logger.info(s"Registering Route [${controller.handlerPath}]")
          app.get(controller.handlerPath, controller)
        }
        case HandlerType.POST => {
          logger.info(s"Registering Route [${controller.handlerPath}]")
          app.post(controller.handlerPath, controller)
        }
      }
    })
    app.start(7070)
  }
}
