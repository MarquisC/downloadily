ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "downloadily",
    idePackagePrefix := Some("io.enigma.downloadily")
  )

libraryDependencies += "io.javalin" % "javalin" % "5.2.0"
libraryDependencies += "org.slf4j" % "slf4j-simple" % "2.0.3"
libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.14"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.14" % "test"
libraryDependencies += "com.google.code.gson" % "gson" % "2.10"
libraryDependencies += "software.amazon.awssdk" % "s3" % "2.19.17"
