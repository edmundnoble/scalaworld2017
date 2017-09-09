name := "sbench"
scalaVersion := "2.12.3"
libraryDependencies += "org.openjdk.jmh" % "jmh-generator-bytecode" % "1.19"

enablePlugins(JmhPlugin)

resolvers += Resolver.sonatypeRepo("releases")

addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.4" cross CrossVersion.binary)

scalacOptions ++= Seq(
  "-Xlint",
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xfatal-warnings",
  "-Yno-adapted-args",
  "-Ywarn-numeric-widen",
  "-Ywarn-unused-import",
  "-Ywarn-value-discard",
  "-Ypartial-unification",
  "-Ypatmat-exhaust-depth", "40",
  "-Xfuture")

libraryDependencies += "org.typelevel" %% "cats-core" % "1.0.0-MF"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0" % "test"
libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.13.4" % "test"
