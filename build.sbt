inThisBuild(List(
  organization := "com.okumin",
  homepage := Some(url("https://github.com/failurewall/failurewall")),
  licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  developers := List(
    Developer(
      id = "okumin",
      name = "okumin",
      email = "git@okumin.com",
      url = url("https://okumin.com/")
    )
  )
))

publish / skip := true

lazy val root = (project in file("."))
  .settings(
    name := "failurewall-root"
  )
  .aggregate(failurewallCore, failurewallAkka, failurewallPatterns)

lazy val failurewallCore = (project in file("failurewall-core"))
  .settings(basicSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    name := "failurewall-core",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest-wordspec" % "3.2.13" % "test",
      "org.scalatestplus" %% "scalacheck-1-15" % "3.2.11.0" % "test"
    )
  )

lazy val failurewallAkka = (project in file("failurewall-akka"))
  .settings(basicSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    name := "failurewall-akka",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.6.20",
      "org.scalatestplus" %% "mockito-3-4" % "3.2.10.0" % "test"
    )
  )
  .dependsOn(
    failurewallCore,
    failurewallCore % "test->test"
  )

lazy val failurewallPatterns = (project in file("failurewall-patterns"))
  .settings(basicSettings: _*)
  .settings(
    name := "failurewall-patterns"
  )
  .dependsOn(
    failurewallCore,
    failurewallCore % "test->test",
    failurewallAkka
  )

lazy val failurewallSample = (project in file("failurewall-sample"))
  .settings(basicSettings: _*)
  .settings(
    name := "failurewall-sample"
  )
  .dependsOn(
    failurewallPatterns
  )

lazy val failurewallBenchmark = (project in file("failurewall-benchmark"))
  .settings(basicSettings: _*)
  .settings(
    name := "failurewall-benchmark",
    resolvers ++= Seq(
      "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/releases"
    ),
    libraryDependencies ++= Seq(
      "com.storm-enroute" %% "scalameter" % "0.8.2" % "test"
    ),
    testFrameworks ++= Seq(
      new TestFramework("org.scalameter.ScalaMeterFramework")
    ),
    Test / parallelExecution := false
  )
  .dependsOn(
    failurewallAkka
  )

def Scala212 = "2.12.17"

lazy val basicSettings = Seq(
  scalacOptions ++= Seq("-deprecation"),
  crossScalaVersions := Seq(Scala212, "2.13.9", "3.1.3"),
  Compile / doc / sources := {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) =>
        (Compile / doc / sources).value
      case _ =>
        // disable if Scala 3
        Nil
    }
  },
  scalaVersion := Scala212,
  publish / skip := true
)

lazy val publishSettings = Seq(
  publish / skip := false
)
