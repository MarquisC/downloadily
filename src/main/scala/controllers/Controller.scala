package io.enigma.downloadily.controllers

import com.google.gson.GsonBuilder
import io.enigma.downloadily.Main
import io.enigma.downloadily.models.HealthzRouteModels
import io.javalin.http.{Context, Handler, HandlerType}
import io.javalin.util.function.ThrowingRunnable

abstract class Controller(val handlerPath : String, val handlerType: HandlerType) extends Handler

case class HealthzController(override val handlerPath : String, override val handlerType: HandlerType) extends Controller(handlerPath, handlerType) {
  override def handle(ctx: Context): Unit = {
    ctx.async(
      Main.JAVALIN_THREAD_POOL, 5, new Runnable {
        val gson = new GsonBuilder()
          .excludeFieldsWithoutExposeAnnotation()
          .create()

        override def run(): Unit = {
          val response = HealthzRouteModels.HealthzResponse("Error, HTTP timeout occurred", 408)
          ctx.status(response.httpStatusCode)
          ctx.result(gson.toJson(response))
        }
      },
      new ThrowingRunnable[Exception] {
        val gson = new GsonBuilder()
          .excludeFieldsWithoutExposeAnnotation()
          .create()

        override def run(): Unit = {
          val response = HealthzRouteModels.HealthzResponse("healthy", 200)
          ctx.status(response.httpStatusCode)
          ctx.result(gson.toJson(response))
        }
      }
    )
  }
}

