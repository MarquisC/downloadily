package io.enigma.downloadily.controllers

import com.google.gson.{Gson, GsonBuilder, JsonSyntaxException}
import io.enigma.downloadily.Config.ThreadPoolIsFullException
import io.enigma.downloadily.models.{DownloadRouteModels, ResponseModels}
import io.enigma.downloadily.{Config, Downloader, HttpUtils}
import io.javalin.http.{Context, HandlerType}
import io.javalin.util.function.ThrowingRunnable
import org.slf4j.{Logger, LoggerFactory}

import java.net.http.HttpResponse
import java.util.concurrent.ThreadPoolExecutor

case class DownloadController(override val handlerPath : String, override val handlerType: HandlerType) extends Controller(handlerPath, handlerType) {

  val logger: Logger = LoggerFactory.getLogger(classOf[DownloadController])

  override def handle(ctx: Context): Unit = {
    ctx.async(
      Config.JAVALIN_THREAD_POOL, Config.DEFAULT_HTTP_CALLBACK_TIMEOUT_SECONDS, new Runnable {

        val gson: Gson = new GsonBuilder()
          .excludeFieldsWithoutExposeAnnotation()
          .create()

        override def run(): Unit = {
          val response = new ResponseModels.ResponseModel("Error, HTTP timeout occurred", 408)
          ctx.status(response.httpStatusCode)
          ctx.result(gson.toJson(response))
        }
      },
      new ThrowingRunnable[Exception] {
        val gson: Gson = new GsonBuilder().create()

        override def run(): Unit = {
          try {
            val downloadPostModel = gson.fromJson(ctx.body(), new DownloadRouteModels.DownloadPostModel().getClass)

            if(Config.isThreadPoolFull(Config.JAVALIN_DOWNLOADER_THREAD_POOL.asInstanceOf[ThreadPoolExecutor])) {
              throw new ThreadPoolIsFullException()
            }

            Config.JAVALIN_DOWNLOADER_THREAD_POOL.submit(new Runnable {
              override def run(): Unit = {
                val http = new HttpUtils()
                // Check if there's data to process
                val response = http.httpGetRequest(downloadPostModel.url, Config.DEFAULT_HTTP_HEAD_REQUEST_TIMEOUT_SECONDS).asInstanceOf[HttpResponse[_]]
                if(http.getContentLength(response) != "-1") {
                  logger.debug(s"The download's content length is [${http.getContentLength(response)}]")
                  Downloader.download(downloadPostModel.createDownloadable)
                }
              }
            })

            ctx.status(201)
            ctx.result(gson.toJson(s"Download for URL [${downloadPostModel.url}] initiated"))

          } catch {
            case _: ThreadPoolIsFullException => {
              ctx.status(429)
              ctx.result(gson.toJson(s"Error, ThreadPoolIsFullException."))
            }
            case _: IllegalThreadStateException => {
              ctx.status(503)
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