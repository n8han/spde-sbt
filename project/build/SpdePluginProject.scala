import sbt._

class SpdePluginProject(info: ProjectInfo) extends PluginProject(info) {
  val publishTo = "Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/releases/"
  Credentials(Path.userHome / ".ivy2" / ".credentials", log)
}

