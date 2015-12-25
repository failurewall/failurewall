import sbt.Keys._
import sbt._

object Publish extends AutoPlugin {
  override lazy val projectSettings = Seq(
    pomExtra := failurewallPomExtra,
    pomIncludeRepository := { _ => false },
    publishArtifact in Test := false,
    publishTo := failurewallPublishTo.value,
    publishMavenStyle := true
  )

  def failurewallPomExtra = {
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

  def failurewallPublishTo = version { v: String =>
    val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT"))
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  }
}
