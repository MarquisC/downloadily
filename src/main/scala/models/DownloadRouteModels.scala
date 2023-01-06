package io.enigma.downloadily.models

object DownloadRouteModels {
  case class DownloadPostModel(url : String, httpHeaders : Map[String, Any], outputPath : String) {}
}
