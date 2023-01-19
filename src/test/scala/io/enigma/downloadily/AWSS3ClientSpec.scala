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
    val listBucketsResponse = s3.client.listBuckets()
    println(listBucketsResponse)
    assert(listBucketsResponse.hasBuckets)
  }
}
