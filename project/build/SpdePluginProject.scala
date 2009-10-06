import sbt._

class SpdeSbtProject(info: ProjectInfo) extends ParentProject(info)
{
  // parent project should not be published
  override def publishAction = task { None }
  override def publishConfiguration = publishLocalConfiguration

  lazy val plugin = project("plugin", "Spde sbt plugin", new PluginProject(_) {
    override def managedStyle = ManagedStyle.Maven
    val publishTo = "Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/releases/"
    Credentials(Path.userHome / ".ivy2" / ".credentials", log)
  })
  
  lazy val graft = project("graft", "spde-sbt graft", new DefaultProject(_) with archetect.ArchetectProject {
    import Process._

    override val templateMappings = Map(
      "sbt.version" -> SpdeSbtProject.this.sbtVersion.value,
      "scala.version" -> SpdeSbtProject.this.scalaVersion.value,
      "spde-sbt.version" -> version
    )
    lazy val proj_target = arcOutput / "graft"
    lazy val proj_target_target = proj_target / "target"
    lazy val installer = task {
      proj_target_target.asFile.setLastModified(System.currentTimeMillis)
      (new java.lang.ProcessBuilder("sbt", "clean", "installer") directory proj_target.asFile) ! log match {
        case 0 => None
        case code => Some("sbt failed on archetect project %s with code %d" format (proj_target, code))
      }
    } dependsOn archetect

    val publishPath = Path.fromFile("/var/www/spde-graft/")
    lazy val publishGraft = copyTask((outputPath / "arc" * "*" / "target" ##) * "*.jar", 
        publishPath) dependsOn(installer)
  })
}

