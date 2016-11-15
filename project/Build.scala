import sbt.Keys._
import sbt._

object Build extends Build {
  val project = "failurewall"
  def idOf(name: String): String = s"$project-$name"

  val scala211 = "2.11.8"

  val basicSettings = Seq(
    organization := "com.okumin",
    version := "0.1.0",
    crossScalaVersions := Seq(scala211, "2.12.0"),
    scalaVersion := scala211
  )

  lazy val root = Project(
    id = idOf("root"),
    base = file("./"),
    settings = Seq(
      name := idOf("root")
    ),
    aggregate = Seq(core, akka, patterns)
  )

  lazy val core = Project(
    id = idOf("core"),
    base = file(idOf("core")),
    settings = basicSettings ++ Publish.projectSettings ++ Seq(
      name := idOf("core"),
      libraryDependencies ++= Seq(
        "org.scalatest" %% "scalatest" % "3.0.1" % "test",
        "org.scalacheck" %% "scalacheck" % "1.13.4" % "test"
      )
    )
  )

  lazy val akka = Project(
    id = idOf("akka"),
    base = file(idOf("akka")),
    settings = basicSettings ++ Publish.projectSettings ++ Seq(
      name := idOf("akka"),
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor" % "2.4.12",
        "org.mockito" % "mockito-core" % "1.10.19" % "test"
      )
    ),
    dependencies = Seq(
      core,
      core % "test->test"
    )
  )

  lazy val patterns = Project(
    id = idOf("patterns"),
    base = file(idOf("patterns")),
    settings = basicSettings ++ Seq(
      name := idOf("patterns"),
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor" % "2.4.12"
      )
    ),
    dependencies = Seq(
      core,
      core % "test->test",
      akka
    )
  )

  lazy val sample = Project(
    id = idOf("sample"),
    base = file(idOf("sample")),
    settings = basicSettings ++ Seq(
      name := idOf("sample")
    ),
    dependencies = Seq(
      patterns
    )
  )

  lazy val benchmark = Project(
    id = idOf("benchmark"),
    base = file(idOf("benchmark")),
    settings = basicSettings ++ Seq(
      name := idOf("benchmark"),
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
    ),
    dependencies = Seq(
      akka
    )
  )
}
