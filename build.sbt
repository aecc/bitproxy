name := "bitproxy"

version := "0.1"

scalaVersion := "2.11.12"

lazy val root =
  project
    .settings(
    name := "root",
    settings,
    libraryDependencies ++= commonDependencies
  ).in( file(".") )
    .aggregate(dht, core)

lazy val core = project
  .settings(
    name := "core",
    settings,
    libraryDependencies ++= commonDependencies
  )

lazy val dht = project
  .settings(
    name := "dht",
    settings,
    libraryDependencies ++= commonDependencies
  ).dependsOn(core)

lazy val main = project
  .settings(
    name := "main",
    settings,
    libraryDependencies ++= commonDependencies
  ).dependsOn(dht)

lazy val settings = commonSettings

lazy val commonSettings = Seq(
  scalacOptions ++= compilerOptions,
  resolvers ++= Seq(
    "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository",
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  )
)

lazy val compilerOptions = Seq(
  "-unchecked",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-deprecation",
  "-encoding",
  "utf8"
)

lazy val commonDependencies = Seq(
  "org.slf4j" % "slf4j-api" % "1.7.5",
  "io.netty" % "netty-transport" % "4.0.28.Final",
  "io.netty" % "netty-buffer" % "4.0.28.Final",
)
        