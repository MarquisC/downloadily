package io.enigma.downloadily.models

import com.google.gson.annotations.Expose
import io.enigma.downloadily.Downloadable

object DownloadRouteModels {
  class DownloadPostModel {
    @Expose
    var url : String = _
    @Expose
    var des :String = _
    def createDownloadable : Downloadable = {
      Downloadable(this.url, Option(this.des).getOrElse("./"))
    }
  }
}
