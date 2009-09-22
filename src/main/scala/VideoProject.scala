import sbt._
import java.io.File
import java.net.URL
import FileUtilities.{copyFlat, unzip, withTemporaryDirectory => tempDirectory}
import spde.SpdeProject

protected class SampleVideoProject(info: ProjectInfo) extends SpdeProject(info) with DirectProject with SampleProject
trait VideoProject extends SpdeProject
{
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

trait DirectProject extends SpdeProject
{
  override lazy val glob = directGlob
  def directGlob = fileTask(sourceGlob from spdeSources) {
    FileUtilities.write(sourceGlob.asFile,
"""   |import processing.core._
      |import PConstants._
      |import PApplet._
      |object MyRunner {
      |  def main(args: Array[String]) { PApplet.main(Array("%s")) }
      |}
      |class %s extends spde.core.SApplet {
      |""".stripMargin.format(name, name), log
    ) orElse {
    spdeSources.get foreach { f =>
      import Process._
      f.asFile #>> sourceGlob.asFile !
    }
    FileUtilities.append(sourceGlob.asFile, "\n}", log)
   }
 }
}