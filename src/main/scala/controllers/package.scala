package io.enigma.downloadily

import io.javalin.http.HandlerType

import scala.collection.mutable

package object controllers {
  private var controllerList : mutable.ArraySeq[Controller] = mutable.ArraySeq.empty

  def registerRoutes(): Unit = {
    controllerList = controllerList :+ HealthzController("/", HandlerType.GET)
    controllerList = controllerList :+ HealthzController("/health", HandlerType.GET)
    controllerList = controllerList :+ DownloadController("/download", HandlerType.POST)
  }

  def getControllers: mutable.ArraySeq[Controller]  = controllerList
}
