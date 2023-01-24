package io.enigma.downloadily
package io.enigma.downloadily

import org.scalatest.flatspec.AnyFlatSpec
import software.amazon.awssdk.auth.credentials.{AwsCredentials, StaticCredentialsProvider}

class AWSS3ClientSpec extends AnyFlatSpec {
  "AWSS3Client" should "generate successfully and connect to localstack" in {
    val s3 = AWSS3Client("us-east-1", "http://localhost:4566", StaticCredentialsProvider.create(new AwsCredentials {
      override def accessKeyId(): String = "test"
      override def secretAccessKey(): String = "test"
    }))
    val listBucketsResponse = s3.client.listBuckets().get()
    println(listBucketsResponse)
    assert(listBucketsResponse.hasBuckets)
  }

  "AWSS3Client" should "non-multipart download a file to s3 successfully" in {
    val source = "https://raw.githubusercontent.com/zio/zio-http/main/zio-http-example/src/main/scala/example/FileStreaming.scala"
    val d = Downloadable(source, "s3://test-bucket1")
    Downloader.download(d)
  }

  "AWSS3Client" should "multipart download a file to s3 successfully" in {
    val source = "https://freetestdata.com/wp-content/uploads/2021/09/Free_Test_Data_1OMB_MP3.mp3"
    val d = Downloadable(source, "s3://test-bucket1")
    Downloader.download(d)
  }
}
