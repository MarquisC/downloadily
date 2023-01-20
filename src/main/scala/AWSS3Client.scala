package io.enigma.downloadily

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import java.net.URI

/*
    Example Credentials Provider
    DefaultCredentialsProvider.builder().build()
 */
case class AWSS3Client(var region : String = "us-east-1",
                       var endpointURL : String = "",
                       var credentialsProvider : AwsCredentialsProvider
                ) {

  var clientRegion : Region = Region.of(region)
  var client : S3AsyncClient = {
    if (endpointURL.isEmpty)
      S3AsyncClient.builder.region(clientRegion).credentialsProvider(credentialsProvider).build
    else S3AsyncClient.builder.endpointOverride(new URI(endpointURL))
      .region(clientRegion)
      .credentialsProvider(credentialsProvider)
      .forcePathStyle(true)
      .build
  }
}

object AWSS3Client {
  def createClient(region: String, endpointURL : String, credentialsProvider : AwsCredentialsProvider) : AWSS3Client = {
    AWSS3Client(region, endpointURL, credentialsProvider)
  }
}
