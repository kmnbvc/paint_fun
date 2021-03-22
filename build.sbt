val Http4sVersion = "0.21.16"
val CirceVersion = "0.13.0"
val MunitVersion = "0.7.20"
val LogbackVersion = "1.2.3"
val MunitCatsEffectVersion = "0.13.0"
val Redis4catsVersion = "0.12.0"

val http4sDependencies = Seq(
  "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
  "org.http4s" %% "http4s-circe" % Http4sVersion,
  "org.http4s" %% "http4s-dsl" % Http4sVersion,
  "org.http4s" %% "http4s-twirl" % Http4sVersion,
)

val circeDependencies = Seq(
  "io.circe" %% "circe-generic" % CirceVersion,
  "io.circe" %% "circe-parser" % CirceVersion,
)

val redisDependencies = Seq(
  "dev.profunktor" %% "redis4cats-streams" % Redis4catsVersion,
  "dev.profunktor" %% "redis4cats-log4cats" % Redis4catsVersion,
  "org.typelevel" %% "log4cats-slf4j" % "1.2.0",
)

val testScopeDependencies = Seq(
  "org.scalameta" %% "munit" % MunitVersion % Test,
  "org.typelevel" %% "munit-cats-effect-2" % MunitCatsEffectVersion % Test,
)

lazy val root = (project in file("."))
  .enablePlugins(SbtTwirl)
  .settings(
    organization := "org",
    name := "paint-fun-http4s",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.13.4",
    libraryDependencies ++= Seq(
      "com.typesafe" % "config" % "1.4.1",
      "ch.qos.logback" % "logback-classic" % LogbackVersion
    ),
    libraryDependencies ++= Seq(
      http4sDependencies,
      circeDependencies,
      redisDependencies,
      testScopeDependencies).flatten,
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3"),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    testFrameworks += new TestFramework("munit.Framework")
  )
