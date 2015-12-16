import sbt.Keys._
import sbt._

object Build extends Build {
  val project = "failurewall"
  def idOf(name: String): String = s"$project-$name"

  val basicSettings = Seq(
    version := "0.0.1",
    scalaVersion := "2.11.7"
  )

  lazy val root = Project(
    id = idOf("root"),
    base = file("./"),
    settings = Seq(
      name := idOf("root")
    ),
    aggregate = Seq(core, akka, failurewall)
  )

  lazy val core = Project(
    id = idOf("core"),
    base = file(idOf("core")),
    settings = basicSettings ++ Seq(
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
    settings = basicSettings ++ Seq(
      name := idOf("akka"),
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor" % "2.4.1"
      )
    ),
    dependencies = Seq(
      core,
      core % "test->test"
    )
  )

  lazy val failurewall = Project(
    id = project,
    base = file(project),
    settings = basicSettings ++ Seq(
      name := project,
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
}
