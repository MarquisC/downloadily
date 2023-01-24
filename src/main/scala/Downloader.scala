package io.enigma.downloadily

import org.slf4j.{Logger, LoggerFactory}
import software.amazon.awssdk.auth.credentials.{AwsCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.services.s3.model.{CompleteMultipartUploadRequest, CompletedMultipartUpload, CompletedPart, CreateMultipartUploadRequest, CreateMultipartUploadResponse, HeadBucketRequest, PutObjectRequest, UploadPartRequest}

import java.io.{BufferedInputStream, File, FileOutputStream}
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers
import java.nio.file.FileAlreadyExistsException
import java.util

case class Downloadable(source : String, destination : String) {}

sealed abstract class ChunkyDownloader(obj : Downloadable) {

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

case class LocalDiskDownloader(obj : Downloadable) extends ChunkyDownloader(obj) {

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
                        var s3Client : Option[AWSS3Client],
                        var s3BucketName : String,
                        s3KeyName : String) extends ChunkyDownloader(obj) {

  val logger: Logger = LoggerFactory.getLogger(classOf[S3Downloader])


  // ToDo, verified multi-part works, pull down content-length and judge if we can just do S3 put normally
  // For multi-part uploads each chunk, (except the last) must be >= 5 MB
  this.BUFFER_SIZE = 1048576 * 5

  /*
    Reference:
    https://docs.aws.amazon.com/AmazonS3/latest/userguide/mpu-upload-object.html
   */

 if(this.s3Client.isEmpty) {
   this.s3Client = Some(AWSS3Client("us-east-1", "http://localhost:4566", StaticCredentialsProvider.create(new AwsCredentials {
      override def accessKeyId(): String = "test"
      override def secretAccessKey(): String = "test"
    }
   )))
 }

  // Do better here
  this.s3BucketName = this.s3BucketName.split("s3://")(1)

  try {
    this.s3Client.get.client.headBucket(HeadBucketRequest.builder().bucket(s3BucketName).build()).get()
  }
  catch {
    case e: Throwable => throw new Exception(s"S3 HeadBucketRequest has failed [${e.getMessage}]")
  }

  // Will they hate me for mutating variables?
  var initialMultipartUploadRequest : CreateMultipartUploadRequest = _
  var initialMultiPartResponse : CreateMultipartUploadResponse = _
  var completedChunkResponses : util.ArrayList[CompletedPart] = new util.ArrayList[CompletedPart]()
  var filePartIncrementor = 0

  // I can make this tail recursive a little easier
  override def foreachByteChunkCallable(arr : Seq[Int]): Unit = {
    if(this.filePartIncrementor == 0) {

      this.filePartIncrementor = this.filePartIncrementor + 1

      this.initialMultipartUploadRequest =
        CreateMultipartUploadRequest.builder()
          .bucket(s3BucketName)
          .key(s3KeyName)
          .build()

      this.initialMultiPartResponse = this.s3Client.get
        .client
        .createMultipartUpload(this.initialMultipartUploadRequest)
        .get()

      val uploadPartRequest = UploadPartRequest.builder()
        .bucket(s3BucketName)
        .key(s3KeyName)
        .uploadId(this.initialMultiPartResponse.uploadId())
        .partNumber(filePartIncrementor).build()

      val requestBodyBytes = AsyncRequestBody.fromBytes(arr.map(_.toByte).toArray)
      val uploadPartResponse = this.s3Client.get.client.uploadPart(uploadPartRequest, requestBodyBytes).get()
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
    val uploadPartResponse = s3Client.get.client.uploadPart(uploadPartRequest, requestBodyBytes).get()
    val complChunk = CompletedPart.builder.partNumber(this.filePartIncrementor)
      .eTag(uploadPartResponse.eTag()).build()
    completedChunkResponses.add(complChunk)
  }

  /*
    ToDo, add if < configurable buffer size, directly upload full byte array
    ToDo, add exception handling for aborting upload
   */
  def download() : Unit = {

    val http = new HttpUtils()
    val response = http.httpGetRequest(this.obj.source, 30).asInstanceOf[HttpResponse[_]]
    if(http.getContentLength(response) != "-1") {
      logger.debug(s"The download's content length is [${http.getContentLength(response)}]")
    }

    if (http.getContentLength(response).toLong < this.BUFFER_SIZE) {
      logger.debug(s"The download's content length is within the bounds for a PutObject request, initiating.")
      val responseBody = http.httpGetRequest(this.obj.source, 30, BodyHandlers.ofByteArray()).asInstanceOf[HttpResponse[_]]
      val s3PutObjectRequest = PutObjectRequest.builder()
        .bucket(this.s3BucketName).key(this.s3KeyName)
        .contentLength(http.getContentLength(response).toLong).build()
      val s3PutObjectResponse = this.s3Client.get.client.putObject(s3PutObjectRequest,
        AsyncRequestBody.fromBytes(responseBody.body().asInstanceOf[Array[Byte]])).get()
      logger.debug(s"PutObjectRequest's response ${s3PutObjectResponse} ")
      return
    }

    val (iterator, bufferedInputStream) = createStream()
    iterator
      .grouped(BUFFER_SIZE)
      .foreach(foreachByteChunkCallable)

    bufferedInputStream.close()

    if(this.initialMultiPartResponse != null) {
      //ToDo add to destination
      logger.info(s"Completing multipart upload for file from source [${this.obj.source}]")
      val completedMultipartUpload = CompletedMultipartUpload.builder.parts(this.completedChunkResponses).build

      val completeMultipartUploadRequest = CompleteMultipartUploadRequest
        .builder.bucket(s3BucketName).key(s3KeyName).uploadId(this.initialMultiPartResponse.uploadId()
      ).multipartUpload(completedMultipartUpload).build

     // Don't forget to add abort exception handling for mishandled multipart chunks that don't fail until the end
      val completedMultipartUploadResponse =
        this.s3Client.get.client.completeMultipartUpload(completeMultipartUploadRequest).get()
      logger.debug(s"${completedMultipartUploadResponse}")
    }
  }
}

case class Downloader() {
  val logger: Logger = LoggerFactory.getLogger(classOf[Downloader])
  var s3Client : Option[AWSS3Client] = None

  def setS3Client(c : AWSS3Client): Unit = {
    this.s3Client = Option(c).orElse(None)
  }

  // Is this function thread safe?
  def download(obj : Downloadable): Unit = {
    logger.info(s"Attempting to download a file from [${obj.source}]")
    try {
      var downloader: ChunkyDownloader = LocalDiskDownloader(obj)
      // do something better than this
      if(obj.destination.contains("s3")) {
        // Do something better than this
        val destinationKey = downloader.generateOutputFileName(obj)
        logger.info(s"Attempting to download a file to S3 destination [${obj.destination}/${destinationKey}]")
        downloader = S3Downloader(obj, this.s3Client, obj.destination, destinationKey)
      }
      downloader.download()
    } catch {
      case e: Throwable => logger.error(s"Failure in downloading file [${obj.source}], for error [${e}]")
    }

  }
}
object Downloader {
  def download(obj : Downloadable): Unit = {
    new Downloader().download(obj)
  }
}
