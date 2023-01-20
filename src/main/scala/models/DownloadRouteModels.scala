package io.enigma.downloadily.models

import com.google.gson.annotations.Expose
import io.enigma.downloadily.Downloadable
import org.jetbrains.annotations.{NotNull, Nullable}

object DownloadRouteModels {
  // This model really needs more information...
  class DownloadPostModel {
    @Expose @NotNull
    var url : String = _
    @Expose @Nullable
    var destination :String = _
    def createDownloadable : Downloadable = {
      Downloadable(this.url, Option(this.destination).getOrElse("./"))
    }
  }
}
