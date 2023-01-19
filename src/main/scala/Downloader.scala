package io.enigma.downloadily

import org.slf4j.{Logger, LoggerFactory}
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.services.s3.model.{CompleteMultipartUploadRequest, CompletedMultipartUpload, CompletedPart, CreateMultipartUploadRequest, CreateMultipartUploadResponse, UploadPartRequest}

import java.io.{BufferedInputStream, File, FileOutputStream}
import java.nio.file.FileAlreadyExistsException
import java.util

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
      if(this.outputFile.get.isFile && !this.overwriteFile) {
        logger.debug("File exists, but file overwrite is not enabled")
        throw new FileAlreadyExistsException(s"${this.outputFile.get}")
      }
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

case class S3Downloader(obj : Downloadable,
                        s3Client : AWSS3Client,
                        s3BucketName : String,
                        s3KeyName : String) extends Downloader(obj) {

  val logger: Logger = LoggerFactory.getLogger(classOf[S3Downloader])


  /*
    Reference:
    https://docs.aws.amazon.com/AmazonS3/latest/userguide/mpu-upload-object.html

   */

  var initialMultipartUploadRequest : CreateMultipartUploadRequest = _
  var initialMultiPartResponse : CreateMultipartUploadResponse = _
  var completedChunkResponses : util.ArrayList[CompletedPart]
  var filePartIncrementor = 0

  override def foreachByteChunkCallable(arr : Seq[Int]): Unit = {
    if(this.filePartIncrementor == 0) {

      this.filePartIncrementor = this.filePartIncrementor + 1

      this.initialMultipartUploadRequest =
        CreateMultipartUploadRequest.builder()
          .bucket(s3BucketName)
          .key(s3KeyName)
          .build()

      this.initialMultiPartResponse = this.s3Client
        .client
        .createMultipartUpload(this.initialMultipartUploadRequest)
        .get()

      val uploadPartRequest = UploadPartRequest.builder()
        .bucket(s3BucketName)
        .key(s3KeyName)
        .uploadId(this.initialMultiPartResponse.uploadId())
        .partNumber(filePartIncrementor).build()

      val requestBodyBytes = AsyncRequestBody.fromBytes(arr.map(_.toByte).toArray)
      val uploadPartResponse = s3Client.client.uploadPart(uploadPartRequest, requestBodyBytes).get()
      val complChunk = CompletedPart.builder.partNumber(this.filePartIncrementor)
        .eTag(uploadPartResponse.eTag()).build()
      completedChunkResponses.add(complChunk)
    }
    this.filePartIncrementor = this.filePartIncrementor + 1

    val uploadPartRequest = UploadPartRequest.builder()
      .bucket(s3BucketName)
      .key(s3KeyName)
      .uploadId(this.initialMultiPartResponse.uploadId())
      .partNumber(filePartIncrementor).build()

    val requestBodyBytes = AsyncRequestBody.fromBytes(arr.map(_.toByte).toArray)
    val uploadPartResponse = s3Client.client.uploadPart(uploadPartRequest, requestBodyBytes).get()
    val complChunk = CompletedPart.builder.partNumber(this.filePartIncrementor)
      .eTag(uploadPartResponse.eTag()).build()
    completedChunkResponses.add(complChunk)
  }

  /*
    ToDo, add if < configurable buffer size, directly upload full byte array
    ToDo, add exception handling for aborting upload
   */
  def download() : Unit = {
    val (iterator, bufferedInputStream) = createStream()
    iterator
      .grouped(BUFFER_SIZE)
      .foreach(foreachByteChunkCallable)

    bufferedInputStream.close()

    if(this.initialMultiPartResponse != null) {
      val completedMultipartUpload = CompletedMultipartUpload.builder.parts(this.completedChunkResponses).build
      val completeMultipartUploadRequest = CompleteMultipartUploadRequest
        .builder.bucket(s3BucketName).key(s3KeyName).uploadId(this.initialMultiPartResponse.uploadId()
      ).multipartUpload(completedMultipartUpload).build
      this.s3Client.client.completeMultipartUpload(completeMultipartUploadRequest)
    }
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
          downloader = S3Downloader(obj, null, "", "")
        }
      downloader.download()
    } catch {
      case e => logger.error(s"Failure in downloading file [${obj.source}], for error [${e}]")
    }

  }

}
