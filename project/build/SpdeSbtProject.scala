import sbt._

class SpdeSbtProject(info: ProjectInfo) extends ParentProject(info) with posterous.Publish
{
  lazy val plugin = project("plugin", "Spde sbt plugin", new PluginProject(_) with AutoCompilerPlugins {
    override def managedStyle = ManagedStyle.Maven
    lazy val publishTo = Resolver.file("Databinder Repository", new java.io.File("/var/dbwww/repo"))
  })
  
  override def publishAction = super.publishAction && publishCurrentNotes
  
  lazy val graft = project("graft", "spde-sbt graft", new DefaultProject(_) with archetect.ArchetectProject {
    import Process._

    override val templateMappings = Map(
      "sbt.version" -> SpdeSbtProject.this.sbtVersion.value,
      "build.scala.version" -> SpdeSbtProject.this.buildScalaVersion,
      "def.scala.version" -> SpdeSbtProject.this.defScalaVersion.value,
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
    } dependsOn (plugin.publishLocal, archetect)
    override def publishAction = task { None } && publishGraft
    val publishPath = Path.fromFile("/var/www/spde-graft/")
    lazy val publishGraft = copyTask((outputPath / "arc" * "*" / "target" ##) * "*.jar", 
        publishPath) dependsOn(installer)
  })
}

