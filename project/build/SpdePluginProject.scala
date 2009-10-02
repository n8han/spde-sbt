import sbt._

class SpdePluginProject(info: ProjectInfo) extends PluginProject(info) {
  override def managedStyle = ManagedStyle.Maven
  val publishTo = "Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/releases/"
  // val archetect = "net.databinder" % "archetect-plugin" % "0.1.2"
	
  Credentials(Path.userHome / ".ivy2" / ".credentials", log)
}

