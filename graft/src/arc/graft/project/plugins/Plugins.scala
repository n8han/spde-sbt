import sbt._

class Plugins(info: ProjectInfo) extends PluginDefinition(info) {
	val spde_sbt = "us.technically.spde" % "spde-sbt-plugin" % "{{spde-sbt.version}}"
	val extract = "org.scala-tools.sbt" % "installer-plugin" % "0.2.1"
}