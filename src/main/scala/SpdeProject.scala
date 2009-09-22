package spde

import sbt._

class SpdeProject(info: ProjectInfo) extends DefaultProject(info) {
  val spdeVersion = propertyOptional[String]("1.0.3__0.1.1")
  val spde = "net.databinder.spde" %% "spde-core" % spdeVersion.value

  val spdeSourcePath = path(".")
  val spdeSources = spdeSourcePath * "*.spde"
  val managedScalaPath = "src_managed" / "main" / "scala"
  val sourceGlob = managedScalaPath / "glob.scala"
  override def mainSourceRoots = super.mainSourceRoots +++ managedScalaPath
  override def compileAction = super.compileAction dependsOn (glob, data)

  override def watchPaths = super.watchPaths +++ spdeSources

  override def cleanAction = super.cleanAction dependsOn cleanTask(managedScalaPath)
  
  class ProjectDirectoryRun extends ForkScalaRun {
    override def workingDirectory = Some(info.projectPath.asFile)
  }

  override def fork = Some(new ProjectDirectoryRun)

  lazy val glob = fileTask(sourceGlob from spdeSources) {
    FileUtilities.write(sourceGlob.asFile,
"""   |import processing.core._
      |import PConstants._
      |import PApplet._
      |object MyRunner {
      |  def main(args: Array[String]) { PApplet.main(Array("%s")) }
      |}
      |class %s extends spde.core.SApplet {
      |  lazy val px = new DrawProxy {
      |""".stripMargin.format(name, name), log
    ) orElse {
      import Process._
      cat(spdeSources.get.map(_.asFile).toSeq) #>> sourceGlob.asFile ! (log)
      FileUtilities.append(sourceGlob.asFile, "\n  }\n}", log)
    }
  }
  lazy val data = syncTask(spdeSourcePath / "data", mainCompilePath / "data")
}