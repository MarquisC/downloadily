package io.enigma.downloadily.controllers

import com.google.gson.{GsonBuilder, JsonSyntaxException}
import io.javalin.http.{Context, HandlerType}
import io.javalin.util.function.ThrowingRunnable
import io.enigma.downloadily.models.ResponseModels
import io.enigma.downloadily.{Main, Downloader, Downloadable}

import java.util

sealed class ThreadPoolIsFullException extends Exception {}

case class DownloadController(override val handlerPath : String, override val handlerType: HandlerType) extends Controller(handlerPath, handlerType) {
  override def handle(ctx: Context): Unit = {
    ctx.async(
      Main.JAVALIN_THREAD_POOL, 120, new Runnable {

        val gson = new GsonBuilder()
          .excludeFieldsWithoutExposeAnnotation()
          .create()

        override def run(): Unit = {
          val response = new ResponseModels.ResponseModel("Error, HTTP timeout occurred", 408)
          ctx.status(response.httpStatusCode)
          ctx.result(gson.toJson(response))
        }
      },
      new ThrowingRunnable[Exception] {
        val gson = new GsonBuilder().create()

        override def run(): Unit = {
          try {
            if(Main.JAVALIN_DOWNLOADER_THREAD_POOL.asInstanceOf[java.util.concurrent.ThreadPoolExecutor].getActiveCount >=
              Main.JAVALIN_DOWNLOADER_THREAD_POOL.asInstanceOf[java.util.concurrent.ThreadPoolExecutor].getMaximumPoolSize
            ) {
              // Don't schedule new work and return error response
              throw new ThreadPoolIsFullException
            }

            // ToDo, easy automagic gson -> Scala class/case class deser
            val jsonMap = gson.fromJson(ctx.body(), new util.HashMap().getClass)
            if(!jsonMap.containsKey("url")) {
              // Invalid JSON, must contain url
            }
            val source : String = jsonMap.get("url")

            Main.JAVALIN_DOWNLOADER_THREAD_POOL.submit(new Runnable {
              override def run(): Unit = {
                val target : Option[String] = Option(jsonMap.get("destination"))
                Downloader.download(Downloadable(source, target.getOrElse("./")))
              }
            })

            ctx.status(201)
            ctx.result(gson.toJson(s"Download for URL [${source}] initiated"))

          } catch {
            case _: ThreadPoolIsFullException => {
              ctx.status(404)
              ctx.result(gson.toJson(s"Error, ThreadPoolIsFullException."))
            }
            case _: IllegalThreadStateException => {
              ctx.status(404)
              ctx.result(gson.toJson(s"Error, IllegalThreadStateException."))
            }
            case _: JsonSyntaxException => {
              ctx.status(404)
              ctx.result(gson.toJson(s"Error, JSONSyntaxError."))
            }
          }
        }
      }
    )
  }
}