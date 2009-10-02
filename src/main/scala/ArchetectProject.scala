/// COPY of net.databinder archtect-plugin 0.1.2. Will remove when plugins can depend on each other.
package archetect

import sbt._

trait TemplateTasks extends FileTasks {
  import io.Source
  private val tmpl = """(?s)(.*)\{\{(.*)\}\}(.*)""".r
  private def template(str: String): String = str match {
    case tmpl(before, key, after) => template(before + templateMappings(key) + after)
    case _ => str
  }
  def templateMappings: Map[String, Any]
  
  def templateTask(in: Path, out: Path) = fileTask(out from in) {
    writeTemplate(Source.fromFile(in asFile), out)
  }
  def templateTask(in: String, out: Path) = task {
    writeTemplate(Source.fromString(in), out)
  }
  private def writeTemplate(source: Source, out: Path) = 
    FileUtilities.write(out.asFile, log) { writer =>
      // not using line-by-line because of 2.7 / 2.8 source incompatibility
      writer write template(source.mkString)
      None
    }
}

trait ArchetectProject extends Project with TemplateTasks {
  import Process._
  val arcSource = "src" / "arc"
  val arcOutput = outputPath / "arc"
  override def watchPaths = super.watchPaths +++ (arcSource ** "*")
  
  lazy val archetect = dynamic(archetectTasks)

  def archetectTasks = task { None } named("archetect-complete") dependsOn (
    descendents(arcSource ##, "*").get.filter(!_.isDirectory).map { in =>
      val out = Path.fromString(arcOutput, in.relativePath)
      templateTask(out, in)
    }.toSeq: _*
  )
}