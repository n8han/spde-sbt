package spde

import sbt._
import java.io.File
import java.net.URL
import FileUtilities.{copyFlat, unzip, withTemporaryDirectory => tempDirectory}

// applet export not yet tested
class DefaultVideoProject(info: ProjectInfo) extends DefaultProject(info) with SpdeProject

trait GSVideoProject extends BasicScalaProject{
  def gsvideoMainVersion = "0.6"
  def gsvideoVersion = gsvideoMainVersion + "-pre0"
  def gsvideoName = "gsvideo"
  def gsvideoArtifactExt = "zip"
  def gsvideoArtifactName = <x>{gsvideoName}-{gsvideoVersion}.{gsvideoArtifactExt}</x>.text
  val gsvideoConf = Configurations.config(gsvideoName)
  def gsvideoURL = new URL( <x>http://sourceforge.net/projects/{gsvideoName}/files/{gsvideoName}/{gsvideoMainVersion}/{gsvideoArtifactName}/download</x>.text )

  val jna = "jna" % "jna" % "3.0.9" from("http://gstreamer-java.googlecode.com/files/jna-3.0.9.jar")
  val gstreamerJava = "gstreamer.java" % "gstreamer-java" % "1.2" from("http://gstreamer-java.googlecode.com/files/gstreamer-java-1.2.jar")
  val gsvideo = gsvideoName % gsvideoName % gsvideoVersion % (gsvideoConf + "->default") artifacts(gsvideoArtifact)
    def gsvideoArtifact = Artifact(gsvideoName, gsvideoArtifactExt, gsvideoArtifactExt, None, Nil, Some(gsvideoURL))

  def gsvideoFilter: NameFilter = "gsvideo/library/gsvideo.jar"
  def gsvideoZip = configurationPath(gsvideoConf)  / gsvideoArtifactName
  
  override def updateAction = gsvideoExtract dependsOn(super.updateAction)
  lazy val gsvideoExtract = task {
    tempDirectory(log) {  temp => Control.thread(unzip(gsvideoZip, Path.fromFile(temp), gsvideoFilter, log))(copyExtracted) }
  }
  protected def copyExtracted(files: scala.collection.Set[Path]): Option[String] = copyFlat(files, configurationPath(Configurations.Compile), log).left.toOption
}
trait VideoProject extends SpdeProject with GSVideoProject
{
  val spde_video = "net.databinder.spde" %% "spde-video" % spdeVersion.value
  override def appletClass = "ProxiedVideoApplet"
  override def proxyClass = "DrawVideoProxy"
  override def imports = "spde.video._" :: "codeanticode.gsvideo._" :: super.imports
}
class SampleVideoProject(info: ProjectInfo) extends DefaultVideoProject(info) with SampleProject
trait SampleProject extends VideoProject
{
  def sampleDirectory: Path = "data"
  def sampleVideo = sampleDirectory / "station.mov"
  def sampleFilter = "gsvideo/examples/Movie/Loop/data/station.mov"
  override def gsvideoFilter = super.gsvideoFilter | sampleFilter
  override def copyExtracted(files: scala.collection.Set[Path]) =
  {
    val (sample, libs) = files.partition(path => "station.mov".accept(path.asFile))
    copyFlat(sample, sampleDirectory, log).left.toOption orElse super.copyExtracted(Set() ++ libs)
  }
}
