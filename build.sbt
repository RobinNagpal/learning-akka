name := "learning-akka"

version := "0.1"

scalaVersion := "2.12.8"

val common = Seq(
  version := "1.0.0",
  scalaVersion := "2.12.8",
  libraryDependencies ++= Seq(
    "joda-time" % "joda-time" % "2.10.1",
    "org.scalactic" %% "scalactic" % "3.0.5",
    "org.scalatest" %% "scalatest" % "3.0.5" % "test"
  )
)

// Root subproject: will not publish JARs, only aggregate other subprojects.
// So e.g. `sbt test` will run tests in the aggregated subprojects.
lazy val hello_root = (project in file("."))
  .settings(common)
  .settings(
    publishArtifact := false
  )
  .aggregate(akka_actors)

// main standalone app subproject, source code in ./myapp/
lazy val akka_actors  = (project in file("akka-actors"))
  .settings(common)
  .settings(
    name := "akka-actors", // my-app_2.12-1.0.0.jar
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.5.19",
      "com.typesafe.akka" %% "akka-testkit" % "2.5.19" % Test
    )
  )
