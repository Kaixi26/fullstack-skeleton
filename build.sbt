Global / cancelable := true

ThisBuild / scalaVersion := "3.3.5"
ThisBuild / organization := "com.example"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / dockerBaseImage := "openjdk:jre-alpine"
ThisBuild / dockerExposedPorts ++= Seq(8080)
ThisBuild / scalacOptions += "-no-indent"

enablePlugins(AshScriptPlugin)

lazy val backend = project
  .in(file("backend"))
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "backend",
    maintainer := "A Scala Dev!",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "2.1.17",
      "dev.zio" %% "zio-http" % "3.2.0",
      "dev.zio" %% "zio-metrics-connectors" % "2.3.1",
      "dev.zio" %% "zio-metrics-connectors-prometheus" % "2.3.1",
      "io.getquill" %% "quill-jdbc-zio" % "4.8.6",
      "org.xerial" % "sqlite-jdbc" % "3.49.1.0",
      "com.typesafe" % "config" % "1.4.3",
      "dev.zio" %% "zio-logging" % "2.5.0",
    ),
  )
