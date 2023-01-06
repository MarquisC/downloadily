package io.enigma
package io.enigma.downloadily

import org.scalatest.flatspec.AnyFlatSpec

import java.io.File

class DownloaderSpec extends AnyFlatSpec {
  "Downloader" should "download a Java class" in {
    //val source = "https://raw.githubusercontent.com/zio/zio-http/main/zio-http-example/src/main/scala/example/FileStreaming.scala"
    val source = "https://ichef.bbci.co.uk/news/912/cpsprodpb/17D2D/production/_111718579_press_release_3.png"
    val destination = "./"
    Downloader.download(Downloadable(source, destination))
    val outputFile = new File("./_111718579_press_release_3.png")
    assert(outputFile.exists())
    outputFile.deleteOnExit()
  }
}
