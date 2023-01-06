package io.enigma.downloadily.models

object HealthzRouteModels {
  object HealthzResponse {
    def apply(message : String, httpStatusCode : Int): ResponseModels.ResponseModel = {
      new ResponseModels.ResponseModel(message, httpStatusCode)
    }
  }

}

