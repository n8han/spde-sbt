package spde

import sbt._

class SpdeProject(info: ProjectInfo) extends DefaultProject(info) {
  val spdeVersion = propertyOptional[String]("1.0.3__0.1.0")
  val spde = "us.technically" %% "spde-core" % spdeVersion.value

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
      spdeSources.get foreach { f =>
        import Process._
        f.asFile #>> sourceGlob.asFile !
      }
      FileUtilities.append(sourceGlob.asFile, "\n  }\n}", log)
    }
  }
  lazy val data = syncTask(spdeSourcePath / "data", mainCompilePath / "data")
}

class SpdeOpenGLProject(info: ProjectInfo) extends SpdeProject(info) with JoglProject {
  val opengl = "us.technically" %% "processing-opengl" % spdeVersion.value
  override def fork = Some(new ProjectDirectoryRun { 
    override def runJVMOptions = "-Djava.library.path=./lib_managed/compile/" :: Nil
  } )
}

trait JoglProject extends BasicManagedProject {
  val jogl_loc: String => String = 
    "http://download.java.net/media/jogl/builds/archive/jsr-231-1.1.1/jogl-1.1.1-%s.zip" format _
  val jogl_linux = "net.java.dev" % "jogl-linux" % "1.1.1" from jogl_loc("linux-i586")
  val jogl_mac = "net.java.dev" % "jogl-mac" % "1.1.1" from jogl_loc("macosx-universal")

  val lib_compile = configurationPath(Configurations.Compile)

  override def updateAction = super.updateAction && task {
    import FileUtilities._
    val jogl_zip = outputPath / "jogl_zip"
    ((lib_compile * "*.zip").get flatMap { file =>
      unzip(file, jogl_zip, "jogl-*/lib/*", log).left.toOption.orElse {
        FileUtilities.clean(file, log)
      } toList
    } match {
      case Seq() => None
      case list => Some(list mkString "\n")
    }) orElse {
      val files = (jogl_zip ** "lib" ##) ** "*"
      copy(files.get, lib_compile, log).left.toOption
    }
  }
}