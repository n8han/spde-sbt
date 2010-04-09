package spde

import sbt._

import archetect.TemplateTasks

class DefaultSpdeProject(info: ProjectInfo) extends DefaultProject(info) with SpdeProject with AppletProject

trait SpdeProject extends BasicScalaProject {
  val databinder_repo = "Databinder Repository" at "http://databinder.net/repo"
  val spdeVersion = propertyOptional[String]("0.2.5-SNAPSHOT")
  val processingVersion = propertyOptional[String]("1.1")
  val spde = spde_artifact
  def spde_artifact = "us.technically.spde" %% "spde-core" % spdeVersion.value

  val spdeSourcePath = path(".")
  val spdeSources = spdeSourcePath * "*.spde"
  val managedSources = "src_managed" / "main"
  val managedScalaPath = managedSources / "scala"
  val managedResourcesPath = managedSources / "resources"
  val sourceGlob = managedScalaPath / "glob.scala"
  abstract override def mainSourceRoots = super.mainSourceRoots +++ managedScalaPath
  abstract override def mainResources = super.mainResources +++ descendents(managedResourcesPath ##, "*")
  override def compileAction = super.compileAction dependsOn (glob, data)

  override def watchPaths = super.watchPaths +++ spdeSources

  override def cleanAction = super.cleanAction dependsOn cleanTask(managedScalaPath)
  
  override def fork = forkRun(info.projectPath.asFile)
  
  // permits a parameters to follow a call to Source#getLines in 2.7
  protected implicit def getLines27(iter: Iterator[String]) = new {
    def apply() = iter
  }

  /** Override to match name of sketch's applet class if using straight sources.
      Defaults to project name with non-letters replaced by underscores. */
  def sketchClass = joinedName
  /** class that the sketch extends with managed sources */
  def appletBaseClass = appletClass
  /** @deprecated use appletBaseClass */
  def appletClass = "ProxiedApplet"
  def runnerClass = joinedName + "Runner"
  lazy val joinedName = name.replaceAll("\\W", "_")
  def proxyClass = "DrawProxy"
  def imports = "processing.core._" :: "spde.core._" :: "PConstants._" :: "PApplet._" :: Nil

  lazy val glob = fileTask(sourceGlob from spdeSources) {
    val sources = spdeSources.get
    val sgf = sourceGlob.asFile
    if (sources.isEmpty) None else
      FileUtilities.write(sgf,
"""     |%s
        |object `%s` {
        |  def main(args: Array[String]) { PApplet.main(Array(classOf[`%s`].getName)) }
        |}
        |class `%s` extends %s {
        |  lazy val px = new %s(this) {
        |""".stripMargin.format(
          imports.mkString("import ", "\nimport ", ""), 
          runnerClass, sketchClass, sketchClass, appletBaseClass, proxyClass
        ), log
      ) orElse {
        import scala.io.Source.fromFile
        for(s <- sources; f = s.asFile; (l, n) <- fromFile(f).getLines().zipWithIndex)
          FileUtilities.append(sgf, l.stripLineEnd + 
            " // %s: %s\n".format(f.getName, n+1), log)
        FileUtilities.append(sgf, "\n  }\n}", log)
      }
  } describedAs "Combine all .spde sources into src_managed/scala/glob.scala"
  lazy val data = (syncTask(spdeSourcePath / "data", managedResourcesPath / "data")
    describedAs "Synch data/ with src_managed/main/resources/data/"  )
}
