ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.4.1"
lazy val AkkaHttpVersion = "10.5.3"
lazy val AkkaVersion =
  "2.8.5" // Make sure this version exists; otherwise, use a valid version.
lazy val scalaTestVersion = "3.2.17"
lazy val logbackVersion   = "1.4.14"
lazy val slf4jVersion     = "2.0.9"
lazy val mockitoVersion   = "1.16.42"

lazy val IntegrationTest = config("it") extend Test

lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    name             := "RoutingService",
    idePackagePrefix := Some("com.ts"),
    scalacOptions += "-Ytasty-reader",
    assembly / mainClass       := Some("com.ts.Main"),
    assembly / test            := {},
    assembly / assemblyJarName := s"routing-service.jar",
    assembly / assemblyMergeStrategy := {
      case PathList("reference.conf")          => MergeStrategy.concat
      case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
      case _                                   => MergeStrategy.first
    },
    inConfig(IntegrationTest)(Defaults.testSettings),
    IntegrationTest / scalaSource       := baseDirectory.value,
    IntegrationTest / resourceDirectory := baseDirectory.value,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-slf4j"          % AkkaVersion,
      "com.typesafe.akka" %% "akka-http"           % AkkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed"    % AkkaVersion,
      "com.typesafe.akka" %% "akka-stream"         % AkkaVersion,
      "com.typesafe.akka" %% "akka-http-testkit"   % AkkaHttpVersion  % Test,
      "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion      % Test,
      "com.typesafe.akka" %% "akka-testkit"        % AkkaVersion      % Test,
      "ch.qos.logback"     % "logback-classic"     % logbackVersion,
      "org.slf4j"          % "slf4j-api"           % slf4jVersion,
      "org.scalatest"     %% "scalatest"           % scalaTestVersion % Test,
      "org.scalatestplus" %% "mockito-5-10"        % "3.2.18.0"       % Test,
      "com.softwaremill.sttp.client3" %% "core" % "3.9.2" % "it,test",
      "com.softwaremill.sttp.client3" %% "async-http-client-backend-future" % "3.9.2" % "it,test"
    )
  )
