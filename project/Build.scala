import sbt.Keys._
import sbt._

object Build extends Build {
  val project = "failurewall"
  def idOf(name: String): String = s"$project-$name"

  val basicSettings = Seq(
    organization := "com.okumin",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := "2.11.7"
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
        "org.scalatest" %% "scalatest" % "2.2.4" % "test",
        "org.scalacheck" %% "scalacheck" % "1.12.5" % "test"
      )
    )
  )

  lazy val akka = Project(
    id = idOf("akka"),
    base = file(idOf("akka")),
    settings = basicSettings ++ Publish.projectSettings ++ Seq(
      name := idOf("akka"),
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor" % "2.4.1",
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
        "com.typesafe.akka" %% "akka-actor" % "2.4.1"
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
        "com.storm-enroute" %% "scalameter" % "0.7" % "test"
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
