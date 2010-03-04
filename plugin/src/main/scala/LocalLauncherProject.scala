package spde
import sbt._

trait LocalLauncherProject extends BasicScalaProject {
  lazy val sbtScript = task {
    import java.io.File
    import Process._
    val launch = (info.projectPath / "sbt").asFile
    val launch_bat = (info.projectPath / "sbt.bat").asFile
    if (System.getProperty("os.name").toLowerCase.indexOf("windows") < 0)
      FileUtilities.write(launch,
        """cd "`dirname "$0"`"; java -Xmx512M -jar sbt-launch-%s.jar "$@" """ format sbtVersion.value,
        log
      ) orElse {
        (("chmod" :: "a+x" :: launch.toString :: Nil) ! log)
        None
      }
    else
      FileUtilities.write(launch_bat,
        "@echo off\njava -Xmx512M -jar sbt-launch-%s.jar %%*" format sbtVersion.value,
        log
      )
  }
}