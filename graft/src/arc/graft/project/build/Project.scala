import sbt._
import spde._

class Project(info: ProjectInfo) extends DefaultSpdeProject(info) 
  with extract.BasicSelfExtractingProject   // not needed after extraction
  with LocalLauncherProject                 // ditto
{
  /** Tasks to run after extraction, not needed after */
  override def installActions = update.name :: sbtScript.name :: "run" :: Nil
}