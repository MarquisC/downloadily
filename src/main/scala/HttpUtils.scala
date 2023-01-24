package io.enigma.downloadily

import java.net.URI
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.time.Duration

class HttpUtils {

  def httpHeadRequest(url: String, connectionTimeout : Int): Any = {

    val httpClient: HttpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(connectionTimeout))
      .build()

    val requestHead = HttpRequest.newBuilder().HEAD.uri(URI.create(url)).build()

    httpClient.send(requestHead, BodyHandlers.discarding())
  }

  def httpGetRequest(url: String, connectionTimeout : Int,
  bodyHandler : HttpResponse.BodyHandler[_] = BodyHandlers.discarding()
                    ): Any = {

    val httpClient: HttpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(connectionTimeout))
      .build()

    val requestHead = HttpRequest.newBuilder().GET().uri(URI.create(url)).build()

    httpClient.send(requestHead, bodyHandler)
  }

  def getContentLength(response: HttpResponse[_]) : String = {
    val length : Option[String] = Option(response.headers().firstValue("content-length").get())
    length.getOrElse("-1")
  }


}
