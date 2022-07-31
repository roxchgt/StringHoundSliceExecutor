version := "0.1.0-SNAPSHOT"
scalaVersion := "2.13.8"
Global / onChangedBuildSource := ReloadOnSourceChanges
lazy val root = (project in file("."))
  .settings(
    name := "worker-node"
  )

libraryDependencies ++= Seq(
  "com.rabbitmq" % "amqp-client" % "5.14.2",
  "org.slf4j" % "slf4j-api" % "1.7.36",
  "ch.qos.logback" % "logback-classic" % "1.2.11"
)


