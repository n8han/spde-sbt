import sbt._
import spde._

class Project(info: ProjectInfo) extends DefaultSpdeProject(info) 
  with extract.BasicSelfExtractingProject // feel free to remove this trait!
{
  
}