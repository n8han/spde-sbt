package spde

import sbt._

// applet export not yet supported
class DefaultOpenGLProject(info: ProjectInfo) extends DefaultProject(info) with OpenGLProject

trait OpenGLProject extends SpdeProject with PackagePaths with JoglProject {
  val opengl = "org.processing" % "opengl" % processingVersion.value
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
    ((configurationPath(Configurations.Provided) * "jogl-*.zip").get.toList flatMap { file =>
      unzip(file, jogl_zip, "jogl-*/lib/*", log).left.toOption.orElse {
        FileUtilities.clean(file, log)
      }
    } match {
      case Nil => None
      case list => Some(list mkString "\n")
    }) orElse {
      val files = (jogl_zip ** "lib" ##) ** "*"
      println("about to copy")
      copy(files.get, configurationPath(Configurations.Compile), log).left.toOption
    }
  }
}
