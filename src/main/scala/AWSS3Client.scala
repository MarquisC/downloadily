package io.enigma.downloadily


import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client

import java.net.URI

/*
    Example Credentials Provider
    DefaultCredentialsProvider.builder().build()
 */
case class AWSS3(var region : String = "us-east-1",
                 var endpointURL : String = "",
                 var credentialsProvider : AwsCredentialsProvider = _
                ) {

  var clientRegion : Region = _
  var client : S3Client = _

   def apply() {
    clientRegion = Region.of(region)
    if(endpointURL.isEmpty) {
      this.client = S3Client.builder.region(clientRegion).credentialsProvider(credentialsProvider).build
    } else {
      this.client = S3Client.builder.endpointOverride(new URI(endpointURL)).region(clientRegion).credentialsProvider(credentialsProvider).build
    }
  }
}

object AWSS3 {
  
}
