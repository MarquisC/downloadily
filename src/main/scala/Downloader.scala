package io.enigma.downloadily

import org.slf4j.{Logger, LoggerFactory}

import java.io.{BufferedInputStream, File, FileOutputStream}

case class Downloadable(source : String, destination : String) {}

sealed abstract class Downloader(obj : Downloadable) {

  var BUFFER_SIZE = 1024

  def foreachByteChunkCallable(arr : Seq[Int]): Unit = {
    println(new String(arr.map(_.toByte).toArray))
  }

  def createStream(): (Iterator[Int], BufferedInputStream) = {
    val url = new java.net.URL(obj.source)
    val bufferedInputStream = new BufferedInputStream(url.openStream())
    (Iterator.continually(bufferedInputStream.read()).takeWhile(_ != -1), bufferedInputStream)
  }

  def download(): Unit

  def generateOutputFileName(obj : Downloadable) : String = {
    var output = obj.destination + "genericFile"
    try {
      // Naive "grab last file from path" in URL
      output = obj.source.split("/")(obj.source.split("/").length - 1 )
    }
    output
  }
}

case class LocalDiskDownloader(obj : Downloadable) extends Downloader(obj) {

  val logger: Logger = LoggerFactory.getLogger(classOf[LocalDiskDownloader])

  var fileOutputStream: Option[FileOutputStream] = None
  var outputFile : Option[File] = None
  var overwriteFile : Boolean = false

  override def foreachByteChunkCallable(arr : Seq[Int]): Unit = {
    if(this.fileOutputStream.isEmpty) {
      logger.debug("Creating new File Output Stream.")
      this.outputFile = Option(new File(generateOutputFileName(this.obj)))
      this.fileOutputStream = Option(new FileOutputStream(this.outputFile.get, this.overwriteFile))
    }

    fileOutputStream match {
      case Some(f) => f.write(arr.map(_.toByte).toArray)
      case None    => logger.error("File Output Stream is null...!")
    }
  }

  def download(): Unit = {
    logger.info(s"Starting download of [${this.obj.source}] to [${this.obj.destination}]")
    val (iterator, bufferedInputStream) = createStream()
    iterator
      .grouped(BUFFER_SIZE)
      .foreach(foreachByteChunkCallable)


    fileOutputStream match {
      case Some(fos) => {
        this.outputFile match {
          case Some(f) => logger.info(s"File [${f.getAbsoluteFile}] is size [${f.length()}]")
        }
        fos.close()
      }
      case None => logger.error("File Output Stream does not exist to be closed.")
    }

    bufferedInputStream.close()
  }
}

case class S3Downloader(obj : Downloadable) extends Downloader(obj) {
  def download() : Unit = {
    val (iterator, bufferedInputStream) = createStream()
    iterator
      .grouped(BUFFER_SIZE)
      .foreach(foreachByteChunkCallable)

    bufferedInputStream.close()
  }
}

object Downloader {

  val logger: Logger = LoggerFactory.getLogger(classOf[Downloader])

  def download(obj : Downloadable): Unit = {
    logger.info(s"Attempting to download a file from [${obj.source}]")
    try {
      var downloader: Downloader = LocalDiskDownloader(obj)
       if(obj.destination.contains("s3")) {
          logger.info(s"Attempting to download a file to S3 destination [${obj.destination}]")
          downloader = S3Downloader(obj)
        }
      downloader.download()
    } catch {
      case e => {
        logger.error(s"Failure in downloading fail [${obj.source}]")
      }
    }

  }

}
