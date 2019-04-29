name := "learning-akka"

version := "0.1"

scalaVersion := "2.12.8"

val AKKA_VERSION = "2.5.22"

val common = Seq(
  version := "1.0.0",
  scalaVersion := "2.12.8",
  libraryDependencies ++= Seq(
    "joda-time" % "joda-time" % "2.10.1",
    "org.scalactic" %% "scalactic" % "3.0.5",
    "com.typesafe.akka" %% "akka-actor" % AKKA_VERSION,
    "org.scalatest" %% "scalatest" % "3.0.5" % Test,
    "com.typesafe.akka" %% "akka-testkit" % AKKA_VERSION % Test
  )
)

// Root subproject: will not publish JARs, only aggregate other subprojects.
// So e.g. `sbt test` will run tests in the aggregated subprojects.
lazy val hello_root = (project in file("."))
  .enablePlugins(PlayJava)
  .settings(common)
  .settings(
    publishArtifact := false
  )
  .aggregate(akka_actors, fsm, persistence)

lazy val akka_actors = (project in file("akka-actors"))
  .settings(common)
  .settings(
    name := "akka-actors"
  )

lazy val fsm = (project in file("fsm"))
  .settings(common)
  .settings(
    name := "fsm",
    libraryDependencies ++= Seq()
  )

lazy val persistence = (project in file("persistence"))
  .enablePlugins(PlayScala)
  .settings(common)
  .settings(
    name := "persistence",
    libraryDependencies ++= Seq(
      guice,
      "com.typesafe.akka" %% "akka-persistence" % AKKA_VERSION,
      "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
      "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
    )
  ).settings(
    javaOptions in Test += "-Dconfig.file=conf/application.test.conf"
  )

lazy val streams = (project in file("streams"))
  .enablePlugins(PlayScala)
  .settings(common)
  .settings(
    name := "streams",
    libraryDependencies ++= Seq(
      guice,
      "com.typesafe.akka" %% "akka-stream" % AKKA_VERSION,
      "com.typesafe.play" %% "play-json-joda" % "2.7.0",
      "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
    )
  ).settings(
    javaOptions in Test += "-Dconfig.file=conf/application.test.conf"
  )

lazy val streamsAndPersistence = (project in file("streams-and-persistence"))
  .enablePlugins(PlayScala)
  .settings(common)
  .settings(
    name := "streams-and-persistence",
    libraryDependencies ++= Seq(
      guice,
      "com.typesafe.akka" %% "akka-persistence" % AKKA_VERSION,
      "com.typesafe.akka" %% "akka-stream" % AKKA_VERSION,
      "com.typesafe.play" %% "play-json-joda" % "2.7.0",
      "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
    )
  ).settings(
    javaOptions in Test += "-Dconfig.file=conf/application.test.conf"
  )

lazy val clustering = (project in file("clustering"))
  .enablePlugins(PlayScala)
  .settings(common)
  .settings(
    name := "clustering",
    libraryDependencies ++= Seq(
      guice,
      "com.typesafe.akka" %% "akka-stream" % AKKA_VERSION,
      "com.typesafe.play" %% "play-json-joda" % "2.7.0",
      "com.typesafe.akka" %% "akka-cluster" % AKKA_VERSION,
      "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
    )
  ).settings(
    javaOptions in Test += "-Dconfig.file=conf/application.test.conf"
  )

