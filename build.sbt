
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
      "org.scalatest" %% "scalatest" % "3.0.1" % "test",
      "org.scalacheck" %% "scalacheck" % "1.13.4" % "test"
    )
  )

lazy val failurewallAkka = (project in file("failurewall-akka"))
  .settings(basicSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    name := "failurewall-akka",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.4.17",
      "org.mockito" % "mockito-core" % "1.10.19" % "test"
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

lazy val basicSettings = Seq(
  organization := "com.okumin",
  version := "0.1.1",
  crossScalaVersions := Seq("2.11.12", "2.12.4"),
  scalaVersion := "2.11.12"
)

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  pomExtra := {
    <url>https://github.com/failurewall/failurewall</url>
      <licenses>
        <license>
          <name>Apache 2 License</name>
          <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:failurewall/failurewall.git</url>
        <connection>scm:git@github.com:failurewall/failurewall.git</connection>
      </scm>
      <developers>
        <developer>
          <id>okumin</id>
          <name>okumin</name>
          <url>http://okumin.com/</url>
        </developer>
      </developers>
  }
)
