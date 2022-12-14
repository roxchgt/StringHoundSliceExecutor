import sbt.Keys.baseDirectory

version := "1.0.0"
scalaVersion := "2.12.8"
Global / onChangedBuildSource := ReloadOnSourceChanges
lazy val root = (project in file("."))
  .settings(
    name := "StringHound_SliceExecutor"
  )

libraryDependencies ++= Seq(
  "org.apache.commons" % "commons-lang3" % "3.12.0",
  "com.rabbitmq" % "amqp-client" % "5.14.2",
  "org.slf4j" % "slf4j-api" % "1.7.36",
  "ch.qos.logback" % "logback-classic" % "1.2.11",
  "com.typesafe" % "config" % "1.4.2",
  "de.opal-project" %% "common" % "2.0.1"
)

Runtime / unmanagedClasspath += baseDirectory.value / "resources"

mainClass := Some("main.StringHoundSliceExecutor")
assembly / assemblyOutputPath := file("docker/files/executor.jar")
