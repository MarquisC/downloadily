package io.enigma.downloadily

import io.javalin.http.HandlerType
import io.javalin.Javalin

import org.slf4j.{Logger, LoggerFactory}

import java.util.concurrent.{ExecutorService, Executors}

object Main {
  val logger: Logger = LoggerFactory.getLogger(classOf[Main.type])

  val JAVALIN_THREAD_POOL: ExecutorService = Executors.newFixedThreadPool(10)
  val JAVALIN_DOWNLOADER_THREAD_POOL: ExecutorService = Executors.newFixedThreadPool(3)

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
