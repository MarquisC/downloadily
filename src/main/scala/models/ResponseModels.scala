package io.enigma.downloadily.models

import com.google.gson.annotations.Expose

object ResponseModels {

  class ResponseModel {
    @Expose
    var message : String = _
    var httpStatusCode : Int = _

    def this(message : String, httpStatusCode : Int) {
      this
      this.message = message
      this.httpStatusCode = httpStatusCode
    }
  }

}
