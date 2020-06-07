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

skip in publish := true

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
      "org.scalatestplus" %% "scalacheck-1-14" % "3.1.2.0" % "test"
    )
  )

lazy val failurewallAkka = (project in file("failurewall-akka"))
  .settings(basicSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    name := "failurewall-akka",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.6.5",
      "org.scalatestplus" %% "mockito-3-2" % "3.1.2.0"
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
    parallelExecution in Test := false
  )
  .dependsOn(
    failurewallAkka
  )

def Scala212 = "2.12.11"

lazy val basicSettings = Seq(
  scalacOptions ++= Seq("-deprecation"),
  crossScalaVersions := Seq(Scala212, "2.13.1"),
  scalaVersion := Scala212,
  skip in publish := true
)

lazy val publishSettings = Seq(
  skip in publish := false
)
