version := "1.0.0"
scalaVersion := "2.12.8"
Global / onChangedBuildSource := ReloadOnSourceChanges
lazy val root = (project in file("."))
  .settings(
    name := "worker-node"
  )

libraryDependencies ++= Seq(
  "org.apache.commons" % "commons-lang3" % "3.12.0",
  "org.apache.commons" % "commons-text" % "1.9",

  "com.rabbitmq" % "amqp-client" % "5.14.2",
  "org.slf4j" % "slf4j-api" % "1.7.36",
  "ch.qos.logback" % "logback-classic" % "1.2.11",

  "com.typesafe" % "config" % "1.4.2",

  "de.opal-project" %% "common" % "4.0.0",
  "de.opal-project" %% "tools" % "4.0.0",
  "de.opal-project" %% "bytecode-representation" % "4.0.0"
)

// unmanagedClasspath in Runtime += baseDirectory.value / "resources"

mainClass := Some("main.StringHoundAnalyser")
