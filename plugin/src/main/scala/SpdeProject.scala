package spde

import sbt._

import archetect.TemplateTasks

class DefaultSpdeProject(info: ProjectInfo) extends DefaultProject(info) with SpdeProject with AppletProject

trait SpdeProject extends BasicScalaProject {
  val spdeVersion = propertyOptional[String]("1.0.3__0.1.3")
  val spde = "net.databinder.spde" %% "spde-core" % spdeVersion.value

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
  
  class ProjectDirectoryRun extends ForkScalaRun {
    override def workingDirectory = Some(info.projectPath.asFile)
  }

  override def fork = Some(new ProjectDirectoryRun)

  lazy val glob = fileTask(sourceGlob from spdeSources) {
    val sources = spdeSources.get
    if (sources.isEmpty) None else
      FileUtilities.write(sourceGlob.asFile,
        // wrapping code, stripped to one line so error line numbers will match
"""     |import processing.core._;
        |import PConstants._;
        |import PApplet._;
        |object MyRunner {
        |  def main(args: Array[String]) { PApplet.main(Array("%s")) }
        |};
        |class %s extends spde.core.SApplet {
        |  lazy val px = new DrawProxy {
        |""".stripMargin.replaceAll("\n","").format(name, name), log
      ) orElse {
        import Process._
        cat(sources.map(_.asFile).toSeq) #>> sourceGlob.asFile ! (log)
        FileUtilities.append(sourceGlob.asFile, "\n  }\n}", log)
      }
  } describedAs "Combine all .spde sources into src_managed/scala/glob.scala"
  lazy val data = (syncTask(spdeSourcePath / "data", managedResourcesPath / "data")
    describedAs "Synch data/ with src_managed/main/resources/data/"  )
}
