package io.enigma.downloadily.standalone

import io.enigma.downloadily.HttpUtils
import org.scalatest.flatspec.AnyFlatSpec
import java.net.http.HttpResponse

class HttpUtilsSpec extends AnyFlatSpec {
  "HTTP Head" should "perform a head request successfully" in {
    val source = "https://ichef.bbci.co.uk/news/912/cpsprodpb/17D2D/production/_111718579_press_release_3.png"
    val http = new HttpUtils()
    val response = http.httpHeadRequest(source, 10)
    assert(response.asInstanceOf[HttpResponse[_]].statusCode() == 200)
  }
  "HTTP Get" should "perform a get request successfully and grab the content-length" in {
    val source = "https://ichef.bbci.co.uk/news/912/cpsprodpb/17D2D/production/_111718579_press_release_3.png"
    val http = new HttpUtils()
    val response = http.httpGetRequest(source, 10).asInstanceOf[HttpResponse[_]]
    println(s"HTTP Get Content Length [${http.getContentLength(response)}]")
    assert(!http.getContentLength(response).equals("-1"))
  }
}
