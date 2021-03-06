import sbt._

class Plugins(info: ProjectInfo) extends PluginDefinition(info) {
  val archetect = "net.databinder" % "archetect-plugin" % "0.1.3"

  val t_repo = "t_repo" at "http://tristanhunt.com:8081/content/groups/public/"
  val snuggletex_repo = "snuggletex_repo" at "http://www2.ph.ed.ac.uk/maven2"
  val posterous = "net.databinder" % "posterous-sbt" % "0.1.6"
}
