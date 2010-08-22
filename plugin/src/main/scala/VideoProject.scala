package spde

import sbt._
import java.io.File
import java.net.URL
import FileUtilities.{copyFlat, unzip, withTemporaryDirectory => tempDirectory}

// applet export not yet tested
class DefaultVideoProject(info: ProjectInfo) extends DefaultProject(info) with SpdeProject

trait GSVideoProject extends BasicScalaProject{
  def gsvideoVersion = "0.7"
  def gsvideoName = "gsvideo"
  def gsvideoArtifactExt = "zip"
  def gsvideoArtifactName = <x>GSVideo-Linux-{gsvideoVersion}.{gsvideoArtifactExt}</x>.text
  val gsvideoConf = Configurations.config(gsvideoName)
  def gsvideoURL = <x>http://downloads.sourceforge.net/project/{gsvideoName}/{gsvideoName}/{gsvideoVersion}/{gsvideoArtifactName}</x>.text

  val gstreamerJava =  "com.googlecode.gstreamer-java" % "gstreamer-java" % "1.4"
  val gsvideo = gsvideoName % gsvideoName % gsvideoVersion % (gsvideoConf + "->default") from(gsvideoURL)

  def gsvideoFilter: NameFilter = "GSVideo/library/GSVideo.jar"
  def gsvideoZip = configurationPath(gsvideoConf)  / "%s-%s.%s".format(
    gsvideoName, gsvideoVersion, gsvideoArtifactExt)
  
  override def updateAction = gsvideoExtract dependsOn(super.updateAction)
  lazy val gsvideoExtract = task {
    tempDirectory(log) {  temp => Control.thread(unzip(gsvideoZip, Path.fromFile(temp), gsvideoFilter, log))(copyExtracted) }
  }
  protected def copyExtracted(files: scala.collection.Set[Path]): Option[String] = copyFlat(files, configurationPath(Configurations.Compile), log).left.toOption
}
trait VideoProject extends SpdeProject with GSVideoProject
{
  val spde_video = "us.technically.spde" %% "spde-video" % spdeVersion.value
  override def appletBaseClass = "ProxiedVideoApplet"
  override def proxyClass = "DrawVideoProxy"
  override def imports = "spde.video._" :: "codeanticode.gsvideo._" :: super.imports
}
class SampleVideoProject(info: ProjectInfo) extends DefaultVideoProject(info) with SampleProject
trait SampleProject extends VideoProject
{
  def sampleDirectory: Path = "data"
  def sampleVideo = sampleDirectory / "station.mov"
  def sampleFilter = "GSVideo/examples/Movie/Loop/data/station.mov"
  override def gsvideoFilter = super.gsvideoFilter | sampleFilter
  override def copyExtracted(files: scala.collection.Set[Path]) =
  {
    val (sample, libs) = files.partition(path => "station.mov".accept(path.asFile))
    copyFlat(sample, sampleDirectory, log).left.toOption orElse super.copyExtracted(Set() ++ libs)
  }
}
