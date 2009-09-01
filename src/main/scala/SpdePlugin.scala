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
  val opengl = "net.databinder.spde" % "processing-opengl" % spdeVersion.value
  override def fork = Some(new ProjectDirectoryRun { 
    override def runJVMOptions = "-Djava.library.path=./lib_managed/compile/" :: Nil
  } )
}

trait JoglProject extends BasicManagedProject {
  def jogl_vers = "1.1.1a"
  val Windows = "Windows(.*)".r
  def jogl_sig = (
    (system[String]("os.name").value, system[String]("os.arch").value) match {
      case ("Linux", "i386") => "linux" :: "i586" :: Nil
      case ("Mac OS X" | "Darwin", _) => "macosx" :: "universal" :: Nil
      case (Windows(_), "x86") => "windows" :: "i586" :: Nil
      case (name, arch) => name :: arch :: Nil
    }
  ) map { _.toLowerCase.split(" ").mkString } mkString "-"
  def jogl_loc = 
    "http://download.java.net/media/jogl/builds/archive/jsr-231-%s/jogl-%s-%s.zip" format
      (jogl_vers, jogl_vers, jogl_sig)
  
  val jogl = "net.java.dev" % ("jogl-" + jogl_sig) % jogl_vers % "provided->default" from jogl_loc

  val lib_compile = configurationPath(Configurations.Compile)

  override def updateAction = super.updateAction && task {
    import FileUtilities._
    val jogl_zip = outputPath / "jogl_zip"
    ((configurationPath(Configurations.Provided) * "jogl-*.zip").get flatMap { file =>
      unzip(file, jogl_zip, "jogl-*/lib/*", log).left.toOption.orElse {
        FileUtilities.clean(file, log)
      } toList
    } match {
      case Seq() => None
      case list => Some(list mkString "\n")
    }) orElse {
      val files = (jogl_zip ** "lib" ##) ** "*"
      copy(files.get, configurationPath(Configurations.Compile), log).left.toOption
    }
  }
}
