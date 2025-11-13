// build.sbt
ThisBuild / scalaVersion := "3.3.1"
ThisBuild / versionScheme := Some("early-semver")
// If your tags are not prefixed with "v", uncomment the following line
// ThisBuild / dynverTagPrefix := ""
ThisBuild / organization := "us.awfl"

name := "compiler-yaml"

ThisBuild / description := "AWFL DSL to Google Cloud Workflows YAML/JSON compiler"
ThisBuild / homepage := Some(url("https://github.com/awfl-us/compiler-yaml"))
ThisBuild / licenses := List("MIT" -> url("https://opensource.org/licenses/MIT"))
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/awfl-us/compiler-yaml"),
    "scm:git:https://github.com/awfl-us/compiler-yaml.git",
    Some("scm:git:ssh://git@github.com/awfl-us/compiler-yaml.git")
  )
)
ThisBuild / developers := List(
  Developer(id = "awfl", name = "AWFL", email = "opensource@awfl.us", url = url("https://github.com/awfl-us"))
)
ThisBuild / pomIncludeRepository := { _ => false }

ThisBuild / version ~= { v =>
  if (sys.env.get("CI").contains("true")) v
  else "0.1.0-SNAPSHOT"
}

publishMavenStyle := true

// Dependencies
libraryDependencies ++= Seq(
  "us.awfl" %% "dsl" % "0.1.2",
)

// dependencyOverrides += "us.awfl" %% "dsl" % "0.1.0-SNAPSHOT"

scalacOptions ++= Seq("-deprecation", "-feature", "-language:implicitConversions")
